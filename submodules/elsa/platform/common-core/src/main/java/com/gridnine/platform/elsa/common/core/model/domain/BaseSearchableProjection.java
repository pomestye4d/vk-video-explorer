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

package com.gridnine.platform.elsa.common.core.model.domain;

import com.gridnine.platform.elsa.common.core.model.common.BaseIntrospectableObject;

public abstract class BaseSearchableProjection<D extends BaseDocument> extends BaseIntrospectableObject {
    private Integer navigationKey;
    private EntityReference<D> document;

    public static class Fields {
        public static final String navigationKey = "navigationKey";
        public static final String document = "document";
    }

    public Integer getNavigationKey() {
        return navigationKey;
    }

    public void setNavigationKey(Integer navigationKey) {
        this.navigationKey = navigationKey;
    }

    public EntityReference<D> getDocument() {
        return document;
    }

    public void setDocument(EntityReference<D> document) {
        this.document = document;
    }

    @Override
    public Object getValue(String propertyName) {
        if (Fields.navigationKey.equals(propertyName)) {
            return navigationKey;
        }
        if (Fields.document.equals(propertyName)) {
            return document;
        }
        return super.getValue(propertyName);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setValue(String propertyName, Object value) {
        if (Fields.navigationKey.equals(propertyName)) {
            navigationKey = (Integer) value;
            return;
        }
        if (Fields.document.equals(propertyName)) {
            document = (EntityReference<D>) value;
            return;
        }
        super.setValue(propertyName, value);
    }
}
