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

package com.gridnine.platform.elsa.common.core.model.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class L10nMessage extends BaseIntrospectableObject {
    private static final String keyPropertyName = "key";
    private static final String bundlePropertyName = "bundle";
    private static final String parametersPropertyName = "parameters";

    public L10nMessage() {
    }

    public L10nMessage(String bundle, String key, Object... parameters) {
        this.bundle = bundle;
        this.key = key;
        Collections.addAll(this.parameters, parameters);
    }

    private String key;

    private String bundle;

    private final List<Object> parameters = new ArrayList<>();

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getBundle() {
        return bundle;
    }

    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

    public List<Object> getParameters() {
        return parameters;
    }

    @Override
    public Object getValue(String propertyName) {
        return switch (propertyName) {
            case keyPropertyName -> key;
            case bundlePropertyName -> bundle;
            default -> super.getValue(propertyName);
        };
    }

    @Override
    public void setValue(String propertyName, Object value) {
        switch (propertyName) {
            case keyPropertyName -> key = (String) value;
            case bundlePropertyName -> bundle = (String) value;
            default -> super.setValue(propertyName, value);
        }
    }

    @Override
    public Collection<?> getCollection(String collectionName) {
        if (parametersPropertyName.equals(collectionName)) {
            return getParameters();
        }
        return super.getCollection(collectionName);
    }


}
