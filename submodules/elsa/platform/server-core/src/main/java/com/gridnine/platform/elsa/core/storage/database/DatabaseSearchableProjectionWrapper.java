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

package com.gridnine.platform.elsa.core.storage.database;

import com.gridnine.platform.elsa.common.core.model.common.BaseIntrospectableObject;
import com.gridnine.platform.elsa.common.core.model.domain.BaseDocument;
import com.gridnine.platform.elsa.common.core.model.domain.BaseSearchableProjection;
import com.gridnine.platform.elsa.common.core.model.domain.EntityReference;
import com.gridnine.platform.elsa.common.meta.domain.DomainMetaRegistry;
import com.gridnine.platform.elsa.common.meta.domain.SearchableProjectionDescription;

import java.util.Collection;

@SuppressWarnings("unchecked")
public class DatabaseSearchableProjectionWrapper<D extends BaseDocument, I extends BaseSearchableProjection<D>> extends BaseIntrospectableObject {

    private I projection;
    private String aggregatedData;
    private final SearchableProjectionDescription descr;

    public static class Fields {
        public static final String aggregatedData = "aggregatedData";
    }

    public DatabaseSearchableProjectionWrapper(DomainMetaRegistry reg, Class<I> projectionCls) {
        this.descr = reg.getSearchableProjections().get(projectionCls.getName());
    }

    public DatabaseSearchableProjectionWrapper(I projection, DomainMetaRegistry reg, String aggregatedData) {
        this.projection = projection;
        this.aggregatedData = aggregatedData;
        this.descr = reg.getSearchableProjections().get(projection.getClass().getName());
    }

    public I getProjection() {
        return projection;
    }

    public void setProjection(I projection) {
        this.projection = projection;
    }

    public String getAggregatedData() {
        return aggregatedData;
    }

    public void setAggregatedData(String aggregatedData) {
        this.aggregatedData = aggregatedData;
    }

    @Override
    public Object getValue(String propertyName) {
        if (Fields.aggregatedData.equals(propertyName)) {
            return aggregatedData;
        }
        if (BaseSearchableProjection.Fields.navigationKey.equals(propertyName)) {
            return projection.getNavigationKey();
        }
        if (BaseSearchableProjection.Fields.document.equals(propertyName)) {
            return projection.getDocument();
        }
        if (descr.getProperties().containsKey(propertyName)) {
            return projection.getValue(propertyName);
        }
        return projection.getCollection(propertyName);
    }

    @Override
    public void setValue(String propertyName, Object value) {
        if (Fields.aggregatedData.equals(propertyName)) {
            aggregatedData = (String) value;
            return;
        }
        if (BaseSearchableProjection.Fields.navigationKey.equals(propertyName)) {
            projection.setNavigationKey((Integer) value);
            return;
        }
        if (BaseSearchableProjection.Fields.document.equals(propertyName)) {
            projection.setDocument((EntityReference<D>) value);
            return;
        }

        if (descr.getProperties().containsKey(propertyName)) {
            projection.setValue(propertyName, value);
            return;
        }
        var coll = (Collection<Object>) projection.getCollection(propertyName);
        coll.clear();
        coll.addAll((Collection<Object>) value);
    }
}
