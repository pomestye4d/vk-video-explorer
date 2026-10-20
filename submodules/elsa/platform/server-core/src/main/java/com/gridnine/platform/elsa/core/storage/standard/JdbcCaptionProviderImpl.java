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
import com.gridnine.platform.elsa.common.core.model.domain.CaptionProvider;
import com.gridnine.platform.elsa.common.core.model.domain.EntityReference;
import com.gridnine.platform.elsa.common.core.utils.ExceptionUtils;
import com.gridnine.platform.elsa.common.core.utils.Lazy;
import com.gridnine.platform.elsa.common.core.utils.LocaleUtils;
import com.gridnine.platform.elsa.common.core.utils.TextUtils;
import com.gridnine.platform.elsa.core.cache.CacheManager;
import com.gridnine.platform.elsa.core.cache.CacheMetadataProvider;
import com.gridnine.platform.elsa.core.cache.CachedValue;
import com.gridnine.platform.elsa.core.cache.KeyValueCache;
import com.gridnine.platform.elsa.core.storage.Storage;
import com.gridnine.platform.elsa.core.storage.database.Database;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JdbcCaptionProviderImpl implements CaptionProvider {

    private final Map<String, KeyValueCache<String, String>> captionsCache = new ConcurrentHashMap<>();

    private final Map<String, KeyValueCache<String, Map<Locale, String>>> localizedCaptionsCache = new ConcurrentHashMap<>();

    private final Environment env;

    private final CacheMetadataProvider cacheMetadataProvider;

    private final Lazy<Database> database;

    private final CacheManager cacheManager;


    private final SupportedLocalesProvider supportedLocalesProvider;
    private final String nullString = TextUtils.generateUUID();

    public JdbcCaptionProviderImpl(Environment env, CacheMetadataProvider cacheMetadataProvider, Lazy<Database> database, CacheManager cacheManager, SupportedLocalesProvider supportedLocalesProvider) {
        this.env = env;
        this.cacheMetadataProvider = cacheMetadataProvider;
        this.database = database;
        this.cacheManager = cacheManager;
        this.supportedLocalesProvider = supportedLocalesProvider;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public String getCaption(EntityReference<?> ref) {
        return ExceptionUtils.wrapException(() ->{
            if (cacheMetadataProvider.isCacheCaption(ref.getType())) {
                var cache = getOrCreateCaptionCache(captionsCache, ref.getType(), String.class);
                var oldValue = cache.get(ref.getId());
                if (oldValue != null && oldValue.value() != null) {
                    return oldValue.value().equals(nullString) ? null : oldValue.value();
                }
                var ar = database.getObject().getCaption(ref.getType(), ref.getId());
                var newValue = new CachedValue<>(System.nanoTime(), ar == null ? nullString : ar);
                cache.replace(ref.getId(), oldValue, newValue);
                return Objects.equals(newValue.value(), nullString) ? null : newValue.value();
            }
            if (cacheMetadataProvider.isCacheLocalizedCaption(ref.getType())) {
                var cache = getOrCreateCaptionCache((Map) localizedCaptionsCache, ref.getType(), Map.class);
                var oldValue = (CachedValue<Map<Locale, String>>) cache.get(ref.getId());
                if (oldValue != null && oldValue.value() != null) {
                    return oldValue.value().get(LocaleUtils.getCurrentLocale());
                }
                var res = new HashMap<Locale, String>();
                for(var loc: supportedLocalesProvider.getSupportedLocales()){
                    var ar = database.getObject().getCaption(ref.getType(), ref.getId(), loc);
                    res.put(loc, ar);
                }
                var newValue = new CachedValue<>(System.nanoTime(), res);
                cache.replace(ref.getId(), oldValue, newValue);
                return res.get(LocaleUtils.getCurrentLocale());
            }
            return ref.getCaption();
        });


    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void invalidateCaptionsCache(Class<?> cls, String id) {
        if (cacheMetadataProvider.isCacheCaption(cls)) {
            getOrCreateCaptionCache(captionsCache, cls, String.class).put(id, new CachedValue<>(System.nanoTime(), null));
            return;
        }
        if (cacheMetadataProvider.isCacheLocalizedCaption(cls)) {
            getOrCreateCaptionCache((Map) localizedCaptionsCache, cls, Map.class).put(id, new CachedValue<>(System.nanoTime(), null));
        }
    }

    private <D> KeyValueCache<String, D> getOrCreateCaptionCache(Map<String, KeyValueCache<String, D>> caches, Class<?> objectClass, Class<D> cacheClass) {
        var className = objectClass.getName();
        var cache = caches.get(className);
        if (cache == null) {
            synchronized (this) {
                cache = caches.get(className);
                if (cache == null) {
                    var capacityStr = env.getProperty("cache.caption.capacity.%s".formatted(className));
                    if (capacityStr == null) {
                        capacityStr = env.getProperty("cache.caption.capacity.default", "10000");
                    }
                    var capacity = Integer.parseInt(capacityStr);
                    var expirationInSecondsStr = env.getProperty("cache.caption.expiration.%s".formatted(className));
                    if (expirationInSecondsStr == null) {
                        expirationInSecondsStr = env.getProperty("cache.caption.expiration.default", "3600");
                    }
                    var expirationInSeconds = Integer.parseInt(expirationInSecondsStr);
                    cache = cacheManager.createKeyValueCache(String.class, cacheClass, "caption_%s".formatted(className), capacity, expirationInSeconds);
                    caches.put(className, cache);
                }
            }
        }
        return cache;
    }
}
