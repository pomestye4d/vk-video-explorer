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

package com.gridnine.platform.elsa.gradle.meta.common;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EntityDescription extends BaseModelElementDescription {

    private boolean isAbstract;

    private String extendsId;

    private final Map<String, StandardPropertyDescription> properties = new LinkedHashMap<>();

    private final Map<String, StandardCollectionDescription> collections = new LinkedHashMap<>();

    private final Map<String, StandardMapDescription> maps = new LinkedHashMap<>();

    private final List<String> codeInjections = new ArrayList<>();

    public EntityDescription() {
    }

    public EntityDescription(String id) {
        super(id);
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public void setAbstract(boolean anAbstract) {
        isAbstract = anAbstract;
    }

    public String getExtendsId() {
        return extendsId;
    }

    public void setExtendsId(String extendsId) {
        this.extendsId = extendsId;
    }

    public Map<String, StandardPropertyDescription> getProperties() {
        return properties;
    }

    public Map<String, StandardCollectionDescription> getCollections() {
        return collections;
    }

    public Map<String, StandardMapDescription> getMaps() {
        return maps;
    }

    public List<String> getCodeInjections() {
        return codeInjections;
    }

}
