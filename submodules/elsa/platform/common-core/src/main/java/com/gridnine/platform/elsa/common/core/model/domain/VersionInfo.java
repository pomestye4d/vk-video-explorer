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
import com.gridnine.platform.elsa.common.core.model.common.Xeption;

import java.time.Instant;

public class VersionInfo extends BaseIntrospectableObject implements Sealable {
    public static class Fields {
        public static final String revision = "revision";
        public static final String modifiedBy = "modifiedBy";
        public static final String modified = "modified";
        public static final String comment = "comment";
        public static final String versionNumber = "versionNumber";
    }

    private boolean sealed;

    private int revision;

    private String modifiedBy;

    private Instant modified;

    private String comment;

    private int versionNumber;

    public int getRevision() {
        return revision;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public Instant getModified() {
        return modified;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public Object getValue(String propertyName) {
        if (Fields.comment.equals(propertyName)) {
            return comment;
        }
        if (Fields.modified.equals(propertyName)) {
            return modified;
        }
        if (Fields.modifiedBy.equals(propertyName)) {
            return modifiedBy;
        }
        if (Fields.revision.equals(propertyName)) {
            return revision;
        }
        if (Fields.versionNumber.equals(propertyName)) {
            return versionNumber;
        }
        return super.getValue(propertyName);
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public void setModified(Instant modified) {
        this.modified = modified;
    }

    @Override
    public void setValue(String propertyName, Object value) {
        if(sealed){
            throw Xeption.forDeveloper("object is sealed");
        }
        if (Fields.comment.equals(propertyName)) {
            comment = (String) value;
            return;
        }
        if (Fields.modified.equals(propertyName)) {
            modified = (Instant) value;
            return;
        }
        if (Fields.modifiedBy.equals(propertyName)) {
            modifiedBy = (String) value;
            return;
        }
        if (Fields.revision.equals(propertyName)) {
            revision = (int) value;
            return;
        }
        if (Fields.versionNumber.equals(propertyName)) {
            versionNumber = (int) value;
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
    }
}
