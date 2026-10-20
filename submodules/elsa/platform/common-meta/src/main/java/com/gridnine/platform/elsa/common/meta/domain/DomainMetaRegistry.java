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

package com.gridnine.platform.elsa.common.meta.domain;

import com.gridnine.platform.elsa.common.meta.common.EntityDescription;
import com.gridnine.platform.elsa.common.meta.common.EnumDescription;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class DomainMetaRegistry {
    private final Map<String, EnumDescription> enums = new LinkedHashMap<>();
    private final Map<String, DocumentDescription> documents = new LinkedHashMap<>();
    private final Map<String, EntityDescription> entities = new LinkedHashMap<>();
    private final Map<String, SearchableProjectionDescription> searchableProjections = new LinkedHashMap<>();
    private final Map<String, AssetDescription> assets = new LinkedHashMap<>();

    private final Map<String, VirtualAssetDescription> virtualAssets = new LinkedHashMap<>();

    @Autowired(required = false)
    public void setConfigurators(List<DomainMetaRegistryConfigurator> configurators) {
        configurators.forEach(it -> it.updateMetaRegistry(this));
    }

    public Map<String, EnumDescription> getEnums() {
        return enums;
    }

    public Map<String, DocumentDescription> getDocuments() {
        return documents;
    }

    public Map<String, EntityDescription> getEntities() {
        return entities;
    }

    public Map<String, SearchableProjectionDescription> getSearchableProjections() {
        return searchableProjections;
    }

    public Map<String, AssetDescription> getAssets() {
        return assets;
    }

    public Map<String, VirtualAssetDescription> getVirtualAssets() {
        return virtualAssets;
    }
}
