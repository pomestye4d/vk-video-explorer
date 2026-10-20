package com.gridnine.platform.elsa.common.core.serialization;

import com.gridnine.platform.elsa.common.core.reflection.ReflectionFactory;
import com.gridnine.platform.elsa.common.core.serialization.meta.BaseObjectMetadataProvider;
import com.gridnine.platform.elsa.common.core.serialization.meta.ObjectMetadataProvidersFactory;
import com.gridnine.platform.elsa.common.core.serialization.meta.SerializablePropertyType;
import com.gridnine.platform.elsa.common.core.utils.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class ParameterMapConverter {

    @Autowired
    private ObjectMetadataProvidersFactory metadataProvidersFactory;

    @Autowired
    private ReflectionFactory reflectionFactory;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final String UOE_MESSAGE = "ENTITY_REFERENCE, ENTITY, BYTE_ARRAY, MAP are not supported";

    public <T> T convert(Class<T> cls, final Map<String, String[]> params) {
        return ExceptionUtils.wrapException(() -> convert(cls.getName(), params));
    }

    @SuppressWarnings("unchecked")
    public <T> T convert(String className, final Map<String, String[]> params) {
        T result = null;
        var realClassName = className;
        var provider = (BaseObjectMetadataProvider<Object>) metadataProvidersFactory.getProvider(realClassName);
        for (String fieldName : params.keySet()) {
            if (result == null) {
                result = reflectionFactory.newInstance(realClassName);
            }
            var propertyDescription = provider.getProperty(fieldName);
            if (propertyDescription != null) {
                var value = convertFromString(propertyDescription.type(), propertyDescription.className(), params.get(fieldName));
                provider.setPropertyValue(result, propertyDescription.id(), value);
                continue;
            }
            var collectionDescription = provider.getCollection(fieldName);
            if (collectionDescription != null) {
                var collection = (Collection<Object>) provider.getCollection(result, fieldName);
                var vit = Arrays.stream(params.get(fieldName)).iterator();
                while (vit.hasNext()) {
                    var value = switch (collectionDescription.elementType()) {
                        case STRING -> vit.next();
                        case UUID -> UUID.fromString(vit.next());
                        case ENUM -> reflectionFactory.safeGetEnum(className, vit.next());
                        case CLASS -> reflectionFactory.getClass(vit.next());
                        case ENTITY_REFERENCE, ENTITY, BYTE_ARRAY, TEXT_SEARCH -> throw new UnsupportedOperationException(UOE_MESSAGE);
                        case BIG_DECIMAL -> BigDecimal.valueOf(Double.parseDouble(vit.next()));
                        case INT -> Integer.parseInt(vit.next());
                        case LONG -> Long.parseLong(vit.next());
                        case LOCAL_DATE_TIME -> LocalDateTime.parse(vit.next(), dateTimeFormatter);
                        case INSTANT -> Instant.parse(vit.next()).atZone(ZoneId.systemDefault()).toInstant();
                        case LOCAL_DATE -> LocalDate.parse(vit.next(), dateFormatter);
                        case BOOLEAN -> Boolean.parseBoolean(vit.next());
                    };
                    collection.add(value);
                }
                continue;
            }
            var mapDescription = provider.getMap(fieldName);
            if (mapDescription != null) {
                throw new UnsupportedOperationException(UOE_MESSAGE);
            }
        }
        if (result == null) {
            result = reflectionFactory.newInstance(realClassName);
        }
        return result;
    }

    private Object convertFromString(SerializablePropertyType type, String className, String[] values) {
        if (values.length == 0) {
            return null;
        }
        return switch (type) {
            case STRING -> values[0];
            case UUID -> UUID.fromString(values[0]);
            case ENUM -> reflectionFactory.safeGetEnum(className, values[0]);
            case CLASS -> reflectionFactory.getClass(values[0]);
            case ENTITY_REFERENCE, ENTITY, BYTE_ARRAY, TEXT_SEARCH -> throw new UnsupportedOperationException(UOE_MESSAGE);
            case BIG_DECIMAL -> BigDecimal.valueOf(Double.parseDouble(values[0]));
            case INT -> Integer.parseInt(values[0]);
            case LONG -> Long.parseLong(values[0]);
            case LOCAL_DATE_TIME -> LocalDateTime.parse(values[0], dateTimeFormatter);
            case INSTANT -> Instant.parse(values[0]).atZone(ZoneId.systemDefault()).toInstant();
            case LOCAL_DATE -> LocalDate.parse(values[0], dateFormatter);
            case BOOLEAN -> Boolean.parseBoolean(values[0]);
        };
    }
}
