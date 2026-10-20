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

import com.gridnine.platform.elsa.common.core.model.common.BaseIdentity;
import com.gridnine.platform.elsa.common.core.model.common.Xeption;

import java.util.UUID;

public abstract class BaseAsset extends BaseIdentity implements Sealable{

    public static class Fields {
        public static final String versionInfo = "versionInfo";
    }

    private boolean sealed;

    public BaseAsset() {
        super(UUID.randomUUID().toString());
    }

    private VersionInfo versionInfo;

    public VersionInfo getVersionInfo() {
        return versionInfo;
    }

    public void setVersionInfo(VersionInfo versionInfo) {
        this.versionInfo = versionInfo;
    }

    @Override
    public Object getValue(String propertyName) {
        if (Fields.versionInfo.equals(propertyName)) {
            return versionInfo;
        }
        return super.getValue(propertyName);
    }

    @Override
    public void setValue(String propertyName, Object value) {
        if(sealed){
            throw Xeption.forDeveloper("object is sealed");
        }
        if (Fields.versionInfo.equals(propertyName)) {
            versionInfo = (VersionInfo) value;
            return;
        }
        super.setValue(propertyName, value);
    }

    @Override
    public boolean isSealed() {
        return sealed;
    }

    @Override
    public void seal() {
        sealed = true;
        if(versionInfo != null){
            versionInfo.seal();
        }
    }

    @Override
    public void setId(String id) {
        if(sealed){
            throw Xeption.forDeveloper("object is sealed");
        }
        super.setId(id);
    }

}
