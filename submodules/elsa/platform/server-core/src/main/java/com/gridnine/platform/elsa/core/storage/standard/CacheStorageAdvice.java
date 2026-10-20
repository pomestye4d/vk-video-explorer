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

import com.gridnine.platform.elsa.common.core.model.common.BaseIdentity;
import com.gridnine.platform.elsa.common.core.model.common.CallableWithExceptionAnd3Arguments;
import com.gridnine.platform.elsa.common.core.model.common.CallableWithExceptionAnd4Arguments;
import com.gridnine.platform.elsa.common.core.model.common.Xeption;
import com.gridnine.platform.elsa.common.core.model.domain.*;
import com.gridnine.platform.elsa.common.core.search.ArgumentType;
import com.gridnine.platform.elsa.common.core.search.EqualitySupport;
import com.gridnine.platform.elsa.common.core.search.FieldNameSupport;
import com.gridnine.platform.elsa.core.cache.CacheManager;
import com.gridnine.platform.elsa.core.cache.CacheMetadataProvider;
import com.gridnine.platform.elsa.core.cache.CachedValue;
import com.gridnine.platform.elsa.core.cache.KeyValueCache;
import com.gridnine.platform.elsa.core.storage.Storage;
import com.gridnine.platform.elsa.core.storage.StorageAdvice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@SuppressWarnings({"rawtypes", "unchecked"})
public class CacheStorageAdvice implements StorageAdvice {

    private final Object nullValue = new BaseIdentity() {
    };

    private final EntityReference nullObjectReference = new EntityReference<>(UUID.randomUUID().toString(), BaseIdentity.class, null);

    @Autowired
    private CacheMetadataProvider cacheMetadataProvider;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private Storage storage;

    private final Map<String, KeyValueCache<String, ?>> resolveCaches = new ConcurrentHashMap<>();

    private final Map<String, Map<String, KeyValueCache<String, EntityReference>>> findCaches = new ConcurrentHashMap<>();

    private final Map<String, Map<String, KeyValueCache<String, Set>>> getAllCaches = new ConcurrentHashMap<>();


    @Autowired
    private Environment env;

    @Override
    public double getPriority() {
        return 10;
    }

    @Override
    public <D extends BaseDocument> D onLoadDocument(Class<D> cls, String id, boolean forModification, CallableWithExceptionAnd3Arguments<D, Class<D>, String, Boolean> callback) throws Exception {
        return onResolve(cls, id, forModification, callback);
    }

    @Override
    public <A extends BaseAsset> A onLoadAsset(Class<A> cls, String id, boolean forModification, CallableWithExceptionAnd3Arguments<A, Class<A>, String, Boolean> callback) throws Exception {
        return onResolve(cls, id, forModification, callback);
    }


    @Override
    public <T, D extends BaseDocument, I extends BaseSearchableProjection<D>, E extends FieldNameSupport & EqualitySupport & ArgumentType<T>> EntityReference<D>
    onFindUniqueDocumentReference(Class<I> projClass, E property, T propertyValue,
                                  CallableWithExceptionAnd3Arguments<EntityReference<D>, Class<I>, E, T> callback) throws Exception {
        if (!cacheMetadataProvider.isCacheFind(projClass, property.name)) {
            return callback.call(projClass, property, propertyValue);
        }
        var cache = this.<D>getOrCreateFindCache(projClass, property.name);
        var propValue = toString(propertyValue);
        var oldValue = cache.get(propValue);
        if (oldValue != null && oldValue.value() != null) {
            return oldValue.value() == nullObjectReference ? null : oldValue.value();
        }
        var ar = callback.call(projClass, property, propertyValue);
        if (ar != null) {
            ar.seal();
        }
        var newValue = new CachedValue<EntityReference<D>>(System.nanoTime(), ar == null ? nullObjectReference :
                ar);
        cache.replace(propValue, oldValue, newValue);
        return newValue.value() == nullObjectReference ? null : newValue.value();
    }

    @Override
    public <T, A extends BaseAsset, E extends FieldNameSupport & EqualitySupport & ArgumentType<T>> A onFindUniqueAsset(Class<A> cls, E property, T propertyValue,
                                                                                                                        boolean forModification, CallableWithExceptionAnd4Arguments<A, Class<A>, E, T, Boolean> callback) throws Exception {
        if (!cacheMetadataProvider.isCacheFind(cls, property.name)) {
            return callback.call(cls, property, propertyValue, forModification);
        }
        var cache = this.<A>getOrCreateFindCache(cls, property.name);
        var propValue = toString(propertyValue);
        var oldValue = cache.get(propValue);
        if (oldValue != null && oldValue.value() != null) {
            return oldValue.value() == nullObjectReference ? null : onResolve(cls, oldValue.value().getId(), forModification,
                    (cls2, id2, forModification2) -> storage.loadAsset(cls2, id2, forModification2));
        }
        var actualStorageResult = callback.call(cls, property, propertyValue, forModification);
        EntityReference<A> ar = null;
        if (actualStorageResult != null) {
            ar = new EntityReference<>(actualStorageResult);
            ar.seal();
        }
        var newValue = new CachedValue<EntityReference<A>>(
                System.nanoTime(),
                ar == null ? nullObjectReference : ar
        );
        cache.replace(propValue, oldValue, newValue);
        return actualStorageResult;
    }

    @Override
    public <T, D extends BaseDocument, I extends BaseSearchableProjection<D>, E extends FieldNameSupport & EqualitySupport & ArgumentType<T>> Set<EntityReference<D>>
    onGetAllDocumentReferences(Class<I> projClass, E property, T propertyValue, CallableWithExceptionAnd3Arguments<Set<EntityReference<D>>, Class<I>, E, T> callback) throws Exception {
        if (!cacheMetadataProvider.isCacheGetAll(projClass, property.name)) {
            return callback.call(projClass, property, propertyValue);
        }
        var propValueStr = toString(propertyValue);
        var cache = this.<D>getOrCreateGetAllCache(projClass, property.name);
        var oldValue = cache.get(propValueStr);
        if (oldValue != null && oldValue.value() != null) {
            return oldValue.value();
        }
        var actualStorageResult = callback.call(projClass, property, propertyValue);
        actualStorageResult.forEach(EntityReference::seal);
        var newValue = new CachedValue<>(
                System.nanoTime(), actualStorageResult);

        cache.replace(propValueStr, oldValue, newValue);
        return newValue.value();
    }

    @Override
    public <T, A extends BaseAsset, E extends FieldNameSupport & EqualitySupport & ArgumentType<T>> Set<A> onGetAllAssets(Class<A> cls, E property, T propertyValue, boolean forModification, CallableWithExceptionAnd4Arguments<Set<A>, Class<A>, E, T, Boolean> callbackObject) throws Exception {
        if (forModification || !cacheMetadataProvider.isCacheGetAll(cls, property.name)) {
            return callbackObject.call(cls, property, propertyValue, forModification);
        }
        var propValueStr = toString(propertyValue);
        var cache = this.<A>getOrCreateGetAllCache(cls, property.name);
        var oldValue = cache.get(propValueStr);
        if (oldValue != null && oldValue.value() != null) {
            var result = new HashSet<A>();
            for (var ref : oldValue.value()) {
                result.add(onResolve(cls, ref.getId(), false, (cls2, id2, forModification2) -> storage.loadAsset(cls2, id2, forModification2)));
            }
            return result;
        }
        var actualStorageResult = callbackObject.call(cls, property, propertyValue, false);
        actualStorageResult.forEach(A::seal);
        var refs = actualStorageResult.stream().map(EntityReference::new).collect(Collectors.toSet());
        refs.forEach(EntityReference::seal);
        var newValue = new CachedValue(
                System.nanoTime(), refs
        );
        cache.replace(propValueStr, oldValue, newValue);
        return actualStorageResult;
    }

    private <D extends BaseIdentity> D onResolve(
            Class<D> cls,
            String id,
            boolean forModification,
            CallableWithExceptionAnd3Arguments<D, Class<D>, String, Boolean> callback) throws Exception {
        if (forModification || !cacheMetadataProvider.isCacheResolve(cls)) {
            return callback.call(cls, id, forModification);
        }
        var cache = getOrCreateResolveCache(cls);
        var oldValue = cache.get(id);
        if (oldValue != null && oldValue.value() != null) {
            return oldValue.value() == nullValue ? null : oldValue.value();
        }
        var ar = callback.call(cls, id, false);
        if(ar != null){
            ((Sealable) ar).seal();
        }
        var newValue = new CachedValue<>(System.nanoTime(), ar == null ? (D) nullValue : ar);
        cache.replace(id, oldValue, newValue);
        return newValue.value() == nullValue ? null : newValue.value();
    }

    public <I extends BaseIdentity> void invalidateResolveCache(Class<I> cls, String id) {
        getOrCreateResolveCache(cls).put(id, new CachedValue<>(System.nanoTime(), null));
    }

    private <D> KeyValueCache<String, D> getOrCreateResolveCache(Class<D> cls) {
        var cache = resolveCaches.get(cls.getName());
        if (cache == null) {
            cache = resolveCaches.computeIfAbsent(cls.getName(), (className) -> {
                var capacityStr = env.getProperty("cache.resolve.capacity.%s".formatted(className));
                if (capacityStr == null) {
                    capacityStr = env.getProperty("cache.resolve.capacity.default", "1000");
                }
                var capacity = Integer.parseInt(capacityStr);
                var expirationInSecondsStr = env.getProperty("cache.resolve.expiration.%s".formatted(className));
                if (expirationInSecondsStr == null) {
                    expirationInSecondsStr = env.getProperty("cache.resolve.expiration.default", "3600");
                }
                var expirationInSeconds = Integer.parseInt(expirationInSecondsStr);
                return cacheManager.createKeyValueCache(String.class, EntityReference.class, "resolve_%s".formatted(className), capacity, expirationInSeconds);
            });
        }
        return (KeyValueCache<String, D>) cache;
    }

    private <E extends BaseIdentity> KeyValueCache<String, EntityReference<E>> getOrCreateFindCache(Class<?> cls, String fieldName) {
        var className = cls.getName();
        var caches = findCaches.get(className);
        if (caches == null) {
            caches = findCaches.computeIfAbsent(className, (cn) -> new ConcurrentHashMap<>());
        }
        var cache = caches.get(fieldName);
        if (cache == null) {
            cache = caches.computeIfAbsent(fieldName, (fn) -> {
                var capacityStr = env.getProperty("cache.find.capacity.%s.%s".formatted(className, fn));
                if (capacityStr == null) {
                    capacityStr = env.getProperty("cache.find.capacity.default", "1000");
                }
                var capacity = Integer.parseInt(capacityStr);
                var expirationInSecondsStr = env.getProperty("cache.find.expiration.%s.%s".formatted(className, fn));
                if (expirationInSecondsStr == null) {
                    expirationInSecondsStr = env.getProperty("cache.find.expiration.default", "3600");
                }
                var expirationInSeconds = Integer.parseInt(expirationInSecondsStr);
                return cacheManager.createKeyValueCache(String.class, EntityReference.class, "find_%s_%s".formatted(className, fn),
                        capacity, expirationInSeconds);
            });
        }
        return (KeyValueCache) cache;
    }

    private <E extends BaseIdentity> KeyValueCache<String, Set<EntityReference<E>>> getOrCreateGetAllCache(Class<?> cls, String fieldName) {
        var className = cls.getName();
        var caches = getAllCaches.get(className);
        if (caches == null) {
            caches = getAllCaches.computeIfAbsent(className, (clName) -> new ConcurrentHashMap<>());
        }
        var cache = caches.get(fieldName);
        if (cache == null) {
            cache = caches.computeIfAbsent(fieldName, (fn) -> {
                var capacityStr = env.getProperty("cache.getAll.capacity.%s.%s".formatted(className, fn));
                if (capacityStr == null) {
                    capacityStr = env.getProperty("cache.getAll.capacity.default", "1000");
                }
                var capacity = Integer.parseInt(capacityStr);
                var expirationInSecondsStr = env.getProperty("cache.getAll.expiration.%s.%s".formatted(className, fn));
                if (expirationInSecondsStr == null) {
                    expirationInSecondsStr = env.getProperty("cache.getAll.expiration.default", "3600");
                }
                var expirationInSeconds = Integer.parseInt(expirationInSecondsStr);
                return cacheManager.createKeyValueCache(String.class, Set.class, "getAll_%s_%s".formatted(className, fn),
                        capacity, expirationInSeconds);
            });
        }
        return (KeyValueCache) cache;
    }

    public void invalidateFindCache(Class<?> cls, String propertyName, Object value) {
        getOrCreateFindCache(cls, propertyName).put(toString(value), new CachedValue<>(System.nanoTime(), null));
    }

    public void invalidateGetAllCache(Class<?> cls, String propertyName, Object value) {
        getOrCreateGetAllCache(cls, propertyName).put(toString(value), new CachedValue<>(System.nanoTime(), null));
    }

    static String toString(Object propertyValue) {
        if (propertyValue == null) {
            return "$_null";
        }
        if (propertyValue instanceof Enum<?> en) {
            return en.name();
        }
        if (propertyValue instanceof String str) {
            return str;
        }
        if (propertyValue instanceof Number num) {
            return num.toString();
        }
        if (propertyValue instanceof EntityReference<?> ref) {
            return ref.getId().toString();
        }
        if (propertyValue instanceof UUID uuid) {
            return uuid.toString();
        }
        throw Xeption.forDeveloper("unsupported property value of type %s".formatted(propertyValue.getClass().getName()));
    }

}
