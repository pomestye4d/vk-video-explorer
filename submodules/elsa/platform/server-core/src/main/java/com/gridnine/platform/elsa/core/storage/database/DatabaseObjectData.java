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

import com.gridnine.platform.elsa.common.core.model.domain.VersionInfo;

public class DatabaseObjectData extends VersionInfo {

    private DatabaseBinaryData data;

    public static class Fields {
        public static final String data = "data";
    }

    public DatabaseBinaryData getData() {
        return data;
    }

    public void setData(DatabaseBinaryData data) {
        this.data = data;
    }

    public void setRevision(int revision) {
        setValue(VersionInfo.Fields.revision, revision);
    }

    @Override
    public Object getValue(String propertyName) {
        if (Fields.data.equals(propertyName)) {
            return data;
        }
        return super.getValue(propertyName);
    }

    @Override
    public void setValue(String propertyName, Object value) {
        if (Fields.data.equals(propertyName)) {
            data = (DatabaseBinaryData) value;
            return;
        }
        super.setValue(propertyName, value);
    }
}
