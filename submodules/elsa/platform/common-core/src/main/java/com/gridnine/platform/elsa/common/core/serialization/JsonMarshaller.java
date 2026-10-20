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

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.gridnine.platform.elsa.common.core.model.common.BaseIdentity;
import com.gridnine.platform.elsa.common.core.model.common.ClassMapper;
import com.gridnine.platform.elsa.common.core.model.common.EnumMapper;
import com.gridnine.platform.elsa.common.core.model.common.Xeption;
import com.gridnine.platform.elsa.common.core.model.domain.CaptionProvider;
import com.gridnine.platform.elsa.common.core.model.domain.EntityReference;
import com.gridnine.platform.elsa.common.core.model.domain.TextSearchValue;
import com.gridnine.platform.elsa.common.core.serialization.meta.*;
import com.gridnine.platform.elsa.common.core.utils.ExceptionUtils;
import com.gridnine.platform.elsa.common.meta.domain.AssetDescription;
import com.gridnine.platform.elsa.common.meta.domain.DocumentDescription;
import com.gridnine.platform.elsa.common.meta.domain.DomainMetaRegistry;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class JsonMarshaller {

    final static String CLASS_NAME_PROPERTY = "_cn";

    @Autowired
    private ObjectMetadataProvidersFactory metadataProvidersFactory;

    @Autowired
    private EnumMapper enumMapper;

    @Autowired
    private ClassMapper classMapper;

    @Autowired
    private DomainMetaRegistry domainMetaRegistry;

    @Autowired
    private CaptionProvider captionProvider;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final DateTimeFormatter instantFormatter = DateTimeFormatter.ISO_INSTANT;

    private final JsonFactory jsonFactory = new JsonFactory();

    public <T> void marshal(T obj, OutputStream os, Boolean isAbstract, SerializationParameters params) {
        ExceptionUtils.wrapException(() -> {
            try (var generator = jsonFactory.createGenerator(os, JsonEncoding.UTF8)) {
                if (params.isPrettyPrint()) {
                    generator.setPrettyPrinter(new DefaultPrettyPrinter());
                }
                marshal(generator, obj, isAbstract, params, new HashSet<>());
            }
        });

    }

    public <T> void marshalCollection(List<T> obj, OutputStream os, Boolean isAbstract, SerializationParameters params) {
        if(obj.isEmpty()){
            return;
        }
        ExceptionUtils.wrapException(() -> {
            try (var generator = jsonFactory.createGenerator(os, JsonEncoding.UTF8)) {
                if (params.isPrettyPrint()) {
                    generator.setPrettyPrinter(new DefaultPrettyPrinter());
                }
                var processed = new HashSet<Object>();
                generator.writeStartArray();
                obj.forEach(obj1 -> {
                   ExceptionUtils.wrapException(()->marshal(generator, obj1, isAbstract, params, processed));
                });
                generator.writeEndArray();
            }
        });

    }

    private void marshal(JsonGenerator generator, Object obj, boolean isAbstract, SerializationParameters params, Set<Object> processed) throws Exception {
        if (!(obj instanceof EntityReference<?>) && processed.contains(obj)) {
            throw Xeption.forDeveloper("object %s is serialized multiple times".formatted(obj));
        }
        processed.add(obj);
        var key = obj.getClass().getName();
        var index = key.indexOf("_Cached");
        if (index != -1) {
            key = key.substring(0, key.lastIndexOf(".")) + "." + key.substring(index + 7);
        }
        @SuppressWarnings("unchecked") var provider = (BaseObjectMetadataProvider<Object>) metadataProvidersFactory.getProvider(key);
        generator.writeStartObject();
        if (isAbstract) {
            switch (params.getClassSerializationStrategy()) {
                case NAME -> generator.writeStringField(CLASS_NAME_PROPERTY, key);
                case ID -> generator.writeNumberField(CLASS_NAME_PROPERTY, classMapper.getId(key));
            }
        }
        for (SerializablePropertyDescription prop : provider.getAllProperties()) {
            var value = provider.getPropertyValue(obj, prop.id());
            if (value != null) {
                writeJsonProperty(prop.id(), prop.type(), prop.isAbstract(), prop.className(), value, generator, params, processed);
            }
        }
        for (SerializableCollectionDescription coll : provider.getAllCollections()) {
            Collection<?> values = provider.getCollection(obj, coll.id());
            if (!values.isEmpty()) {
                generator.writeFieldName(coll.id());
                generator.writeStartArray();
                for (Object value : values) {
                    switch (coll.elementType()) {
                        case STRING -> generator.writeString((String) value);
                        case UUID -> generator.writeString(value.toString());
                        case ENUM -> {
                            switch (params.getEnumSerializationStrategy()) {
                                case ID -> generator.writeNumber(enumMapper.getId((Enum<?>) value));
                                case NAME -> generator.writeString(((Enum<?>) value).name());
                            }
                        }
                        case ENTITY -> marshal(generator, value, coll.isAbstract(), params, processed);
                        case ENTITY_REFERENCE -> writeEntityReference(generator, value, coll.elementClassName(), params);
                        case LONG -> generator.writeNumber((Long) value);
                        case INT -> generator.writeNumber((Integer) value);
                        case BIG_DECIMAL -> generator.writeNumber((BigDecimal) value);
                        case LOCAL_DATE_TIME -> generator.writeString(((LocalDateTime) value).format(dateTimeFormatter));
                        case INSTANT -> generator.writeString(((Instant) value).atZone(ZoneId.systemDefault()).format(instantFormatter));
                        case LOCAL_DATE -> generator.writeString(((LocalDate) value).format(dateFormatter));
                        case BOOLEAN -> generator.writeBoolean(((Boolean) value));
                        case CLASS -> {
                            switch (params.getClassSerializationStrategy()) {
                                case NAME -> generator.writeString(((Class<?>) value).getName());
                                case ID -> generator.writeNumber(classMapper.getId(((Class<?>) value).getName()));
                            }
                        }
                        case BYTE_ARRAY -> generator.writeBinary((byte[]) value);
                        case TEXT_SEARCH -> throw new UnsupportedOperationException("TEXT_SEARCH is not supported in collections");
                    }
                }
                generator.writeEndArray();
            } else if (params.getEmptyListSerializationStrategy() == SerializationParameters.EmptyListSerializationStrategy.INCLUDE) {
                generator.writeFieldName(coll.id());
                generator.writeStartArray();
                generator.writeEndArray();
            }
        }
        for (SerializableMapDescription mapDescription : provider.getAllMaps()) {
            var map = provider.getMap(obj, mapDescription.id());
            if (!map.isEmpty()) {
                generator.writeFieldName(mapDescription.id());
                generator.writeStartArray();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    generator.writeStartObject();
                    if (entry.getKey() != null) {
                        writeJsonProperty("key", mapDescription.keyType(), mapDescription.keyIsAbstract(), mapDescription.keyClassName(), entry.getKey(), generator, params, processed);
                    }
                    if (entry.getValue() != null) {
                        writeJsonProperty("value", mapDescription.valueType(), mapDescription.valueIsAbstract(), mapDescription.valueClassName(), entry.getValue(), generator, params, processed);
                    }
                    generator.writeEndObject();
                }
                generator.writeEndArray();
            } else if (params.getEmptyListSerializationStrategy() == SerializationParameters.EmptyListSerializationStrategy.INCLUDE) {
                generator.writeFieldName(mapDescription.id());
                generator.writeStartArray();
                generator.writeEndArray();
            }
        }
        generator.writeEndObject();
    }


    private void writeJsonProperty(String id, SerializablePropertyType type, boolean isAbstract, String className, Object value, JsonGenerator generator, SerializationParameters params, Set<Object> processed) throws Exception {
        switch (type) {
            case STRING -> generator.writeStringField(id, (String)value);
            case UUID -> generator.writeStringField(id, value.toString());
            case CLASS -> {
                switch (params.getClassSerializationStrategy()) {
                    case NAME -> generator.writeStringField(id, ((Class<?>) value).getName());
                    case ID -> generator.writeNumberField(id, classMapper.getId(((Class<?>) value).getName()));
                }
            }
            case ENUM -> {
                switch (params.getEnumSerializationStrategy()) {
                    case ID -> generator.writeNumberField(id, enumMapper.getId((Enum<?>) value));
                    case NAME -> generator.writeStringField(id, ((Enum<?>) value).name());
                }
            }
            case ENTITY -> {
                generator.writeFieldName(id);
                marshal(generator, value, isAbstract, params, processed);
            }
            case BIG_DECIMAL -> generator.writeNumberField(id, (BigDecimal) value);
            case INT -> generator.writeNumberField(id, (Integer) value);
            case LONG -> generator.writeNumberField(id, (Long) value);
            case BOOLEAN -> generator.writeBooleanField(id, (Boolean) value);
            case BYTE_ARRAY -> generator.writeBinaryField(id, (byte[]) value);
            case LOCAL_DATE_TIME -> generator.writeStringField(id, ((LocalDateTime) value).format(dateTimeFormatter));
            case INSTANT -> generator.writeStringField(id, ((Instant) value).atZone(ZoneId.systemDefault()).format(instantFormatter));
            case LOCAL_DATE -> generator.writeStringField(id, ((LocalDate) value).format(dateFormatter));
            case ENTITY_REFERENCE -> {
                var er = (EntityReference<?>) value;
                generator.writeFieldName(id);
                writeEntityReference(generator, value, className, params);
            }
            case TEXT_SEARCH -> {
                var tsv = (TextSearchValue) value;
                generator.writeFieldName(id);
                generator.writeStartObject();
                generator.writeStringField("value", tsv.getValue());
                generator.writeFieldName("matchedFragmentPositions");
                generator.writeStartArray();
                for (var pos : tsv.getMatchedFragmentPositions()) {
                    generator.writeNumber(pos);
                }
                generator.writeEndArray();
                generator.writeEndObject();
            }
        }

    }

    private void writeEntityReference(JsonGenerator generator, Object value, String className, SerializationParameters params) throws IOException {
        var er = (EntityReference<?>) value;
        generator.writeStartObject();
        generator.writeFieldName("id");
        generator.writeString(er.getId());
        switch (params.getEntityReferenceTypeSerializationStrategy()) {
            case ALL_CLASS_NAME -> {
                generator.writeFieldName(EntityReference.Fields.type);
                generator.writeString(className);
            }
            case ABSTRACT_CLASS_ID -> {
                if (className.equals("Object") || className.equals(BaseIdentity.class.getName())
                        || metadataProvidersFactory.getProvider(className).isAbstract()) {
                    generator.writeNumberField("type", classMapper.getId(er.getType().getName()));
                }
            }
        }
        switch (params.getEntityReferenceCaptionSerializationStrategy()) {
            case ALL -> generator.writeStringField(EntityReference.Fields.caption, isCachedCaption(er) ? captionProvider.getCaption(er) : er.getCaption());
            case ONLY_NOT_CACHED -> {
                if (!isCachedCaption(er)) {
                    generator.writeStringField(EntityReference.Fields.caption, er.getCaption());
                }
            }
        }
        generator.writeEndObject();
    }

    private boolean isCachedCaption(EntityReference<?> er) {

        DocumentDescription dd = domainMetaRegistry.getDocuments().get(er.getType().getName());
        if (dd != null && (dd.isCacheCaption() || dd.isCacheResolve())) {
            return true;
        }
        AssetDescription ad = domainMetaRegistry.getAssets().get(er.getType().getName());
        return ad != null && (ad.isCacheCaption() || ad.isCacheResolve());
    }
}
