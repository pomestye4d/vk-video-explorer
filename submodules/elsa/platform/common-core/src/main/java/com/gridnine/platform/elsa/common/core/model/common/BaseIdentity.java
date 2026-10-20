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

import com.gridnine.platform.elsa.common.core.search.ArgumentType;
import com.gridnine.platform.elsa.common.core.search.EqualitySupport;
import com.gridnine.platform.elsa.common.core.search.FieldNameSupport;
import com.gridnine.platform.elsa.common.core.utils.LocaleUtils;

import java.util.UUID;

public abstract class BaseIdentity extends BaseIntrospectableObject {

    public static class Fields {
        public final static String idName = "id";
        public final static _idField id = new _idField();
    }

    public BaseIdentity() {
    }

    public BaseIdentity(String id) {
        this.id = id;
    }

    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public Object getValue(String propertyName) {
        if (Fields.idName.equals(propertyName)) {
            return id;
        }
        return super.getValue(propertyName);
    }

    @Override
    public void setValue(String propertyName, Object value) {
        if (Fields.idName.equals(propertyName)) {
            id = (String) value;
            return;
        }
        super.setValue(propertyName, value);
    }

    public static class _idField extends FieldNameSupport implements EqualitySupport, ArgumentType<String> {
        _idField(){
            super("id");
        }

        @Override
        public String toString(){
            return LocaleUtils.getLocalizedName(new String[]{"en","ru"}, new String[]{"Document id", "Идентификатор документа"}, this.name);
        }
    }
}
