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

package com.gridnine.platform.elsa.common.core.serialization.meta;

import com.gridnine.platform.elsa.common.core.model.common.Xeption;
import com.gridnine.platform.elsa.common.meta.custom.CustomMetaRegistry;
import com.gridnine.platform.elsa.common.meta.domain.DomainMetaRegistry;
import com.gridnine.platform.elsa.common.meta.remoting.RemotingMetaRegistry;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ObjectMetadataProvidersFactory {

    private final Map<String, BaseObjectMetadataProvider<?>> providersCache = new ConcurrentHashMap<>();

    @Autowired
    private DomainMetaRegistry domainMetaRegistry;

    @Autowired
    private CustomMetaRegistry customMetaRegistry;

    @Autowired
    private RemotingMetaRegistry remotingMetaRegistry;

    public BaseObjectMetadataProvider<?> getProvider(String className) {
        BaseObjectMetadataProvider<?> result = providersCache.get(className);
        if (result != null) {
            return result;
        }
        return providersCache.computeIfAbsent(className, this::createProvider);
    }

    private BaseObjectMetadataProvider<?> createProvider(String className) {
        var docDescription = domainMetaRegistry.getDocuments().get(className);
        if (docDescription != null) {
            return new DomainDocumentMetadataProvider(docDescription, domainMetaRegistry, customMetaRegistry, remotingMetaRegistry);
        }
        var domainEntityDescription = domainMetaRegistry.getEntities().get(className);
        if (domainEntityDescription != null) {
            return new EntityMetadataProvider(domainEntityDescription, domainMetaRegistry, customMetaRegistry, remotingMetaRegistry);
        }
        var assetDescription = domainMetaRegistry.getAssets().get(className);
        if (assetDescription != null) {
            return new AssetMetadataProvider(assetDescription, domainMetaRegistry, customMetaRegistry);
        }
        var projectionDescription = domainMetaRegistry.getSearchableProjections().get(className);
        if (projectionDescription != null) {
            return new SearchableProjectionMetadataProvider(projectionDescription, domainMetaRegistry, customMetaRegistry);
        }
        var customEntityDescription = customMetaRegistry.getEntities().get(className);
        if (customEntityDescription != null) {
            return new EntityMetadataProvider(customEntityDescription, domainMetaRegistry, customMetaRegistry, remotingMetaRegistry);
        }
        var remotingEntityDescription = remotingMetaRegistry.getEntities().get(className);
        if (remotingEntityDescription != null) {
            return new EntityMetadataProvider(remotingEntityDescription, domainMetaRegistry, customMetaRegistry, remotingMetaRegistry);
        }
        throw Xeption.forDeveloper("no description found for entity %s".formatted(className));
    }

}
