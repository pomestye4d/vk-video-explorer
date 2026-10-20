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

import com.gridnine.platform.elsa.common.core.model.common.BaseIdentity;
import com.gridnine.platform.elsa.common.core.model.domain.BaseAsset;
import com.gridnine.platform.elsa.common.core.model.domain.VersionInfo;
import com.gridnine.platform.elsa.common.meta.custom.CustomMetaRegistry;
import com.gridnine.platform.elsa.common.meta.domain.AssetDescription;
import com.gridnine.platform.elsa.common.meta.domain.DomainMetaRegistry;

class AssetMetadataProvider extends BaseSearchableMetadataProvider {
    AssetMetadataProvider(AssetDescription description, DomainMetaRegistry dr, CustomMetaRegistry cr) {
        super(description, dr,cr);
        addProperty(new SerializablePropertyDescription(BaseIdentity.Fields.idName, SerializablePropertyType.STRING, null, false));
        addProperty(new SerializablePropertyDescription(BaseAsset.Fields.versionInfo, SerializablePropertyType.ENTITY, VersionInfo.class.getName(), false));
    }
}
