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

package com.gridnine.platform.elsa.config;

import com.gridnine.platform.elsa.core.cache.CacheManager;
import com.gridnine.platform.elsa.core.cache.CacheMetadataProvider;
import com.gridnine.platform.elsa.core.cache.ehCache.EhCacheManager;
import com.gridnine.platform.elsa.core.codec.AesCodec;
import com.gridnine.platform.elsa.core.migration.MigrationProcessor;
import com.gridnine.platform.elsa.core.remoting.standard.GetL10nBundleHandler;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcDatabaseMetadataProvider;
import com.gridnine.platform.elsa.core.storage.standard.CacheStorageAdvice;
import com.gridnine.platform.elsa.core.storage.standard.InvalidateCacheStorageInterceptor;
import com.gridnine.platform.elsa.server.core.CoreDomainConfigurator;
import com.gridnine.platform.elsa.server.core.CoreL10nMessagesRegistryConfigurator;
import com.gridnine.platform.elsa.server.core.CoreL10nMessagesRegistryFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElsaServerCoreConfiguration {

    @Bean
    public AesCodec desCodec() {
        return new AesCodec();
    }

    @Bean
    public JdbcDatabaseMetadataProvider databaseMetadataProvider() {
        return new JdbcDatabaseMetadataProvider();
    }

    @Bean
    public CacheManager standardCacheManager() {
        return new EhCacheManager();
    }

    @Bean
    public CacheStorageAdvice cacheStorageAdvice() {
        return new CacheStorageAdvice();
    }


    @Bean
    public InvalidateCacheStorageInterceptor invalidateCacheStorageInterceptor() {
        return new InvalidateCacheStorageInterceptor();
    }


    @Bean
    public CacheMetadataProvider cacheMetadataProvider() {
        return new CacheMetadataProvider();
    }

    @Bean
    public GetL10nBundleHandler getL10nBundleHandler() {
        return new GetL10nBundleHandler();
    }

    @Bean
    public CoreL10nMessagesRegistryFactory coreL10nMessagesRegistryFactory() {
        return new CoreL10nMessagesRegistryFactory();
    }

    @Bean
    public CoreL10nMessagesRegistryConfigurator coreL10nMessagesRegistryConfigurator() {
        return new CoreL10nMessagesRegistryConfigurator();
    }

    @Bean
    public CoreDomainConfigurator coreDomainConfigurator(){
        return new CoreDomainConfigurator();
    }

    @Bean
    public MigrationProcessor migrationProcessor(){
        return new MigrationProcessor();
    }

}
