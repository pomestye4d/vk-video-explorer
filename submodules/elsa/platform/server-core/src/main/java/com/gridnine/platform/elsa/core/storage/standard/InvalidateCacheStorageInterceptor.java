/*
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.gridnine.platform.elsa.core.storage.standard;

import com.gridnine.platform.elsa.common.core.l10n.SupportedLocalesProvider;
import com.gridnine.platform.elsa.common.core.model.common.BaseIdentity;
import com.gridnine.platform.elsa.common.core.model.common.Localizable;
import com.gridnine.platform.elsa.common.core.model.domain.BaseAsset;
import com.gridnine.platform.elsa.common.core.model.domain.BaseDocument;
import com.gridnine.platform.elsa.common.core.model.domain.BaseSearchableProjection;
import com.gridnine.platform.elsa.common.core.model.domain.CaptionProvider;
import com.gridnine.platform.elsa.common.meta.domain.DomainMetaRegistry;
import com.gridnine.platform.elsa.core.cache.CacheManager;
import com.gridnine.platform.elsa.core.cache.CacheMetadataProvider;
import com.gridnine.platform.elsa.core.storage.*;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InvalidateCacheStorageInterceptor implements StorageInterceptor {

    @Autowired
    private CacheMetadataProvider cacheMetadataProvider;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ListableBeanFactory factory;

    private volatile Map<String, List<SearchableProjectionHandler<BaseDocument, BaseSearchableProjection<BaseDocument>>>> projectionHandlers;

    @Autowired
    private CacheStorageAdvice cacheStorageAdvice;

    @Override
    public double getPriority() {
        return 100;
    }

    @Autowired
    private SupportedLocalesProvider supportedLocalesProvider;

    @Autowired
    private CaptionProvider storageCaptionProvider;

    @Autowired
    private DomainMetaRegistry domainMetaRegistry;


    @Override
    public <A extends BaseAsset> void onDelete(A oldAssset, OperationContext<A> operationContext) {
        init();
        if (cacheMetadataProvider.isCacheResolve(oldAssset.getClass())) {
            operationContext.globalContext().getContext().getPostCommitCallbacks().add(()
                    -> cacheStorageAdvice.invalidateResolveCache(oldAssset.getClass(), oldAssset.getId()));
        }
        {
            var props = cacheMetadataProvider.getFindCacheProperties(oldAssset.getClass());
            if (!props.isEmpty()) {
                for (var prop : props) {
                    var value = oldAssset.getValue(prop);
                    operationContext.globalContext().getContext().getPostCommitCallbacks().add(()
                            -> cacheStorageAdvice.invalidateFindCache(oldAssset.getClass(), prop, value));
                }
            }
        }
        {
            var props = cacheMetadataProvider.getGetAllCacheProperties(oldAssset.getClass());
            if (!props.isEmpty()) {
                for (var prop : props) {
                    var value = oldAssset.getValue(prop);
                    operationContext.globalContext().getContext().getPostCommitCallbacks().add(()
                            -> cacheStorageAdvice.invalidateGetAllCache(oldAssset.getClass(), prop, value));
                }
            }
        }
        if (cacheMetadataProvider.isCacheCaption(oldAssset.getClass()) && storageCaptionProvider instanceof  JdbcCaptionProviderImpl  jdbcCP) {
            jdbcCP.invalidateCaptionsCache(oldAssset.getClass(), oldAssset.getId());
        }
    }

    @Override
    public <D extends BaseDocument> void onDelete(D document, OperationContext<D> operationContext) throws Exception {
        init();
        if (cacheMetadataProvider.isCacheResolve(document.getClass())) {
            operationContext.globalContext().getContext().getPostCommitCallbacks().add(()
                    -> cacheStorageAdvice.invalidateResolveCache(document.getClass(), document.getId()));
        }
        for (var handler : projectionHandlers.get(document.getClass().getName())) {
            {
                var props = cacheMetadataProvider.getFindCacheProperties(handler.getProjectionClass());
                if (!props.isEmpty()) {
                    for (var proj : handler.createProjections(document, props)) {
                        for (var prop : props) {
                            var value = proj.getValue(prop);
                            operationContext.globalContext().getContext().getPostCommitCallbacks().add(()
                                    -> cacheStorageAdvice.invalidateFindCache(handler.getProjectionClass(), prop, value));
                        }
                    }
                }
            }
            {
                var props = cacheMetadataProvider.getGetAllCacheProperties(handler.getProjectionClass());
                if (!props.isEmpty()) {
                    for (var proj : handler.createProjections(document, props)) {
                        for (var prop : props) {
                            var value = proj.getValue(prop);
                            operationContext.globalContext().getContext().getPostCommitCallbacks().add(()
                                    -> cacheStorageAdvice.invalidateGetAllCache(handler.getProjectionClass(), prop, value));
                        }
                    }
                }
            }
        }
        if (cacheMetadataProvider.isCacheCaption(document.getClass())) {
            ((JdbcCaptionProviderImpl) storageCaptionProvider).invalidateCaptionsCache(document.getClass(), document.getId());
        }
    }

    @Override
    public <A extends BaseAsset> void onSave(A asset, OperationContext<A> context) {
        init();
        if (cacheMetadataProvider.isCacheResolve(asset.getClass())) {
            context.globalContext().getContext().getPostCommitCallbacks().add(()
                    -> cacheStorageAdvice.invalidateResolveCache(asset.getClass(), asset.getId()));
        }
        {
            var props = cacheMetadataProvider.getFindCacheProperties(asset.getClass());
            if (!props.isEmpty()) {
                for (var prop : props) {
                    var oldValue = context.localContext().getOldObject() == null ? null :
                            context.localContext().getOldObject().getValue(prop);
                    var newValue = asset.getValue(prop);
                    if (!Objects.equals(oldValue, newValue)) {
                        cacheStorageAdvice.invalidateFindCache(asset.getClass(), prop, oldValue);
                        cacheStorageAdvice.invalidateFindCache(asset.getClass(), prop, newValue);
                    }
                }
            }
        }
        {
            var props = cacheMetadataProvider.getGetAllCacheProperties(asset.getClass());
            if (!props.isEmpty()) {
                for (var prop : props) {
                    var oldValue = context.localContext().getOldObject() == null ? null :
                            context.localContext().getOldObject().getValue(prop);
                    var newValue = asset.getValue(prop);
                    if (!Objects.equals(oldValue, newValue)) {
                        cacheStorageAdvice.invalidateGetAllCache(asset.getClass(), prop, oldValue);
                        cacheStorageAdvice.invalidateGetAllCache(asset.getClass(), prop, newValue);
                    }
                }
            }
        }
        if (cacheMetadataProvider.isCacheCaption(asset.getClass())) {
            checkCaptionChanged(asset, context.localContext().getOldObject());
        }
        if (cacheMetadataProvider.isCacheLocalizedCaption(asset.getClass())) {
            checkCaptionChanged(asset, context.localContext().getOldObject());
        }
    }

    private <A extends BaseIdentity> void checkCaptionChanged(A asset, A oldObject) {
        if (oldObject == null) {
            return;
        }
        if (supportedLocalesProvider.getSupportedLocales().stream().anyMatch(it ->
                !Objects.equals(getCaption(asset, it), getCaption(oldObject, it)))) {
             storageCaptionProvider.invalidateCaptionsCache(asset.getClass(), asset.getId());
        }
    }


    private <A extends BaseIdentity> Object getCaption(A oldObject, Locale it) {
        var docDescr = domainMetaRegistry.getDocuments().get(oldObject.getClass().getName());
        if (docDescr != null) {
            return docDescr.getLocalizableCaptionExpression() == null ? oldObject.toString() : ((Localizable) oldObject).toString(it);
        }
        var assetDescr = domainMetaRegistry.getAssets().get(oldObject.getClass().getName());
        if (assetDescr != null) {
            return assetDescr.getLocalizableCaptionExpression() == null ? oldObject.toString() : ((Localizable) oldObject).toString(it);
        }
        return null;
    }


    @Override
    public <D extends BaseDocument> void onSave(D doc, OperationContext<D> context) throws Exception {
        init();
        if (cacheMetadataProvider.isCacheResolve(doc.getClass())) {
            context.globalContext().getContext().getPostCommitCallbacks().add(()
                    -> cacheStorageAdvice.invalidateResolveCache(doc.getClass(), doc.getId()));
        }
        for (var handler : projectionHandlers.get(doc.getClass().getName())) {
            var findProps = cacheMetadataProvider.getFindCacheProperties(handler.getProjectionClass());
            var getAllProps = cacheMetadataProvider.getGetAllCacheProperties(handler.getProjectionClass());
            var props = new HashSet<String>();
            props.addAll(findProps);
            props.addAll(getAllProps);
            if (!props.isEmpty()) {
                var newProjections = new ArrayList<>(handler.createProjections(doc, props));
                var oldProjections = context.localContext().getOldObject() == null ?
                        Collections.<BaseSearchableProjection<BaseDocument>>emptyList() :
                        handler.createProjections(context.localContext().getOldObject(), props);
                for (var oldProj : oldProjections) {
                    var newProj = oldProj.getNavigationKey() != null ? newProjections.stream()
                            .filter(it -> oldProj.getNavigationKey().equals(it.getNavigationKey())).findFirst().orElse(null) : (
                            newProjections.size() == 1 ? newProjections.get(0) : null
                    );
                    if (newProj != null) {
                        newProjections.remove(newProj);
                    }
                    for (var prop : props) {
                        var oldValue = oldProj.getValue(prop);
                        var newValue = newProj == null ? null : newProj.getValue(prop);
                        if (!Objects.equals(oldValue, newValue)) {
                            context.globalContext().getContext().getPostCommitCallbacks().add(()
                                    -> {
                                if (findProps.contains(prop)) {
                                    cacheStorageAdvice.invalidateFindCache(handler.getProjectionClass(), prop, oldValue);
                                    cacheStorageAdvice.invalidateFindCache(handler.getProjectionClass(), prop, newValue);
                                }
                                if (getAllProps.contains(prop)) {
                                    cacheStorageAdvice.invalidateGetAllCache(handler.getProjectionClass(), prop, oldValue);
                                    cacheStorageAdvice.invalidateGetAllCache(handler.getProjectionClass(), prop, newValue);
                                }
                            });
                        }
                    }
                }
                for (var proj : newProjections) {
                    for (var prop : props) {
                        var newValue = proj.getValue(prop);
                        context.globalContext().getContext().getPostCommitCallbacks().add(()
                                -> {
                            if (findProps.contains(prop)) {
                                cacheStorageAdvice.invalidateFindCache(handler.getProjectionClass(), prop, null);
                                cacheStorageAdvice.invalidateFindCache(handler.getProjectionClass(), prop, newValue);
                            }
                            if (getAllProps.contains(prop)) {
                                cacheStorageAdvice.invalidateGetAllCache(handler.getProjectionClass(), prop, null);
                                cacheStorageAdvice.invalidateGetAllCache(handler.getProjectionClass(), prop, newValue);
                            }
                        });
                    }
                }
            }
        }
        if (cacheMetadataProvider.isCacheCaption(doc.getClass())) {
            checkCaptionChanged(doc, context.localContext().getOldObject());
        }
    }

    @SuppressWarnings("unchecked")
    private void init() {
        if (projectionHandlers == null) {
            synchronized (this) {
                if (projectionHandlers == null) {
                    var handlers = new ConcurrentHashMap<String, List<SearchableProjectionHandler<BaseDocument, BaseSearchableProjection<BaseDocument>>>>();
                    factory.getBeansOfType(SearchableProjectionHandler.class).values().forEach(h -> {
                        var lst = handlers.computeIfAbsent(h.getDocumentClass().getName(), k -> new ArrayList<>());
                        lst.add(h);
                    });
                    projectionHandlers = handlers;
                }
            }

        }
    }

    @Override
    public boolean isApplicableOnDelete(DeleteAssetParameters params) {
        return true;
    }

    @Override
    public boolean isApplicableOnDelete(DeleteDocumentParameters params) {
        return true;
    }

    @Override
    public boolean isApplicableOnSave(SaveAssetParameters params) {
        return  true;
    }

    @Override
    public boolean isApplicableOnSave(SaveDocumentParameters params) {
        return true;
    }

}
