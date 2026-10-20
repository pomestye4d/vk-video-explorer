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

import com.gridnine.platform.elsa.common.core.l10n.Localizer;
import com.gridnine.platform.elsa.common.core.l10n.SupportedLocalesProvider;
import com.gridnine.platform.elsa.common.core.lock.LockManager;
import com.gridnine.platform.elsa.common.core.lock.standard.StandardLockManager;
import com.gridnine.platform.elsa.common.core.model.domain.CaptionProvider;
import com.gridnine.platform.elsa.common.core.model.domain.EntityReference;
import com.gridnine.platform.elsa.common.core.reflection.ReflectionFactory;
import com.gridnine.platform.elsa.common.core.serialization.Cloner;
import com.gridnine.platform.elsa.common.core.serialization.JsonMarshaller;
import com.gridnine.platform.elsa.common.core.serialization.JsonUnmarshaller;
import com.gridnine.platform.elsa.common.core.serialization.ParameterMapConverter;
import com.gridnine.platform.elsa.common.core.serialization.meta.ObjectMetadataProvidersFactory;
import com.gridnine.platform.elsa.common.core.utils.ExceptionUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElsaCommonCoreConfiguration {

    @Bean
    public CaptionProvider standardCaptionProvider() {
        return EntityReference::getCaption;
    }

    @Bean
    public Localizer localizer() {
        return new Localizer();
    }

    @Bean
    public ExceptionUtils exceptionUtils() {
        return new ExceptionUtils();
    }

    @Bean
    public SupportedLocalesProvider supportedLocalesProvider() {
        return new SupportedLocalesProvider();
    }

    @Bean
    public LockManager standardLockManager() {
        return new StandardLockManager();
    }

    @Bean
    public ReflectionFactory reflectionFactory() {
        return new ReflectionFactory();
    }

    @Bean
    public Cloner cloner() {
        return new Cloner();
    }

    @Bean
    public JsonMarshaller jsonMarshaller() {
        return new JsonMarshaller();
    }

    @Bean
    public JsonUnmarshaller jsonUnmarshaller() {
        return new JsonUnmarshaller();
    }

    @Bean
    public ParameterMapConverter parameterMapConverter() {
        return new ParameterMapConverter();
    }

    @Bean
    public ObjectMetadataProvidersFactory objectMetadataProvidersFactory() {
        return new ObjectMetadataProvidersFactory();
    }

    @Bean
    public ElsaCommonCoreCustomMetaRegistryConfigurator elsaCommonCoreCustomMetaRegistryConfigurator() {
        return new ElsaCommonCoreCustomMetaRegistryConfigurator();
    }

    @Bean
    public ElsaCommonCoreRemotingMetaRegistryConfigurator elsaCommonCoreRemotingMetaRegistryConfigurator() {
        return new ElsaCommonCoreRemotingMetaRegistryConfigurator();
    }

}
