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

package com.gridnine.platform.elsa.common.core.serialization;

import com.gridnine.platform.elsa.common.core.model.common.BaseIntrospectableObject;
import com.gridnine.platform.elsa.common.core.model.domain.EntityReference;
import com.gridnine.platform.elsa.common.core.model.domain.TextSearchValue;
import com.gridnine.platform.elsa.common.core.reflection.ReflectionFactory;
import com.gridnine.platform.elsa.common.core.serialization.meta.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public class Cloner {

    @Autowired
    private ObjectMetadataProvidersFactory metadataProvidersFactory;

    @Autowired
    private ReflectionFactory reflectionFactory;

    @SuppressWarnings("unchecked")
    public <T extends BaseIntrospectableObject> T clone(T source) {
        var result = (T) reflectionFactory.newInstance(source.getClass().getName());
        copy(source, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseIntrospectableObject> void copy(T source, T result) {
        var provider = metadataProvidersFactory.getProvider(source.getClass().getName());
        for (SerializablePropertyDescription prop : provider.getAllProperties()) {
            result.setValue(prop.id(), getCopy(source.getValue(prop.id()), prop.type()));
        }
        for (SerializableCollectionDescription coll : provider.getAllCollections()) {
            Collection<Object> rc = (Collection<Object>) result.getCollection(coll.id());
            rc.clear();
            for (Object value : source.getCollection(coll.id())) {
                rc.add(getCopy(value, coll.elementType()));
            }
        }
        for (SerializableMapDescription map : provider.getAllMaps()) {
            Map<Object, Object> rm = (Map<Object, Object>) result.getMap(map.id());
            rm.clear();
            for (Map.Entry<?, ?> entry : source.getMap(map.id()).entrySet()) {
                rm.put(getCopy(entry, map.keyType()), getCopy(entry.getValue(), map.valueType()));
            }
        }
    }

    private Object getCopy(Object value, SerializablePropertyType type) {
        if (value == null) {
            return null;
        }
        return switch (type) {
            case STRING, ENUM, LONG, INT, BIG_DECIMAL, LOCAL_DATE_TIME, LOCAL_DATE,
                 BOOLEAN, CLASS, INSTANT,UUID -> value;
            case BYTE_ARRAY -> {
                var ba = (byte[]) value;
                yield Arrays.copyOf(ba, ba.length);
            }
            case ENTITY_REFERENCE -> {
                var er = (EntityReference<?>) value;
                yield new EntityReference<>(er.getId(), er.getType(), er.getCaption());
            }
            case ENTITY -> {
                var qualifiedName = value.getClass().getName();
                var ett = (BaseIntrospectableObject) reflectionFactory.newInstance(qualifiedName);
                metadataProvidersFactory.getProvider(qualifiedName);
                copy((BaseIntrospectableObject) value, ett);
                yield ett;
            }
            case TEXT_SEARCH -> {
                var tsv = (TextSearchValue) value;
                yield new TextSearchValue(tsv.getValue(), Arrays.copyOf(tsv.getMatchedFragmentPositions(), tsv.getMatchedFragmentPositions().length));
            }
        };
    }
}
