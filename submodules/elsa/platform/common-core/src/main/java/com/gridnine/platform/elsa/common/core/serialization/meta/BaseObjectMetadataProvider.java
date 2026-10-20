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

import java.util.*;

public abstract class BaseObjectMetadataProvider<T> {

    private final Map<String, SerializablePropertyDescription> propertiesMap = new LinkedHashMap<>();

    private final Map<String, SerializableCollectionDescription> collectionsMap = new LinkedHashMap<>();

    private final Map<String, SerializableMapDescription> mapsMap = new LinkedHashMap<>();

    private final List<SerializablePropertyDescription> allProperties = new ArrayList<>();

    private final List<SerializableCollectionDescription> allCollections = new ArrayList<>();

    private final List<SerializableMapDescription> allMaps = new ArrayList<>();

    private boolean isAbstract;

    public void setAbstract(boolean anAbstract) {
        isAbstract = anAbstract;
    }

    public SerializablePropertyDescription getProperty(String id) {
        return propertiesMap.get(id);
    }

    public SerializableCollectionDescription getCollection(String id) {
        return collectionsMap.get(id);
    }

    public SerializableMapDescription getMap(String id) {
        return mapsMap.get(id);
    }

    public List<SerializablePropertyDescription> getAllProperties() {
        return allProperties;
    }

    public List<SerializableCollectionDescription> getAllCollections() {
        return allCollections;
    }

    public List<SerializableMapDescription> getAllMaps() {
        return allMaps;
    }

    void addProperty(SerializablePropertyDescription prop) {
        propertiesMap.put(prop.id(), prop);
        allProperties.add(prop);
    }

    void addCollection(SerializableCollectionDescription prop) {
        collectionsMap.put(prop.id(), prop);
        allCollections.add(prop);
    }

    void addMap(SerializableMapDescription prop) {
        mapsMap.put(prop.id(), prop);
        allMaps.add(prop);
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public abstract Object getPropertyValue(T obj, String id);

    public abstract void setPropertyValue(T obj, String id, Object value);

    public abstract Collection<?> getCollection(T obj, String id);

    public abstract Map<?, ?> getMap(T obj, String id);

}
