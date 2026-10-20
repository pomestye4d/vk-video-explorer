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
import com.gridnine.platform.elsa.common.core.lock.LockManager;
import com.gridnine.platform.elsa.common.core.model.common.*;
import com.gridnine.platform.elsa.common.core.model.domain.*;
import com.gridnine.platform.elsa.common.core.search.*;
import com.gridnine.platform.elsa.common.core.serialization.JsonMarshaller;
import com.gridnine.platform.elsa.common.core.serialization.JsonUnmarshaller;
import com.gridnine.platform.elsa.common.core.serialization.SerializationParameters;
import com.gridnine.platform.elsa.common.core.utils.ExceptionUtils;
import com.gridnine.platform.elsa.common.core.utils.IoUtils;
import com.gridnine.platform.elsa.common.core.utils.LocaleUtils;
import com.gridnine.platform.elsa.common.meta.domain.BaseSearchableDescription;
import com.gridnine.platform.elsa.common.meta.domain.DatabaseCollectionDescription;
import com.gridnine.platform.elsa.common.meta.domain.DatabasePropertyDescription;
import com.gridnine.platform.elsa.common.meta.domain.DomainMetaRegistry;
import com.gridnine.platform.elsa.core.auth.AuthContext;
import com.gridnine.platform.elsa.core.storage.*;
import com.gridnine.platform.elsa.core.storage.database.*;
import com.gridnine.platform.elsa.core.storage.transaction.ElsaTransactionContext;
import com.gridnine.platform.elsa.core.storage.transaction.ElsaTransactionManager;
import com.gridnine.platform.elsa.server.core.CoreL10nMessagesRegistryFactory;
import com.nothome.delta.Delta;
import com.nothome.delta.GDiffPatcher;
import com.nothome.delta.GDiffWriter;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@SuppressWarnings("unchecked")
public class StandardStorage implements Storage {

    private final  ListableBeanFactory factory;

    private final AbstractBeanFactory abstractBeanFactory;

    private final  LockManager lockManager;

    private final SupportedLocalesProvider localesProvider;

    private final  List<DatabaseFactory> databaseFactories;

    private final  DomainMetaRegistry domainMetaRegistry;

    private final String configurationPrefix;

    private final Database database;

    private final ElsaTransactionManager transactionManager;

    private final EnumMapper enumMapper;

    private final ClassMapper classMapper;

    private final CaptionProvider captionProvider;

    private final Map<String, AggregatedDataProvider<?,?>> aggregatedDataProviders = new HashMap<>();

    public StandardStorage(ListableBeanFactory factory,
                           LockManager lockManager,
                           DomainMetaRegistry metaRegistry,
                           SupportedLocalesProvider localesProvider,
                           List<DatabaseFactory> databaseFactories,
                           AbstractBeanFactory abstractBeanFactory, String prefix, Map<String, Object> customParameters) throws Exception {
        this.factory = factory;
        this.lockManager = lockManager;
        this.localesProvider = localesProvider;
        this.databaseFactories = databaseFactories;
        this.domainMetaRegistry = metaRegistry;
        this.abstractBeanFactory = abstractBeanFactory;
        this.configurationPrefix = prefix;
        String adapterId = abstractBeanFactory.resolveEmbeddedValue("${%s.databaseType:SIMPLE}".formatted(this.configurationPrefix));
        DatabaseFactory databaseFactory = databaseFactories.stream().filter(it -> it.getId().equals(adapterId)).findFirst().orElseThrow();
        databaseFactory.init(configurationPrefix, customParameters);
        this.database = databaseFactory.getPrimaryDatabase();
        this.transactionManager = databaseFactory.getTransactionManager();
        this.enumMapper = databaseFactory.getEnumMapper();
        this.classMapper = databaseFactory.getClassMapper();
        this.captionProvider = databaseFactory.getCaptionProvider();
    }
    private volatile List<StorageAdvice> advices;

    private List<StorageInterceptor> interceptors;

    private JsonUnmarshaller unmarshaller;

    private JsonMarshaller marshaller;

    private SerializationParameters serializationParameters;



    private final ThreadLocal<GlobalOperationContext> contexts = new ThreadLocal<>();


    private final GDiffPatcher patcher = new GDiffPatcher();
    private final Delta delta = new Delta();

    private DynamicCriterionsUpdater criterionsUpdater;


    private Map<String, List<SearchableProjectionHandler<BaseDocument, BaseSearchableProjection<BaseDocument>>>> projectionHandlers;


    @Override
    public <D extends BaseDocument> D loadDocument(EntityReference<D> ref, boolean forModification) {
        init();
        if (ref == null) {
            return null;
        }
        return loadDocument(ref.getType(), ref.getId(), forModification);
    }

    @Override
    public <D extends BaseDocument> D loadDocument(Class<D> cls, String id, boolean forModification) {
        init();
        return ExceptionUtils.wrapException(() -> loadDocument(cls, id, forModification, advices, 0));
    }

    @Override
    public <A extends BaseAsset> void saveAsset(A asset, boolean createNewVersion, String comment) {
        saveAsset(asset, createNewVersion, comment, null);
    }

    @Override
    public <A extends BaseAsset> void saveAsset(A asset, boolean createNewVersion, String comment, SaveAssetParameters params) {
        init();
        lockManager.withLock(asset, () -> transactionManager.withTransaction((tc) -> withGlobalContext(() ->
                saveAsset(asset, createNewVersion, comment, params, advices, tc, 0))));
    }

    @Override
    public <A extends BaseAsset> void deleteAsset(A asset) {
        deleteAsset(asset, null);
    }

    @Override
    public <A extends BaseAsset> void deleteAsset(A asset, DeleteAssetParameters params) {
        init();
        lockManager.withLock(asset, () -> transactionManager.withTransaction((tc) -> withGlobalContext(() ->
                deleteAsset(asset, advices, params, tc, 0))));
    }

    @Override
    public List<VersionInfo> getVersionsMetadata(Class<?> cls, String id) {
        init();
        return ExceptionUtils.wrapException(() -> transactionManager.withTransaction((tx) -> getVersionsMetadata(cls, id, advices, 0), false));
    }

    @Override
    public <A extends BaseAsset> void saveAsset(A asset, String comment) {
        init();
        saveAsset(asset, false, comment);
    }

    @Override
    public <A extends BaseAsset> List<A> searchAssets(Class<A> cls, SearchQuery query, boolean forModification) {
        init();
        return ExceptionUtils.wrapException(() -> searchAssets(cls, query, forModification, advices, 0));
    }

    @Override
    public <VA extends BaseVirtualAsset> List<VA> searchVirtualAssets(Class<VA> cls, SearchQuery query) {
        init();
        return ExceptionUtils.wrapException(()-> searchVirtualAssets(cls, query, advices, 0));
    }

    @Override
    public <VA extends BaseVirtualAsset> List<List<Object>> searchVirtualAssets(Class<VA> cls, AggregationQuery query) {
        init();
        return ExceptionUtils.wrapException(() -> searchVirtualAssets(cls, query, advices, 0));
    }

    private <VA extends BaseVirtualAsset> List<List<Object>> searchVirtualAssets(Class<VA> cls, AggregationQuery query, List<StorageAdvice> advices, int idx) throws Exception {
        if (idx == advices.size()) {
            return transactionManager.withTransaction((tx) -> {
                return database.searchVirtualAssets(cls, criterionsUpdater.updateQuery(cls.getName(), query));
            }, true);

        }
        return advices.get(idx).onSearchVirtualAssets(cls, query,  (cls2, query2)
                -> searchVirtualAssets(cls2, query2, advices, idx + 1));
    }

    private <VA extends BaseVirtualAsset> List<VA> searchVirtualAssets(Class<VA> cls, SearchQuery query, List<StorageAdvice> advices, int idx) throws Exception {
        if (idx == advices.size()) {
            return transactionManager.withTransaction((tx) -> {
                var result = database.searchVirtualAssets(cls, criterionsUpdater.updateQuery(cls.getName(), query));
                result.forEach(BaseVirtualAsset::seal);

                return result;
            }, true);
        }
        return advices.get(idx).onSearchVirtualAssets(cls, query,  (cls2, query2)
                -> searchVirtualAssets(cls2, query2, advices, idx + 1));
    }

    @Override
    public <A extends BaseAsset> List<A> searchAssets(Class<A> cls, SearchQuery query) {
        init();
        return searchAssets(cls, query, false);
    }

    @Override
    public <A extends BaseAsset> A loadAssetVersion(Class<A> cls, String id, int version) {
        init();
        return ExceptionUtils.wrapException(() -> loadAssetVersion(cls, id, version, advices, 0));
    }

    @Override
    public <A extends BaseAsset> A loadAsset(Class<A> cls, String id, boolean forModification) {
        init();
        return ExceptionUtils.wrapException(() -> loadAsset(cls, id, forModification, advices, 0));
    }

    @Override
    public <D extends BaseDocument> D loadDocumentVersion(Class<D> cls, String id, int version) {
        init();
        return ExceptionUtils.wrapException(() -> loadDocumentVersion(cls, id, version, advices, 0));
    }

    @Override
    public <T, D extends BaseDocument, I extends BaseSearchableProjection<D>, E extends FieldNameSupport & EqualitySupport & ArgumentType<T>>
    EntityReference<D> findUniqueDocumentReference(Class<I> projClass, E property, T propertyValue) {
        init();
        return ExceptionUtils.wrapException(() -> findUniqueDocumentReference(projClass, property, propertyValue, advices, 0));
    }

    @Override
    public <T, D extends BaseDocument, I extends BaseSearchableProjection<D>, E extends FieldNameSupport & EqualitySupport & ArgumentType<T>>
    D findUniqueDocument(Class<I> projClass, E property, T propertyValue, boolean forModification) {
        init();
        return loadDocument(findUniqueDocumentReference(projClass, property, propertyValue), forModification);
    }

    @Override
    public <T, D extends BaseDocument, I extends BaseSearchableProjection<D>, E extends FieldNameSupport & EqualitySupport & ArgumentType<T>>
    D findUniqueDocument(Class<I> projClass, E property, T propertyValue) {
        return this.findUniqueDocument(projClass, property, propertyValue, false);
    }

    @Override
    public <T, D extends BaseDocument, I extends BaseSearchableProjection<D>, E extends FieldNameSupport & EqualitySupport & ArgumentType<T>>
    Set<EntityReference<D>> getAllDocumentReferences(Class<I> projClass, E property, T propertyValue) {
        init();
        return ExceptionUtils.wrapException(() ->  getAllDocumentReferences(projClass, property, propertyValue, advices, 0));
    }

    @Override
    public <T, D extends BaseDocument, I extends BaseSearchableProjection<D>, E extends FieldNameSupport & EqualitySupport & ArgumentType<T>>
    Set<D> getAllDocuments(Class<I> projClass, E property, T propertyValue, boolean forModification) {
        init();
        return getAllDocumentReferences(projClass, property, propertyValue).stream().map((it) -> loadDocument(it, forModification)).collect(Collectors.toSet());
    }

    @Override
    public <A extends BaseAsset> List<List<Object>> searchAssets(Class<A> cls, AggregationQuery query) {
        init();
        return ExceptionUtils.wrapException(() -> searchAssets(cls, query, advices, 0));
    }

    @Override
    public <D extends BaseDocument, I extends BaseSearchableProjection<D>> List<I> searchDocuments(Class<I> cls, SearchQuery query) {
        init();
        return ExceptionUtils.wrapException(() -> searchDocuments(cls, query, advices, 0));
    }

    @Override
    public <D extends BaseDocument, I extends BaseSearchableProjection<D>> List<List<Object>> searchDocuments(Class<I> cls, AggregationQuery query) {
        init();
        return ExceptionUtils.wrapException(() -> searchDocuments(cls, query, advices, 0));
    }

    @Override
    public <D extends BaseDocument> void updateProjections(D doc, Class<?>... projectionClasses) {
        init();
        transactionManager.withTransaction((tc) -> updateProjectionsInternal(doc, null, true, projectionClasses));
    }

    @Override
    public <D extends BaseDocument> void saveDocument(D doc) {
        saveDocument(doc, false, null, new SaveDocumentParameters());
    }


    @Override
    public <D extends BaseDocument> void saveDocument(D doc, String comment) {
        saveDocument(doc, false, comment, new SaveDocumentParameters());
    }


    @Override
    public <D extends BaseDocument> void saveDocument(D doc, boolean createNewVersion, String comment) {
        saveDocument(doc, createNewVersion, comment, new SaveDocumentParameters());
    }


    @Override
    public <D extends BaseDocument> void saveDocument(D doc, boolean createNewVersion, String comment, SaveDocumentParameters params) {
        init();
        lockManager.withLock(doc, () -> transactionManager.withTransaction((tx) ->
                withGlobalContext(() -> saveDocument(doc, createNewVersion, comment, params, advices, tx, 0))));
    }


    @Override
    public <D extends BaseDocument> void deleteDocument(D doc) {
        deleteDocument(doc, new DeleteDocumentParameters());
    }

    @Override
    public <D extends BaseDocument> void deleteDocument(D doc, DeleteDocumentParameters params) {
        init();
        lockManager.withLock(doc, () -> transactionManager.withTransaction((tx) ->
                withGlobalContext(() -> deleteDocument(doc, params, advices, tx, 0))));
    }

    @Override
    public <D extends BaseIdentity> void updateCaptions(D entity) {

        updateCaptions(entity, new UpdateCaptionsParameters());
    }

    @Override
    public <D extends BaseIdentity> void updateCaptions(D entity, UpdateCaptionsParameters params) {
        init();
        lockManager.withLock(entity, () -> transactionManager.withTransaction((tx) ->
                withGlobalContext(() -> updateCaptions(entity, params, advices, tx, 0))));
    }

    @Override
    public <D extends BaseIdentity> List<EntityReference<D>> searchCaptions(Class<D> cls, String pattern, int limit) {
        init();
        return ExceptionUtils.wrapException(() -> transactionManager.withTransaction((tx) -> database.searchCaptions(cls, pattern, limit, LocaleUtils.getCurrentLocale()), false));
    }

    @Override
    public <T, A extends BaseAsset, E extends FieldNameSupport & EqualitySupport & ArgumentType<T>> A findUniqueAsset(Class<A> cls, E property,
                                                                                                                      T propertyValue,
                                                                                                                      boolean forModification) {
        init();
        return ExceptionUtils.wrapException(() -> findUniqueAsset(cls, property, propertyValue, forModification, advices, 0));
    }

    @Override
    public <A extends BaseAsset> void updateAggregatedData(A asset) {
        throw Xeption.forDeveloper("not implemented");
    }

    public<I extends BaseIdentity> void deleteCaption(I doc){
        ExceptionUtils.wrapException(() -> {
            database.deleteCaptions(doc.getClass(), doc.getId());
        });
    }
    @Override
    public <T, A extends BaseAsset, E extends FieldNameSupport & EqualitySupport & ArgumentType<T>> Set<A> getAllAssets(Class<A> cls, E property, T propertyValue, boolean forModification) {
        return ExceptionUtils.wrapException(() ->  getAllAssets(cls, property, propertyValue, forModification, advices, 0));
    }

    private <T, A extends BaseAsset, E extends FieldNameSupport & EqualitySupport & ArgumentType<T>> Set<A> getAllAssets(Class<A> cls, E property, T propertyValue, boolean forModification, List<StorageAdvice> advices, int idx) throws Exception {
        if (idx == advices.size()) {
            return transactionManager.withTransaction((tx) -> {
                var query = new SearchQuery();
                query.getPreferredFields().add(BaseSearchableProjection.Fields.document);
                if (propertyValue != null) {
                    query.getCriterions().add(SearchCriterion.eq(property, propertyValue));
                } else {
                    query.getCriterions().add(SearchCriterion.isNull(property));
                }
                return new HashSet<>(database.searchAssets(cls, query));
            }, true);
        }
        return advices.get(idx).onGetAllAssets(cls, property, propertyValue, forModification, (asset2, property2, propertyValue2, forModification2) ->
                getAllAssets(asset2, property2, propertyValue2, forModification2, advices, idx + 1));
    }

    private <T, A extends BaseAsset, E extends FieldNameSupport & EqualitySupport & ArgumentType<T>> A findUniqueAsset(Class<A> cls, E property,
                                                                                                                       T propertyValue,
                                                                                                                       boolean forModification, List<StorageAdvice> advices, int idx) throws Exception {
        if (idx == advices.size()) {
            return transactionManager.withTransaction((tx) -> {
                var query = new SearchQueryBuilder().where(propertyValue == null ?
                        SearchCriterion.isNull(property) :
                        SearchCriterion.eq(property, propertyValue)).build();
                var lst = database.searchAssets(cls, query);
                var size = lst.size();
                if (size == 0) {
                    return null;
                } else if (size == 1) {
                    return lst.get(0);
                } else {
                    throw Xeption.forAdmin(CoreL10nMessagesRegistryFactory
                            .Found_several_recordsMessage(cls.getName(), property.name, propertyValue == null ? null : propertyValue.toString()));
                }
            }, true);
        }
        return advices.get(idx).onFindUniqueAsset(cls, property, propertyValue, forModification, (cls2, property2, propertyValue2, forModification2) ->
                findUniqueAsset(cls2, property2, propertyValue2, forModification2, advices, idx + 1)
        );
    }

    private <D extends BaseIdentity> void updateCaptions(D entity, UpdateCaptionsParameters params, List<StorageAdvice> advices, ElsaTransactionContext tx, int idx) throws Exception {
        if (idx == advices.size()) {
            database.deleteCaptions(entity.getClass(), entity.getId());
            if (entity instanceof Localizable lz) {
                var names = new LinkedHashMap<Locale, String>();
                for (var locale : localesProvider.getSupportedLocales()) {
                    names.put(locale, lz.toString(locale));
                }
                database.updateCaptions(entity.getClass(), entity.getId(), names, true);
            } else {
                database.updateCaptions(entity.getClass(), entity.getId(), entity.toString(), true);
            }
            return;
        }
        advices.get(idx).onUpdateCaptions(entity, params, (entity2, params2) -> updateCaptions(entity2, params2, advices, tx, idx + 1));
    }

    private <D extends BaseDocument> void deleteDocument(D document, DeleteDocumentParameters params, List<StorageAdvice> storageAdvices, ElsaTransactionContext ctx, int idx) throws Exception {
        if (idx == storageAdvices.size()) {
            var udc = getUpdateDocumentContext(document, ctx);
            if (udc.oldDocument == null) {
                throw Xeption.forDeveloper("document %s id = %s is absent in db".formatted(document.getClass().getName(), document.getId()));
            }
            if (document.getVersionInfo().getRevision() != udc.oldDocument.getRevision()) {
                throw new IllegalArgumentException("revision conflict: expected = %s, actual: %s".formatted(
                        udc.oldDocument.getRevision(), document.getVersionInfo().getRevision()
                ));
            }
                for (var interceptor : interceptors) {
                    if(params == null || interceptor.isApplicableOnDelete(params)) {
                        interceptor.onDelete(document, udc.operationContext);
                    }
                }
            database.deleteDocument(document.getClass(), document.getId(), udc.oldDocument.getData().id());
            for (var metadata : database.getVersionsMetadata(document.getClass(), document.getId())) {
                database.deleteVersion(document.getClass(), document.getId(), metadata.getVersionNumber());
            }
            database.deleteCaptions(document.getClass(), document.getId());
            var handlers = projectionHandlers.get(document.getClass().getName());
            if (handlers != null) {
                for (var projectionHandler : handlers) {
                    database.deleteProjections(projectionHandler.getProjectionClass(), document.getId());
                }
            }
            return;
        }
        advices.get(idx).onDeleteDocument(document, params, (document2, params2) -> deleteDocument(document2, params2, advices, ctx, idx + 1));
    }

    private <D extends BaseDocument> void saveDocument(D doc, boolean createNewVersion, String comment, SaveDocumentParameters params,
                                                       List<StorageAdvice> advices, ElsaTransactionContext ctx, int idx) throws Exception {
        if (idx == advices.size()) {
            if (doc.getVersionInfo() == null) {
                doc.setVersionInfo(new VersionInfo());
            }
            var context = getUpdateDocumentContext(doc, ctx);
            var updatePreviousVersion = !createNewVersion && (context.oldDocument != null && context.oldDocument.getVersionNumber() > 0);
            byte[] previousVersionContent = null;
            if (updatePreviousVersion) {
                var os = new ByteArrayOutputStream();
                try (var is = new ByteArrayInputStream(IoUtils.gunzip(context.previousVersionFactory.createObject()
                        .getData().content()))) {
                    patcher.patch(IoUtils.gunzip(context.oldDocument.getData().content()), is, os);
                }
                previousVersionContent = IoUtils.gzip(os.toByteArray());
            }
                for (var interceptor : interceptors) {
                    if(params ==null || interceptor.isApplicableOnSave(params)) {
                        interceptor.onSave(doc, context.operationContext);
                    }
                }
            var now = Instant.now();
            doc.getVersionInfo().setValue(VersionInfo.Fields.revision, context.oldDocument == null ?
                    0 : context.oldDocument.getRevision() + 1);
            doc.getVersionInfo().setComment(comment);
            doc.getVersionInfo().setModified(now);
            doc.getVersionInfo().setModifiedBy(AuthContext.getCurrentUser());
            doc.getVersionInfo().setValue(VersionInfo.Fields.revision, doc.getVersionInfo().getRevision());
            doc.getVersionInfo().setVersionNumber(
                    context.oldDocument == null ? 0 : (createNewVersion ? context.oldDocument.getVersionNumber() + 1 : context.oldDocument.getVersionNumber())
            );

            var baos = new ByteArrayOutputStream();
            marshaller.marshal(doc, baos, false, serializationParameters);
            var data = baos.toByteArray();
            var baos3 = new ByteArrayOutputStream();
            try (var os = new GZIPOutputStream(baos3)) {
                IoUtils.copy(new ByteArrayInputStream(data), os);
            }
            var obj = new DatabaseObjectData();
            obj.setData(new DatabaseBinaryData(null, baos3.toByteArray()));
            obj.setComment(doc.getVersionInfo().getComment());
            obj.setModified(doc.getVersionInfo().getModified());
            obj.setModifiedBy(doc.getVersionInfo().getModifiedBy());
            obj.setRevision(doc.getVersionInfo().getRevision());
            obj.setVersionNumber(doc.getVersionInfo().getVersionNumber());
            database.saveDocument(doc.getId(), doc.getClass(), obj, context.oldDocument);
            if ((createNewVersion && context.oldDocument != null) || (context.oldDocument != null && updatePreviousVersion)) {
                var baos2 = new ByteArrayOutputStream();
                try (var writer = new GDiffWriter(baos2)) {
                    delta.compute(data, new GZIPInputStream(new ByteArrayInputStream(updatePreviousVersion ? previousVersionContent
                            : context.oldDocument.getData().content())), writer);
                    writer.flush();
                }
                var version = new DatabaseObjectData();
                version.setVersionNumber(updatePreviousVersion ? context.oldDocument.getVersionNumber() - 1 : context.oldDocument.getVersionNumber());
                version.setRevision(context.oldDocument.getRevision());
                version.setModifiedBy(context.oldDocument.getModifiedBy());
                version.setModified(context.oldDocument.getModified());
                version.setComment(context.oldDocument.getComment());
                version.setData(new DatabaseBinaryData(null, IoUtils.gzip(baos2.toByteArray())));
                database.saveDocumentVersion((Class<D>) doc.getClass(), doc.getId(), version,
                        updatePreviousVersion ? context.oldDocument.getData().id() : null);
            }
            if (doc instanceof Localizable lz) {
                var names = new LinkedHashMap<Locale, String>();
                for (var locale : localesProvider.getSupportedLocales()) {
                    names.put(locale, lz.toString(locale));
                }
                database.updateCaptions(doc.getClass(), doc.getId(), names, context.oldDocument == null);
            } else {
                database.updateCaptions(doc.getClass(), doc.getId(), doc.toString(), context.oldDocument == null);
            }
            updateProjectionsInternal(doc, context.operationContext, context.oldDocument != null);
            return;
        }
        advices.get(idx).onSaveDocument(doc, createNewVersion, comment, params, (doc2, createNewVersion2, comment2, params2) -> saveDocument(doc2, createNewVersion2, comment2, params2, advices, ctx, idx + 1));
    }

    private <D extends BaseDocument> UpdateDocumentContext<D> getUpdateDocumentContext(D doc, ElsaTransactionContext tc) throws Exception {
        var oldDocument = database.loadDocumentData((Class<D>) doc.getClass(), doc.getId());
        var docRevision = doc.getVersionInfo() == null ? -1 : doc.getVersionInfo().getRevision();
        if (docRevision == -1) {
            docRevision = oldDocument == null ? 0 : oldDocument.getRevision();
        }
        if (oldDocument != null && docRevision != oldDocument.getRevision()) {
            throw Xeption.forDeveloper("revision conflict with document %s %s, ".formatted(doc.getClass().getName(), doc.getId()) +
                    "db revision = %s, operation revision %s".formatted(oldDocument.getRevision(), doc.getVersionInfo().getRevision()));
        }
        D oldObject = null;
        if (oldDocument != null) {
            try (var is = new GZIPInputStream(new ByteArrayInputStream(oldDocument.getData().content()))) {
                oldObject = unmarshaller.unmarshal((Class<D>) doc.getClass(), is, serializationParameters);
            }
        }
        var previousVersionContentFactory = new ObjectFactoryWithException<DatabaseObjectData>() {

            DatabaseObjectData result;

            @Override
            public DatabaseObjectData createObject() throws Exception {
                if (oldDocument == null || oldDocument.getVersionNumber() == 0) {
                    return null;
                }
                if (result == null) {
                    result = database.loadVersion(doc.getClass(), doc.getId(),
                            oldDocument.getVersionNumber() - 1);
                }
                return result;
            }
        };
        var globalContext = contexts.get();
        if (globalContext == null) {
            globalContext = new GlobalOperationContext(doc, oldObject, tc);
            contexts.set(globalContext);
        }
        var localContext = new LocalOperationContext<>(oldObject);
        var operationContext = new OperationContext<>(globalContext, localContext);
        return new UpdateDocumentContext<>(oldDocument, previousVersionContentFactory, operationContext);
    }

    private <D extends BaseDocument> void updateProjectionsInternal(D doc, OperationContext<D> ctx, boolean update, Class<?>... indexClasses) throws Exception {
        var standardObject = doc;
        var handlers = projectionHandlers.get(standardObject.getClass().getName());
        if (handlers == null) {
            return;
        }
        for (var projectionHandler : handlers) {
            if (indexClasses.length == 0 || Arrays.stream(indexClasses).anyMatch(it -> it == projectionHandler.getProjectionClass())) {
                var description = domainMetaRegistry.getSearchableProjections().get(projectionHandler.getProjectionClass().getName());
                var projections = projectionHandler.createProjections(doc, Collections.emptySet());
                var wrappers = new ArrayList<DatabaseSearchableProjectionWrapper<BaseDocument, BaseSearchableProjection<BaseDocument>>>();
                for (var proj : projections) {
                    proj.setDocument(new EntityReference<>(doc));
                    String data = buildAggregatedData(proj, description, ctx);
                    wrappers.add(new DatabaseSearchableProjectionWrapper<>(
                            proj, domainMetaRegistry, data));
                }
                database.updateProjections(projectionHandler.getProjectionClass(), doc.getId(), wrappers, update);
            }
        }

    }

    private <D extends BaseDocument, I extends BaseSearchableProjection<D>> List<I> searchDocuments(Class<I> cls, SearchQuery query, List<StorageAdvice> advices, int idx) throws Exception {
        if (idx == advices.size()) {
            return transactionManager.withTransaction((tx) -> {
                return database.searchDocuments(cls, criterionsUpdater.updateQuery(cls.getName(), query));
            }, true);
        }
        return advices.get(idx).onSearchDocuments(cls, query, (cls2, query2) ->
                searchDocuments(cls2, query2, advices, idx + 1));
    }

    private <D extends BaseDocument, I extends BaseSearchableProjection<D>> List<List<Object>> searchDocuments(Class<I> cls, AggregationQuery query, List<StorageAdvice> advices, int idx) throws Exception {
        if (idx == advices.size()) {
            return transactionManager.withTransaction((tx) -> {
                return database.searchDocuments(cls, criterionsUpdater.updateQuery(cls.getName(), query));
            }, true);
        }
        return advices.get(idx).onSearchDocuments(cls, query, (cls2, query2) ->
                searchDocuments(cls2, query2, advices, idx + 1));
    }

    private <A extends BaseAsset> List<List<Object>> searchAssets(Class<A> cls, AggregationQuery query, List<StorageAdvice> advices, int idx) throws Exception {
        if (idx == advices.size()) {
            return transactionManager.withTransaction((tx) -> {
                return database.searchAssets(cls, criterionsUpdater.updateQuery(cls.getName(), query));
            }, true);
        }
        return advices.get(idx).onSearchAssets(cls, query, (cls2, query2) ->
                searchAssets(cls2, query2, advices, idx + 1));
    }

    private <T, D extends BaseDocument, I extends BaseSearchableProjection<D>, E extends FieldNameSupport & EqualitySupport & ArgumentType<T>>
    Set<EntityReference<D>> getAllDocumentReferences(Class<I> projClass, E property, T propertyValue, List<StorageAdvice> advices, int idx) throws Exception {
        if (idx == advices.size()) {
            return transactionManager.withTransaction((tx) -> {
                var query = new SearchQuery();
                query.getPreferredFields().add(BaseSearchableProjection.Fields.document);
                if (propertyValue != null) {
                    query.getCriterions().add(SearchCriterion.eq(property, propertyValue));
                } else {
                    query.getCriterions().add(SearchCriterion.isNull(property));
                }
                return database.searchDocuments(projClass, query).stream().map(BaseSearchableProjection::getDocument).collect(Collectors.toSet());
            }, true);
        }

        return advices.get(idx).onGetAllDocumentReferences(projClass, property, propertyValue, (projClass2, property2, propertyValue2) ->
                getAllDocumentReferences(projClass2, property2, propertyValue2, advices, idx + 1)
        );
    }

    private <T, D extends BaseDocument, I extends BaseSearchableProjection<D>, E extends FieldNameSupport & EqualitySupport & ArgumentType<T>>
    EntityReference<D> findUniqueDocumentReference(Class<I> projClass, E property, T propertyValue, List<StorageAdvice> advices, int idx) throws Exception {
        if (idx == advices.size()) {
            return transactionManager.withTransaction((tx) -> {
                var query = new SearchQuery();
                query.getPreferredFields().add(BaseSearchableProjection.Fields.document);
                if (propertyValue != null) {
                    query.getCriterions().add(SearchCriterion.eq(property, propertyValue));
                } else {
                    query.getCriterions().add(SearchCriterion.isNull(property));
                }
                var lst = database.searchDocuments(projClass, query);
                var size = lst.size();
                if (size == 0) {
                    return null;
                } else if (size == 1) {
                    return lst.get(0).getDocument();
                } else {
                    throw Xeption.forAdmin(CoreL10nMessagesRegistryFactory
                            .Found_several_recordsMessage(projClass.getName(), property.name, propertyValue == null ? null : propertyValue.toString()));
                }
            }, true);
        }
        return advices.get(idx).onFindUniqueDocumentReference(projClass, property, propertyValue, (projClass2, property2, propertyValue2) ->
                findUniqueDocumentReference(projClass2, property2, propertyValue2, advices, idx + 1));
    }

    private <D extends BaseDocument> D loadDocumentVersion(Class<D> cls, String objectId, int versionNumber,
                                                           List<StorageAdvice> advices, int idx) throws Exception {
        if (idx == advices.size()) {
            return transactionManager.withTransaction((tx) -> {
                var lastVersion = database.loadDocumentData(cls, objectId);
                if (lastVersion == null) {
                    throw new IllegalArgumentException("document of type %s with id %s is not found".formatted(cls.getName(), objectId));
                }
                var data = IoUtils.gunzip(lastVersion.getData().content());
                for (int n = lastVersion.getVersionNumber() - 1; n >= versionNumber; n--) {
                    var versionData = database.loadVersion(cls, objectId, n);
                    data = patcher.patch(data, IoUtils.gunzip(versionData.getData().content()));
                }
                return unmarshaller.unmarshal(cls, new ByteArrayInputStream(data), serializationParameters);
            }, true);
        }
        return advices.get(idx).onLoadDocumentVersion(cls, objectId, versionNumber, (cls2, id2, versionNumber2) ->
                loadDocumentVersion(cls2, id2, versionNumber2, advices, idx + 1));
    }

    private <A extends BaseAsset> A loadAsset(Class<A> cls, String id, boolean forModification, List<StorageAdvice> advices, int idx) throws Exception {
        if (idx == advices.size()) {
            return transactionManager.withTransaction((tx) ->{
                return database.loadAsset(cls, id);
            }, true);
        }
        return advices.get(0).onLoadAsset(cls, id, forModification, (cls2, id2, forModification2) ->
                loadAsset(cls2, id2, forModification2, advices, idx + 1)
        );
    }

    private <A extends BaseAsset> A loadAssetVersion(Class<A> cls, String id, int version, List<StorageAdvice> advices, int idx) throws Exception {
        if (idx == advices.size()) {
            return transactionManager.withTransaction((tx) -> {
                var data = database.loadVersion(cls, id, version);
                var is = new GZIPInputStream(new ByteArrayInputStream(data.getData().content()));
                return unmarshaller.unmarshal(cls, is, serializationParameters);
            },  true);
        }
        return advices.get(idx).onLoadAssetVersion(cls, id, version, (cls2, uid2, version2) ->
                loadAssetVersion(cls2, uid2, version2, advices, idx + 1)
        );
    }

    private <A extends BaseAsset> List<A> searchAssets(Class<A> cls, SearchQuery query, boolean forModification, List<StorageAdvice> advices, int idx) throws Exception {
        if (idx == advices.size()) {
            return transactionManager.withTransaction((tx) ->{
                return database.searchAssets(cls, criterionsUpdater.updateQuery(cls.getName(), query));
            }, true);
        }
        return advices.get(idx).onSearchAssets(cls, query, forModification, (cls2, query2, forModification2)
                -> searchAssets(cls2, query2, forModification2, advices, idx + 1));
    }

    private List<VersionInfo> getVersionsMetadata(Class<?> cls, String id, List<StorageAdvice> storageAdvices, int idx) throws Exception {
        init();
        if (idx == storageAdvices.size()) {
            return database.getVersionsMetadata(cls, id).stream().sorted(Comparator.comparing(VersionInfo::getVersionNumber)).toList();
        }
        return advices.get(idx).onGetVersionsMetadata(cls, () -> getVersionsMetadata(cls, id, advices, idx + 1));
    }

    private <A extends BaseAsset> void deleteAsset(A asset, List<StorageAdvice> storageAdvices, DeleteAssetParameters deleteAssetParameters, ElsaTransactionContext ctx, int idx) throws Exception {
        init();
        if (idx == storageAdvices.size()) {
            var uac = getUpdateAssetContext(asset, ctx);
            if (uac.oldAsset == null) {
                throw Xeption.forDeveloper("asset %s id = %s is absent in db".formatted(asset.getClass().getName(), asset.getId()));
            }
            if (asset.getVersionInfo() != null && uac.oldAsset.getAsset().getVersionInfo() != null && asset.getVersionInfo().getRevision() != uac.oldAsset.getAsset().getVersionInfo().getRevision()) {
                throw new IllegalArgumentException("revision conflict: expected = %s, actual: %s".formatted(
                        uac.oldAsset.getAsset().getVersionInfo().getRevision(), asset.getVersionInfo().getRevision()
                ));
            }
            for (var interceptor : interceptors) {
                if(deleteAssetParameters  == null || interceptor.isApplicableOnDelete(deleteAssetParameters)){
                    interceptor.onDelete(uac.oldAsset.getAsset(), uac.operationContext);
                }
            }
            database.deleteAsset(asset.getClass(), asset.getId());
            for (var metadata : database.getVersionsMetadata(asset.getClass(), asset.getId())) {
                database.deleteVersion(asset.getClass(), asset.getId(), metadata.getVersionNumber());
            }
            database.deleteCaptions(asset.getClass(), asset.getId());
            return;
        }
        advices.get(idx).onDeleteAsset(asset, (asset2) -> deleteAsset(asset2, advices, deleteAssetParameters, ctx, idx + 1));
    }

    private <A extends BaseAsset> void saveAsset(A asset, boolean createNewVersion, String comment, SaveAssetParameters params, List<StorageAdvice> storageAdvices, ElsaTransactionContext ctx, int idx) throws Exception {
        init();
        if (idx == storageAdvices.size()) {
            var uc = getUpdateAssetContext(asset, ctx);
            if(params == null) {
                for (var interceptor : interceptors) {
                    if(params == null || interceptor.isApplicableOnSave(params)) {
                        interceptor.onSave(asset, uc.operationContext());
                    }
                }
            }
            if (asset.getVersionInfo() == null) {
                asset.setVersionInfo(new VersionInfo());
            }
            var oldAsset = uc.oldAsset();
            if (oldAsset == null) {
                asset.getVersionInfo().setValue(VersionInfo.Fields.revision, 0);
            } else {
                if (oldAsset.getAsset().getVersionInfo() != null && asset.getVersionInfo().getRevision() != oldAsset.getAsset().getVersionInfo().getRevision()) {
                    throw new IllegalArgumentException("revision conflict: expected = %s, actual: %s".formatted(
                            oldAsset.getAsset().getVersionInfo().getRevision(), asset.getVersionInfo().getRevision()
                    ));
                }
                asset.getVersionInfo().setValue(VersionInfo.Fields.revision, oldAsset.getAsset().getVersionInfo().getRevision() + 1);
            }
            var description = domainMetaRegistry.getAssets().get(asset.getClass().getName());
            String aggregatedData = buildAggregatedData(asset, description, uc.operationContext);
            var now = Instant.now();
            asset.getVersionInfo().setModified(now);
            asset.getVersionInfo().setModifiedBy(AuthContext.getCurrentUser());
            asset.getVersionInfo().setComment(comment);
            asset.getVersionInfo().setVersionNumber(oldAsset == null ? 0 : oldAsset.getAsset().getVersionInfo().getVersionNumber() + 1);
            database.saveAsset(new DatabaseAssetWrapper<>(asset, aggregatedData), uc.oldAsset);
            if (createNewVersion && oldAsset != null) {
                var vi = new VersionInfo();
                vi.setVersionNumber(oldAsset.getAsset().getVersionInfo().getVersionNumber());
                vi.setModifiedBy(AuthContext.getCurrentUser());
                vi.setComment(comment);
                vi.setModified(now);
                var baos = new ByteArrayOutputStream();
                var gzipos = new GZIPOutputStream(baos);
                try {
                    marshaller.marshal(oldAsset.getAsset(), gzipos, false, serializationParameters);
                } finally {
                    gzipos.flush();
                    gzipos.close();
                }
                database.saveAssetVersion((Class<A>) asset.getClass(), asset.getId(), new DatabaseBinaryData(null, baos.toByteArray()), vi);
            }
            if (asset instanceof Localizable lz) {
                var names = new LinkedHashMap<Locale, String>();
                for (var locale : localesProvider.getSupportedLocales()) {
                    names.put(locale, lz.toString(locale));
                }
                database.updateCaptions(asset.getClass(), asset.getId(), names, oldAsset == null);
            } else {
                database.updateCaptions(asset.getClass(), asset.getId(), asset.toString(), oldAsset == null);
            }
            return;
        }
        advices.get(idx).onSave(asset, (asset2) -> saveAsset(asset2, createNewVersion, comment, params, advices, ctx, idx + 1));
    }


    private <A extends BaseIntrospectableObject,I extends BaseIdentity> String buildAggregatedData(A object, BaseSearchableDescription description, OperationContext<I> ctx) {
        var provider = (AggregatedDataProvider<A,I>)aggregatedDataProviders.get(object.getClass().getName());
        if(provider != null) {
            return ExceptionUtils.wrapException(()->provider.getAggregatedData( object, ctx));
        }
        var result = new AggregatedData(domainMetaRegistry);
        for (DatabasePropertyDescription prop : description.getProperties().values()) {
            if (prop.isUseInTextSearch()) {
                result.aggregate(object.getValue(prop.getId()));
            }
        }
        for (DatabaseCollectionDescription coll : description.getCollections().values()) {
            if (coll.isUseInTextSearch()) {
                result.aggregate(object.getCollection(coll.getId()));
            }
        }
        return result.getAggregatedData();
    }

    private <A extends BaseAsset> UpdateAssetContext<A> getUpdateAssetContext(A asset, ElsaTransactionContext ctx) throws Exception {
        if (asset.getVersionInfo() == null) {
            asset.setVersionInfo(new VersionInfo());
        }
        var oldAssetWrapperData = database.loadAssetWrapper((Class<A>) asset.getClass(), asset.getId());
        var revision = asset.getVersionInfo().getRevision();
        if (revision == -1) {
            revision = oldAssetWrapperData == null ? 0 : oldAssetWrapperData.getAsset().getVersionInfo().getRevision() + 1;
        }
        var oldAsset = oldAssetWrapperData == null ? null : oldAssetWrapperData.getAsset();
        if (oldAsset != null && revision != oldAsset.getVersionInfo().getRevision()) {
            throw Xeption.forDeveloper("revision conflict with asset %s %s, db revision = %s, operation revision %s"
                    .formatted(asset.getClass().getName(), asset.getId(),
                            oldAsset.getVersionInfo().getRevision(), revision));
        }
        var globalContext = contexts.get();
        if (globalContext == null) {
            globalContext = new GlobalOperationContext(asset, oldAsset, ctx);
            contexts.set(globalContext);
        }
        var localContext = new LocalOperationContext<>(oldAsset);
        var operationContext = new OperationContext<>(globalContext, localContext);
        return new UpdateAssetContext<>(oldAssetWrapperData, operationContext);
    }


    private void withGlobalContext(RunnableWithException runnable) {
        ExceptionUtils.wrapException(() -> {
            var owner = contexts.get() == null;
            try {
                runnable.run();
            } finally {
                if (owner) {
                    contexts.remove();
                }
            }
        });
    }

    public void resetClassesCache(){
        advices = null;
    }

    private void init() {
        if (advices == null) {
            synchronized (this) {
                if (advices == null) {
                    interceptors = factory.getBeansOfType(StorageInterceptor.class).values().stream().sorted(Comparator.comparing(StorageInterceptor::getPriority)).toList();
                    serializationParameters = new SerializationParameters();
                    serializationParameters.setPrettyPrint(false);
                    serializationParameters.setEntityReferenceCaptionSerializationStrategy(SerializationParameters.EntityReferenceCaptionSerializationStrategy.ONLY_NOT_CACHED);
                    serializationParameters.setEnumSerializationStrategy(SerializationParameters.EnumSerializationStrategy.ID);
                    serializationParameters.setEntityReferenceTypeSerializationStrategy(SerializationParameters.EntityReferenceTypeSerializationStrategy.ABSTRACT_CLASS_ID);
                    serializationParameters.setClassSerializationStrategy(SerializationParameters.ClassSerializationStrategy.ID);
                    //noinspection rawtypes
                    criterionsUpdater = new DynamicCriterionsUpdater((List) factory.getBeansOfType(DynamicCriterionHandler.class).values().stream().toList());
                    projectionHandlers = new HashMap<>();
                    marshaller = factory.getBean(JsonMarshaller.class);
                    unmarshaller = factory.getBean(JsonUnmarshaller.class);
                    factory.getBeansOfType(SearchableProjectionHandler.class).values().forEach(h -> {
                        var lst = projectionHandlers.computeIfAbsent(h.getDocumentClass().getName(), k -> new ArrayList<>());
                        lst.add(h);
                    });
                    advices = factory.getBeansOfType(StorageAdvice.class).values().stream().sorted(Comparator.comparing(StorageAdvice::getPriority)).toList();
                    factory.getBeansOfType(AggregatedDataProvider.class).values().forEach(h -> {
                        aggregatedDataProviders.put(h.getObjectClass().getName(), h);
                    });
                }
            }
        }
    }

    private <D extends BaseDocument> D loadDocument(Class<D> cls, String id, boolean forModification, List<StorageAdvice> advices, int idx) throws Exception {
        if (idx == advices.size()) {
            return transactionManager.withTransaction((tx) ->{
                var documentData = database.loadDocumentData(cls, id);
                if (documentData == null) {
                    return null;
                }
                try (var is = new GZIPInputStream(new ByteArrayInputStream(documentData.getData().content()))) {
                    return unmarshaller.unmarshal(cls, is, serializationParameters);
                }
            }, true);

        }
        return advices.get(idx).onLoadDocument(cls, id, forModification, (cls2, id2, forModificationInt2) ->
                loadDocument(cls2, id2, forModificationInt2, advices, idx + 1));
    }

    @Override
    public <I extends BaseIdentity> String getCaption(Class<I> type, String id, Locale currentLocale) {
        var locale = currentLocale;
        if(locale == null){
            locale = LocaleUtils.getCurrentLocale();
        }
        var oldLocale = LocaleUtils.getCurrentLocale();
        LocaleUtils.setCurrentLocale(locale);
        try{
            var ref = new EntityReference<BaseIdentity>();
            ref.setType((Class<BaseIdentity>) type);
            ref.setId(id);
            return captionProvider.getCaption(ref);
        } finally {
            LocaleUtils.setCurrentLocale(oldLocale);
        }
    }

    @Override
    public <RP> RP performNativeOperation(CallableWithExceptionAndArgument<RP, ElsaTransactionContext> operation) {
        return ExceptionUtils.wrapException(() -> transactionManager.withTransaction(tx ->
                database.performNativeOperation(operation, tx), false));
    }

    public CaptionProvider getCaptionProvider() {
        return captionProvider;
    }

    public ClassMapper getClassMapper() {
        return this.classMapper;
    }

    public EnumMapper getEnumMapper() {
        return this.enumMapper;
    }

    public ElsaTransactionManager getTransactionManager() {
        return this.transactionManager;
    }


    private record UpdateAssetContext<A extends BaseAsset>(DatabaseAssetWrapper<A> oldAsset,
                                                           OperationContext<A> operationContext) {
    }

    private record UpdateDocumentContext<D extends BaseDocument>(DatabaseObjectData oldDocument,
                                                                 ObjectFactoryWithException<DatabaseObjectData> previousVersionFactory,
                                                                 OperationContext<D> operationContext) {
    }

}
