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

package com.gridnine.platform.elsa.gradle.meta.domain;


import com.gridnine.platform.elsa.gradle.meta.common.BaseModelElementDescription;

public class DatabasePropertyDescription extends BaseModelElementDescription {
    private DatabasePropertyType type;

    private boolean cacheFind;

    private boolean cacheGetAll;

    private String className;

    private boolean useInTextSearch;

    private boolean nonNullable;

    public DatabasePropertyDescription() {
    }

    public DatabasePropertyDescription(String id) {
        super(id);
    }

    public DatabasePropertyType getType() {
        return type;
    }

    public void setType(DatabasePropertyType type) {
        this.type = type;
    }

    public boolean isCacheFind() {
        return cacheFind;
    }

    public void setCacheFind(boolean cacheFind) {
        this.cacheFind = cacheFind;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public boolean isUseInTextSearch() {
        return useInTextSearch;
    }

    public void setUseInTextSearch(boolean useInTextSearch) {
        this.useInTextSearch = useInTextSearch;
    }

    public boolean isCacheGetAll() {
        return cacheGetAll;
    }

    public void setCacheGetAll(boolean cacheGetAll) {
        this.cacheGetAll = cacheGetAll;
    }

    public boolean isNonNullable() {
        return nonNullable;
    }

    public void setNonNullable(boolean nonNullable) {
        this.nonNullable = nonNullable;
    }
}
