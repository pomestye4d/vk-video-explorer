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

import com.gridnine.platform.elsa.common.core.model.common.BaseIntrospectableObject;
import com.gridnine.platform.elsa.common.core.model.common.Xeption;
import com.gridnine.platform.elsa.common.core.model.domain.BaseAsset;
import com.gridnine.platform.elsa.common.core.model.domain.EntityReference;
import com.gridnine.platform.elsa.common.meta.common.StandardValueType;
import com.gridnine.platform.elsa.common.meta.custom.CustomMetaRegistry;
import com.gridnine.platform.elsa.common.meta.domain.BaseSearchableDescription;
import com.gridnine.platform.elsa.common.meta.domain.DatabaseCollectionType;
import com.gridnine.platform.elsa.common.meta.domain.DatabasePropertyType;
import com.gridnine.platform.elsa.common.meta.domain.DomainMetaRegistry;

import java.util.Collection;
import java.util.Map;

abstract class BaseSearchableMetadataProvider extends BaseObjectMetadataProvider<BaseIntrospectableObject> {
    BaseSearchableMetadataProvider(BaseSearchableDescription description, DomainMetaRegistry dr, CustomMetaRegistry cr) {
        description.getProperties().values().forEach(prop ->
                addProperty(new SerializablePropertyDescription(prop.getId(), toSerializableType(prop.getType()),
                        toClassName(prop.getType(), prop.getClassName()), false)));
        description.getCollections().values().forEach(coll ->
                addCollection(new SerializableCollectionDescription(coll.getId(), toSerializableType(coll.getElementType()),
                        toClassName(coll.getElementType(), coll.getElementClassName()), false)));
        var extendsId = description.getExtendsId();
        while (extendsId != null) {
            var parentDescr = dr.getEntities().get(extendsId);
            if (parentDescr == null) {
                parentDescr = dr.getDocuments().get(extendsId);
            }
            if (parentDescr == null) {
                parentDescr = cr.getEntities().get(extendsId);
            }
            if (parentDescr != null) {
                parentDescr.getProperties().values().stream().filter(it -> !BaseAsset.Fields.versionInfo.equals(it.getId())).forEach(prop ->
                        addProperty(new SerializablePropertyDescription(prop.getId(), toSerializableType(prop.getType()),
                                toClassName(prop.getType(), prop.getClassName()), false)));
                parentDescr.getCollections().values().forEach(coll ->
                        addCollection(new SerializableCollectionDescription(coll.getId(), toSerializableType(coll.getElementType()),
                                toClassName(coll.getElementType(), coll.getElementClassName()), false)));
                extendsId = parentDescr.getExtendsId();
                continue;
            }
            var bad = dr.getAssets().get(extendsId);
            if(bad == null) {
                throw Xeption.forDeveloper("no entity found for id %s".formatted(extendsId));
            }
            bad.getProperties().values().forEach(prop ->
                    addProperty(new SerializablePropertyDescription(prop.getId(), toSerializableType(prop.getType()),
                            toClassName(prop.getType(), prop.getClassName()), false)));
            bad.getCollections().values().forEach(coll ->
                    addCollection(new SerializableCollectionDescription(coll.getId(), toSerializableType(coll.getElementType()),
                            toClassName(coll.getElementType(), coll.getElementClassName()), false)));
            extendsId = bad.getExtendsId();
        }
    }

    private SerializablePropertyType toSerializableType(DatabasePropertyType type) {
        return switch (type) {
            case UUID -> SerializablePropertyType.STRING;
            case LONG -> SerializablePropertyType.LONG;
            case LOCAL_DATE_TIME -> SerializablePropertyType.LOCAL_DATE_TIME;
            case INSTANT -> SerializablePropertyType.INSTANT;
            case LOCAL_DATE -> SerializablePropertyType.LOCAL_DATE;
            case INT -> SerializablePropertyType.INT;
            case ENUM -> SerializablePropertyType.ENUM;
            case ENTITY_REFERENCE -> SerializablePropertyType.ENTITY_REFERENCE;
            case BOOLEAN -> SerializablePropertyType.BOOLEAN;
            case BIG_DECIMAL -> SerializablePropertyType.BIG_DECIMAL;
            case STRING, TEXT -> SerializablePropertyType.STRING;
            case ENTITY -> SerializablePropertyType.ENTITY;
            case TEXT_SEARCH -> SerializablePropertyType.TEXT_SEARCH;
        };
    }

    private SerializablePropertyType toSerializableType(StandardValueType type) {
        return switch (type) {
            case UUID -> SerializablePropertyType.STRING;
            case LONG -> SerializablePropertyType.LONG;
            case LOCAL_DATE_TIME -> SerializablePropertyType.LOCAL_DATE_TIME;
            case INSTANT -> SerializablePropertyType.INSTANT;
            case LOCAL_DATE -> SerializablePropertyType.LOCAL_DATE;
            case INT -> SerializablePropertyType.INT;
            case ENUM -> SerializablePropertyType.ENUM;
            case ENTITY_REFERENCE -> SerializablePropertyType.ENTITY_REFERENCE;
            case BOOLEAN -> SerializablePropertyType.BOOLEAN;
            case BIG_DECIMAL -> SerializablePropertyType.BIG_DECIMAL;
            case STRING -> SerializablePropertyType.STRING;
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    private SerializablePropertyType toSerializableType(DatabaseCollectionType type) {
        return switch (type) {
            case ENTITY_REFERENCE -> SerializablePropertyType.ENTITY_REFERENCE;
            case ENUM -> SerializablePropertyType.ENUM;
            case STRING -> SerializablePropertyType.STRING;
            case UUID -> SerializablePropertyType.STRING;
            case ENTITY -> SerializablePropertyType.ENTITY;
        };
    }

    private String toClassName(DatabasePropertyType type, String className) {
        return className;
    }

    private String toClassName(StandardValueType type, String className) {
        return className;
    }
    private String toClassName(DatabaseCollectionType type, String className) {
        return className;
    }

    @Override
    public Object getPropertyValue(BaseIntrospectableObject obj, String id) {
        return obj.getValue(id);
    }

    @Override
    public Collection<?> getCollection(BaseIntrospectableObject obj, String id) {
        return obj.getCollection(id);
    }

    @Override
    public Map<?, ?> getMap(BaseIntrospectableObject obj, String id) {
        return obj.getMap(id);
    }

    @Override
    public void setPropertyValue(BaseIntrospectableObject obj, String id, Object value) {
        obj.setValue(id, value);
    }
}
