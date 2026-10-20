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
import com.gridnine.platform.elsa.common.meta.common.EntityDescription;
import com.gridnine.platform.elsa.common.meta.common.StandardPropertyDescription;
import com.gridnine.platform.elsa.common.meta.common.StandardValueType;
import com.gridnine.platform.elsa.common.meta.custom.CustomMetaRegistry;
import com.gridnine.platform.elsa.common.meta.domain.DomainMetaRegistry;
import com.gridnine.platform.elsa.common.meta.remoting.RemotingMetaRegistry;

import java.util.Collection;
import java.util.Map;

class EntityMetadataProvider extends BaseObjectMetadataProvider<BaseIntrospectableObject> {

    EntityMetadataProvider(EntityDescription entityDescription,
                           DomainMetaRegistry dr, CustomMetaRegistry cr, RemotingMetaRegistry rr,
                           SerializablePropertyDescription... additionalProperties) {
        for (SerializablePropertyDescription ap : additionalProperties) {
            addProperty(ap);
        }
        var extendsId = entityDescription.getExtendsId();
        while (extendsId != null) {
            var parentDescr = dr.getEntities().get(extendsId);
            if (parentDescr == null) {
                parentDescr = dr.getDocuments().get(extendsId);
            }
            if (parentDescr == null) {
                parentDescr = cr.getEntities().get(extendsId);
            }
            if (parentDescr == null) {
                parentDescr = rr.getEntities().get(extendsId);
            }
            if (parentDescr == null) {
                throw Xeption.forDeveloper("no entity found for id %s".formatted(extendsId));
            }
            fillProperties(parentDescr, dr, cr, rr);
            fillCollections(parentDescr, dr, cr, rr);
            fillMaps(parentDescr, dr, cr, rr);
            extendsId = parentDescr.getExtendsId();
        }
        fillProperties(entityDescription, dr, cr, rr);
        fillCollections(entityDescription, dr, cr, rr);
        fillMaps(entityDescription, dr, cr, rr);
        setAbstract(entityDescription.isAbstract());
    }

    private boolean isAbstractClass(String className, DomainMetaRegistry dr, CustomMetaRegistry cr, RemotingMetaRegistry rr) {
        if (className == null) {
            return false;
        }
        if("Object".equals(className)) {
            return true;
        }
        var domainDocument = dr.getDocuments().get(className);
        if (domainDocument != null) {
            return domainDocument.isAbstract();
        }
        var entity = dr.getEntities().get(className);
        if (entity != null) {
            return entity.isAbstract();
        }

        var customEntity = cr.getEntities().get(className);
        if (customEntity != null) {
            return customEntity.isAbstract();
        }
        var remotingEntity = rr.getEntities().get(className);
        if (remotingEntity != null) {
            return remotingEntity.isAbstract();
        }
        return false;
    }

    private SerializablePropertyType toSerializableType(StandardValueType type) {
        return switch (type) {
            case LONG -> SerializablePropertyType.LONG;
            case STRING -> SerializablePropertyType.STRING;
            case UUID ->  SerializablePropertyType.UUID;
            case LOCAL_DATE_TIME -> SerializablePropertyType.LOCAL_DATE_TIME;
            case INSTANT -> SerializablePropertyType.INSTANT;
            case LOCAL_DATE -> SerializablePropertyType.LOCAL_DATE;
            case INT -> SerializablePropertyType.INT;
            case ENUM -> SerializablePropertyType.ENUM;
            case ENTITY_REFERENCE -> SerializablePropertyType.ENTITY_REFERENCE;
            case BOOLEAN -> SerializablePropertyType.BOOLEAN;
            case BIG_DECIMAL -> SerializablePropertyType.BIG_DECIMAL;
            case BYTE_ARRAY -> SerializablePropertyType.BYTE_ARRAY;
            case ENTITY -> SerializablePropertyType.ENTITY;
            case CLASS -> SerializablePropertyType.CLASS;
            case TEXT_SEARCH -> throw new IllegalStateException("TEXT_SEARCH is only supported for domain asset/searchable projection properties");
        };
    }

    private String toClassName(StandardValueType type, String className) {
        return className;
    }

    private void fillMaps(EntityDescription desc, DomainMetaRegistry dr, CustomMetaRegistry cr, RemotingMetaRegistry rr) {
        desc.getMaps().values().forEach((map) -> addMap(new SerializableMapDescription(map.getId(), toSerializableType(map.getKeyType()),
                toClassName(map.getKeyType(), map.getKeyClassName()),
                toSerializableType(map.getValueType()), toClassName(map.getValueType(), map.getValueClassName()),
                isAbstractClass(map.getKeyClassName(), dr, cr, rr), isAbstractClass(map.getValueClassName(), dr, cr, rr))));
    }

    private void fillCollections(EntityDescription desc, DomainMetaRegistry dr, CustomMetaRegistry cr, RemotingMetaRegistry rr) {
        desc.getCollections().values().forEach((coll) -> addCollection(new SerializableCollectionDescription(coll.getId(), toSerializableType(coll.getElementType()),
                        toClassName(coll.getElementType(), coll.getElementClassName()), isAbstractClass(
                        coll.getElementClassName(), dr, cr, rr)
                ))
        );
    }

    private void fillProperties(EntityDescription desc, DomainMetaRegistry dr, CustomMetaRegistry cr, RemotingMetaRegistry rr) {
        desc.getProperties().values().forEach((prop) -> addProperty(new SerializablePropertyDescription(toPropertyId(prop), toSerializableType(prop.getType()),
                        toClassName(prop.getType(), prop.getClassName()), isAbstractClass(
                        prop.getClassName(), dr, cr, rr)
                ))
        );
    }

    private String toPropertyId(StandardPropertyDescription prop) {
        String res = prop.getId();
        if(prop.getParameters().containsKey("fieldName")){
            res = "%s|%s".formatted(prop.getId(), prop.getParameters().get("fieldName"));
        }
        return res;
    }

    @Override
    public Object getPropertyValue(BaseIntrospectableObject obj, String id) {
        return obj.getValue(id);
    }

    public @Override
    Collection<?> getCollection(BaseIntrospectableObject obj, String id) {
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
