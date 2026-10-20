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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.gridnine.platform.elsa.common.core.model.common.BaseIdentity;
import com.gridnine.platform.elsa.common.core.model.common.ClassMapper;
import com.gridnine.platform.elsa.common.core.model.common.EnumMapper;
import com.gridnine.platform.elsa.common.core.model.common.Xeption;
import com.gridnine.platform.elsa.common.core.model.domain.CaptionProvider;
import com.gridnine.platform.elsa.common.core.model.domain.EntityReference;
import com.gridnine.platform.elsa.common.core.model.domain.TextSearchValue;
import com.gridnine.platform.elsa.common.core.reflection.ReflectionFactory;
import com.gridnine.platform.elsa.common.core.serialization.meta.BaseObjectMetadataProvider;
import com.gridnine.platform.elsa.common.core.serialization.meta.ObjectMetadataProvidersFactory;
import com.gridnine.platform.elsa.common.core.serialization.meta.SerializablePropertyType;
import com.gridnine.platform.elsa.common.core.utils.ExceptionUtils;
import com.gridnine.platform.elsa.common.meta.domain.DomainMetaRegistry;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class JsonUnmarshaller {

    @Autowired
    private ObjectMetadataProvidersFactory metadataProvidersFactory;

    @Autowired
    private ReflectionFactory reflectionFactory;

    @Autowired
    private EnumMapper enumMapper;

    @Autowired
    private ClassMapper classMapper;

    @Autowired
    private CaptionProvider captionProvider;

    @Autowired
    private DomainMetaRegistry domainMetaRegistry;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final JsonFactory jsonFactory = new JsonFactory();


    public <T> List<T> unmarshalCollection(Class<T> cls, InputStream is, SerializationParameters params) {
        return ExceptionUtils.wrapException(()->{
            var result = new ArrayList<T>();
            try (var parser = jsonFactory.createParser(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    parser.nextToken();//ARRAY START
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        var ett = unmarshal(parser, cls.getName(), params);
                        result.add((T) ett);
                    }
            }
            return result;
        });


    }

    public <T> T unmarshal(Class<T> cls, InputStream is, SerializationParameters params) {
        return unmarshal(cls.getName(), is, params);

    }

    public <T> T unmarshal(String className, InputStream is, SerializationParameters params) {
        return ExceptionUtils.wrapException(() -> {
            try (var parser = jsonFactory.createParser(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return unmarshal(parser, className, params);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T> T unmarshal(JsonParser parser, String className, SerializationParameters params) throws Exception {
        T result = null;
        var realClassName = className;
        var provider = "Object".equals(realClassName)? null: (BaseObjectMetadataProvider<Object>) metadataProvidersFactory.getProvider(realClassName);
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if(parser.currentToken() == null){
                break;
            }
            if (parser.currentToken() == JsonToken.START_ARRAY) {
                var nextToken = parser.nextToken();
                if (nextToken == JsonToken.END_ARRAY) {
                    result = reflectionFactory.newInstance(realClassName);
                    break;
                }
            }
            var tagName = parser.currentName();
            if (JsonMarshaller.CLASS_NAME_PROPERTY.equals(tagName)) {
                parser.nextToken();
                realClassName = parser.getText();
                if (params.getClassSerializationStrategy() == SerializationParameters.ClassSerializationStrategy.ID) {
                    realClassName = classMapper.getName(Integer.parseInt(realClassName));
                }
                provider = (BaseObjectMetadataProvider<Object>) metadataProvidersFactory.getProvider(realClassName);
                continue;
            }
            if (result == null) {
                result = reflectionFactory.newInstance(realClassName);
            }
            var propertyDescription = provider.getProperty(tagName);
            if (propertyDescription != null) {
                var value = readJsonValue(propertyDescription.type(), propertyDescription.className(), parser, params);
                provider.setPropertyValue(result, propertyDescription.id(), value);
                continue;
            }
            var collectionDescription = provider.getCollection(tagName);
            if (collectionDescription != null) {
                var collection = (Collection<Object>) provider.getCollection(result, tagName);
                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    if (parser.currentToken() == JsonToken.START_ARRAY && parser.nextToken() == JsonToken.END_ARRAY) {
                        break;
                    }
                    var value = switch (collectionDescription.elementType()) {
                        case STRING -> parser.getText();
                        case UUID -> parser.getText() == null? null: UUID.fromString(parser.getText());
                        case ENUM -> switch (params.getEnumSerializationStrategy()) {
                            case ID -> reflectionFactory.safeGetEnum(collectionDescription.elementClassName(),
                                    enumMapper.getName(parser.getIntValue(),
                                            reflectionFactory.getClass(collectionDescription.elementClassName())));
                            case NAME -> reflectionFactory.safeGetEnum(collectionDescription.elementClassName(), parser.getText());
                        };
                        case CLASS -> switch (params.getClassSerializationStrategy()) {
                            case ID -> reflectionFactory.getClass(classMapper.getName(parser.getIntValue()));
                            case NAME -> reflectionFactory.getClass(parser.getText());
                        };
                        case ENTITY -> unmarshal(parser, collectionDescription.elementClassName(), params);
                        case BIG_DECIMAL -> BigDecimal.valueOf(parser.getDoubleValue());
                        case INT -> parser.getIntValue();
                        case ENTITY_REFERENCE -> readEntityReference(parser, collectionDescription.elementClassName(), params);
                        case LONG -> parser.getLongValue();
                        case BYTE_ARRAY -> parser.getBinaryValue();
                        case LOCAL_DATE_TIME -> dateTimeFormatter.parse(parser.getText());
                        case INSTANT -> Instant.parse(parser.getText()).atZone(ZoneId.systemDefault()).toInstant();
                        case LOCAL_DATE -> dateFormatter.parse(parser.getText());
                        case BOOLEAN -> parser.getBooleanValue();
                        case TEXT_SEARCH -> throw new UnsupportedOperationException("TEXT_SEARCH is not supported in collections");
                    };
                    collection.add(value);
                }
                continue;
            }
            var mapDescription = provider.getMap(tagName);
            if (mapDescription != null) {
                var map = ((Map<Object, Object>) provider.getMap(result, tagName));
                parser.nextToken();
                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    parser.nextToken();
                    var keyValue = readJsonValue(mapDescription.keyType(), mapDescription.keyClassName(), parser, params);
                    parser.nextToken();
                    var valueValue = readJsonValue(mapDescription.valueType(), mapDescription.valueClassName(), parser, params);
                    map.put(keyValue, valueValue);
                    parser.nextToken();
                }
            }
        }
        if (result == null) {
            result = reflectionFactory.newInstance(realClassName);
        }
        return result;
    }

    private Object readJsonValue(SerializablePropertyType type, String className, JsonParser parser, SerializationParameters params) throws Exception {
        var nextToken = parser.nextToken();
        if(nextToken == JsonToken.VALUE_NULL){
            return null;
        }
        return switch (type) {
            case STRING -> parser.getText();
            case UUID -> parser.getText() == null? null: UUID.fromString(parser.getText());
            case ENUM -> switch (params.getEnumSerializationStrategy()) {
                case ID -> reflectionFactory.safeGetEnum(className, enumMapper.getName(parser.getIntValue(), reflectionFactory.getClass(className)));
                case NAME -> reflectionFactory.safeGetEnum(className, parser.getText());
            };
            case CLASS -> switch (params.getClassSerializationStrategy()) {
                case ID -> reflectionFactory.getClass(classMapper.getName(parser.getIntValue()));
                case NAME -> reflectionFactory.getClass(parser.getText());
            };
            case ENTITY_REFERENCE -> readEntityReference(parser, className, params);
            case ENTITY -> unmarshal(parser, className, params);
            case BIG_DECIMAL -> BigDecimal.valueOf(parser.getDoubleValue());
            case INT -> parser.getIntValue();
            case LONG -> parser.getLongValue();
            case BYTE_ARRAY -> parser.getBinaryValue();
            case LOCAL_DATE_TIME -> LocalDateTime.parse(parser.getText(), dateTimeFormatter);
            case INSTANT -> Instant.parse(parser.getText()).atZone(ZoneId.systemDefault()).toInstant();
            case LOCAL_DATE -> LocalDate.parse(parser.getText(), dateFormatter);
            case BOOLEAN -> parser.getBooleanValue();
            case TEXT_SEARCH -> readTextSearchValue(parser);
        };
    }

    private TextSearchValue readTextSearchValue(JsonParser parser) throws IOException {
        var result = new TextSearchValue();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            var tagName = parser.currentName();
            if ("value".equals(tagName)) {
                parser.nextToken();
                result.setValue(parser.getText());
            } else if ("matchedFragmentPositions".equals(tagName)) {
                parser.nextToken();
                var positions = new ArrayList<Integer>();
                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    positions.add(parser.getIntValue());
                }
                result.setMatchedFragmentPositions(positions.stream().mapToInt(Integer::intValue).toArray());
            }
        }
        return result;
    }

    private EntityReference<?> readEntityReference(JsonParser parser, String className, SerializationParameters params) throws IOException {
        var result = new EntityReference<>();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            var tagName = parser.currentName();
            parser.nextToken();
            switch (tagName) {
                case BaseIdentity.Fields.idName -> result.setId(parser.getValueAsString());
                case EntityReference.Fields.type -> {
                    switch (params.getEntityReferenceTypeSerializationStrategy()) {
                        case ALL_CLASS_NAME -> result.setType(reflectionFactory.getClass(parser.getText()));
                        case ABSTRACT_CLASS_ID -> result.setType(reflectionFactory.getClass(classMapper.getName(parser.getIntValue())));
                    }
                }
                case EntityReference.Fields.caption -> result.setCaption(parser.getText());
            }
        }
        if (result.getType() == null) {
            if (metadataProvidersFactory.getProvider(className).isAbstract()) {
                throw Xeption.forDeveloper("no classname is provided for abstract type %s".formatted(className));
            } else {
                result.setType(reflectionFactory.getClass(className));
            }
        }
        var dd = domainMetaRegistry.getDocuments().get(result.getType().getName());
        var ad = domainMetaRegistry.getAssets().get(result.getType().getName());
        if ((dd != null && (dd.isCacheCaption() || dd.isCacheResolve())) || (ad != null && (ad.isCacheCaption() || ad.isCacheResolve()))) {
            result.setCaption(captionProvider.getCaption(result));
        }
        return result;
    }

}
