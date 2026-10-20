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

package com.gridnine.platform.elsa.core.cache.ehCache;

import com.gridnine.platform.elsa.core.cache.CachedValue;
import com.gridnine.platform.elsa.core.cache.KeyValueCache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.spi.serialization.Serializer;
import org.ehcache.spi.serialization.SerializerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.nio.ByteBuffer;
import java.time.Duration;

@Component
public class EhCacheManager implements com.gridnine.platform.elsa.core.cache.CacheManager {
    private final CacheManager cacheManager;

    public EhCacheManager(){
        this.cacheManager =  CacheManagerBuilder.newCacheManagerBuilder().build(true);
    }

    @Autowired
    private Environment env;

    @PreDestroy
    public void close() {
        cacheManager.close();
    }

    @Override
    public <K, D> KeyValueCache<K, D> createKeyValueCache(Class<K> keyClass, Class<D> cls, String name, int capacity, int expirationInSeconds) {
        var delegate = cacheManager.createCache(name,
                CacheConfigurationBuilder.newCacheConfigurationBuilder(keyClass, CachedValue.class, ResourcePoolsBuilder.heap(capacity))
                        .withValueSerializer(new Serializer<>() {
                            @Override
                            public ByteBuffer serialize(CachedValue object) throws SerializerException {
                                return null;
                            }

                            @Override
                            public CachedValue<?> read(ByteBuffer binary) throws SerializerException {
                                return null;
                            }

                            @Override
                            public boolean equals(CachedValue object, ByteBuffer binary) throws SerializerException {
                                return false;
                            }
                        })
                        .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(expirationInSeconds))));
        return new KeyValueCache<>() {
            @SuppressWarnings("unchecked")
            @Override
            public CachedValue<D> get(K key) {
                return delegate.get(key);
            }

            @Override
            public void put(K key, CachedValue<D> value) {
                delegate.put(key, value);
            }

            @Override
            public void replace(K key, CachedValue<D> oldValue, CachedValue<D> newValue) {
                if (oldValue == null) {
                    delegate.putIfAbsent(key, newValue);
                } else {
                    delegate.replace(key, oldValue, newValue);
                }
            }

        };
    }
}
