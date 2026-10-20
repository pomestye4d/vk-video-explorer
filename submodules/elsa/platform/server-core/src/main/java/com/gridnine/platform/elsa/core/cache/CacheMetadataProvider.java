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

package com.gridnine.platform.elsa.core.cache;

import com.gridnine.platform.elsa.common.core.model.common.BaseIdentity;
import com.gridnine.platform.elsa.common.meta.common.BaseElementWithId;
import com.gridnine.platform.elsa.common.meta.domain.DatabasePropertyDescription;
import com.gridnine.platform.elsa.common.meta.domain.DomainMetaRegistry;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CacheMetadataProvider {

    private final Map<String, Boolean> resolveCache = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Boolean>> findCache = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> findProperties = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> getAllProperties = new ConcurrentHashMap<>();
    private final Map<String, Boolean> cacheCaption = new ConcurrentHashMap<>();
    private final Map<String, Boolean> cacheLocalizedCaption = new ConcurrentHashMap<>();

    @Autowired
    private DomainMetaRegistry domainMetaRegistry;

    public <I extends BaseIdentity> boolean isCacheResolve(Class<I> cls) {
        var className = cls.getName();
        var res = resolveCache.get(className);
        if (res != null) {
            return res;
        }
        {
            var descr = domainMetaRegistry.getDocuments().get(className);
            if (descr != null) {
                res = descr.isCacheResolve();
                resolveCache.put(className, res);
                return res;
            }
        }
        var descr = domainMetaRegistry.getAssets().get(className);
        res = descr.isCacheResolve();
        resolveCache.put(className, res);
        return res;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isCacheFind(Class<?> cls, String propertyName) {
        var className = cls.getName();
        var c1 = findCache.get(className);
        if (c1 == null) {
            c1 = new ConcurrentHashMap<>();
            findCache.put(className, c1);
        }
        var res = c1.get(propertyName);
        if (res != null) {
            return res;
        }
        {
            var descr = domainMetaRegistry.getSearchableProjections().get(className);
            if (descr != null) {
                res = descr.getProperties().get(propertyName).isCacheFind();
                c1.put(propertyName, res);
                return res;
            }
        }
        var descr = domainMetaRegistry.getAssets().get(className);
        DatabasePropertyDescription dpd = descr.getProperties().get(propertyName);
        res = dpd == null? false: dpd.isCacheFind();
        c1.put(propertyName, res);
        return res;
    }


    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isCacheGetAll(Class<?> cls, String propertyName) {
        var className = cls.getName();
        var c1 = findCache.get(className);
        if (c1 == null) {
            c1 = new ConcurrentHashMap<>();
            findCache.put(className, c1);
        }
        var res = c1.get(propertyName);
        if (res != null) {
            return res;
        }
        {
            var descr = domainMetaRegistry.getSearchableProjections().get(className);
            if (descr != null) {
                res = descr.getProperties().get(propertyName).isCacheGetAll();
                c1.put(propertyName, res);
                return res;
            }
        }
        var descr = domainMetaRegistry.getAssets().get(className);
        res = descr.getProperties().get(propertyName).isCacheGetAll();
        c1.put(propertyName, res);
        return res;
    }

    public Set<String> getFindCacheProperties(Class<?> cls) {
        var className = cls.getName();
        var res = findProperties.get(className);
        if (res != null) {
            return res;
        }
        {
            var descr = domainMetaRegistry.getSearchableProjections().get(className);
            if (descr != null) {
                res = descr.getProperties().values().stream().filter(DatabasePropertyDescription::isCacheFind)
                        .map(BaseElementWithId::getId).collect(Collectors.toSet());
                findProperties.put(className, res);
                return res;
            }
        }
        var descr = domainMetaRegistry.getAssets().get(className);
        res = descr.getProperties().values().stream().filter(DatabasePropertyDescription::isCacheFind)
                .map(BaseElementWithId::getId).collect(Collectors.toSet());
        findProperties.put(className, res);
        return res;
    }

    public Set<String> getGetAllCacheProperties(Class<?> cls) {
        var className = cls.getName();
        var res = getAllProperties.get(className);
        if (res != null) {
            return res;
        }
        {
            var descr = domainMetaRegistry.getSearchableProjections().get(className);
            if (descr != null) {
                res = descr.getProperties().values().stream().filter(DatabasePropertyDescription::isCacheGetAll)
                        .map(BaseElementWithId::getId).collect(Collectors.toSet());
                getAllProperties.put(className, res);
                return res;
            }
        }
        var descr = domainMetaRegistry.getAssets().get(className);
        res = descr.getProperties().values().stream().filter(DatabasePropertyDescription::isCacheGetAll)
                .map(BaseElementWithId::getId).collect(Collectors.toSet());
        getAllProperties.put(className, res);
        return res;
    }

    public boolean isCacheCaption(Class<?> cls) {
        var className = cls.getName();
        var res = cacheCaption.get(className);
        if (res != null) {
            return res;
        }
        var docDescr = domainMetaRegistry.getDocuments().get(className);
        if (docDescr != null) {
            res = (docDescr.isCacheCaption() || docDescr.isCacheResolve()) && docDescr.getLocalizableCaptionExpression() == null;
            cacheCaption.put(className, res);
            return res;
        }
        var assetDescr = domainMetaRegistry.getAssets().get(className);
        if (assetDescr != null) {
            res = (assetDescr.isCacheCaption() || assetDescr.isCacheResolve()) && assetDescr.getLocalizableCaptionExpression() == null;
            cacheCaption.put(className, res);
            return res;
        }
        cacheCaption.put(className, false);
        return false;
    }

    public boolean isCacheLocalizedCaption(Class<?> cls) {
        var className = cls.getName();
        var res = cacheLocalizedCaption.get(className);
        if (res != null) {
            return res;
        }
        var docDescr = domainMetaRegistry.getDocuments().get(className);
        if (docDescr != null) {
            res = (docDescr.isCacheCaption() || docDescr.isCacheResolve()) && docDescr.getLocalizableCaptionExpression() != null;
            cacheLocalizedCaption.put(className, res);
            return res;
        }
        var assetDescr = domainMetaRegistry.getAssets().get(className);
        if (assetDescr != null) {
            res = (assetDescr.isCacheCaption() || assetDescr.isCacheResolve()) && assetDescr.getLocalizableCaptionExpression() != null;
            cacheLocalizedCaption.put(className, res);
            return res;
        }
        cacheLocalizedCaption.put(className, false);
        return false;
    }
}
