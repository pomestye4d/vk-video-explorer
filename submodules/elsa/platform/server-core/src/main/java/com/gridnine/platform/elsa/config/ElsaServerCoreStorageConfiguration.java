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

import com.gridnine.platform.elsa.common.core.model.common.ClassMapper;
import com.gridnine.platform.elsa.common.core.model.common.EnumMapper;
import com.gridnine.platform.elsa.common.core.model.domain.CaptionProvider;
import com.gridnine.platform.elsa.core.storage.Storage;
import com.gridnine.platform.elsa.core.storage.StorageFactory;
import com.gridnine.platform.elsa.core.storage.standard.JdbcCaptionProviderImpl;
import com.gridnine.platform.elsa.core.storage.transaction.ElsaTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;
import java.util.List;

@Configuration
public class ElsaServerCoreStorageConfiguration {

    private StorageFactory storageFactory;

    private List<StorageFactory> storageFactories;

    private AbstractBeanFactory beanFactory;

    public ElsaServerCoreStorageConfiguration(@Autowired List<StorageFactory> storageFactories, @Autowired AbstractBeanFactory beanFactory) throws Exception {
        this.storageFactories = storageFactories;
        this.beanFactory = beanFactory;
        String storageType = beanFactory.resolveEmbeddedValue("${elsa.storage.type:STANDARD}");
        storageFactory = storageFactories.stream().filter(it -> it.getId().equals(storageType)).findFirst().orElseThrow();
        var customParameters = new HashMap<String,Object>();
        storageFactory.init("elsa.storage", customParameters);
    }
    @Bean
    public Storage storage(){
        return storageFactory.getStorage();
    }


    @Bean
    public ClassMapper classMapper() {
        return storageFactory.getClassMapper();
    }

    @Bean
    public EnumMapper enumMapper() {
        return storageFactory.getEnumMapper();
    }

    @Bean
    public ElsaTransactionManager elsaTransactionManager() {
        return storageFactory.getTransactionManager();
    }

    @Bean
    @Primary
    public CaptionProvider captionProvider() {
        return storageFactory.getCaptionProvider();
    }

}
