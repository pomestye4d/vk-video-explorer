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

package com.gridnine.platform.elsa.gradle.codegen.remoting;

import com.gridnine.platform.elsa.gradle.codegen.common.CodeGenerator;
import com.gridnine.platform.elsa.gradle.codegen.common.JavaCodeGeneratorUtils;
import com.gridnine.platform.elsa.gradle.meta.common.*;
import com.gridnine.platform.elsa.gradle.meta.domain.DomainMetaRegistry;
import com.gridnine.platform.elsa.gradle.meta.remoting.RemotingMetaRegistry;
import com.gridnine.platform.elsa.gradle.parser.domain.DomainMetaRegistryParser;
import com.gridnine.platform.elsa.gradle.parser.remoting.RemotingMetaRegistryParser;
import com.gridnine.platform.elsa.gradle.plugin.ElsaGlobalData;

import java.io.File;
import java.util.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public class OpenapiRemotingCodeGenerator implements CodeGenerator<OpenApiCodeGenRecord> {
    private final String schemaPrefix = "#/components/schemas";

    @SuppressWarnings("CodeBlock2Expr")
    @Override
    public void generate(OpenApiCodeGenRecord record, File openApiFile, Set<File> generatedFiles, Map<Object, Object> context) throws Exception {
        ElsaGlobalData elsaGlobalData = (ElsaGlobalData) context.get("elsa-global-data");
        var rmr = new RemotingMetaRegistry();
        {
            var parser = new RemotingMetaRegistryParser();
            // невозможно прокастить elsaGlobalData.getRemotingRecords до JavaRemotingCodeGenRecord из за разных класслоадеров
            for (Object obj : elsaGlobalData.getRemotingRecords().values().stream().flatMap(Collection::stream).toList()) {
                parser.updateMetaRegistry(rmr, (List<File>) obj.getClass().getMethod("getSources").invoke(obj));
            }
        }
        var dmr = new DomainMetaRegistry();
        {
            var parser = new DomainMetaRegistryParser();
            for (Object obj : elsaGlobalData.getDomainRecords().values()) {
                parser.updateMetaRegistry(dmr, (List<File>) obj.getClass().getMethod("getSources").invoke(obj));
            }
        }
        Map<String, EntityDescription> entities = new LinkedHashMap<>();
        Map<String, EnumDescription> enums = new LinkedHashMap<>();

        var root = new LinkedHashMap<>();
        root.put("openapi", "3.0.1");
        var info = new LinkedHashMap<>();
        root.put("info", info);
        info.put("title", "OpenAPI definition");
        info.put("description", "This page contains generated endpoint definitions for services %s".formatted(record.getRestIds()));
        info.put("termsOfService", "https://swagger.io/terms/");
        var license = new LinkedHashMap<>();
        info.put("license", license);
        license.put("name", "Apache 2.0");
        license.put("url", " https://springdoc.org");
        info.put("version", "0.0.1");
        var mainServer = new LinkedHashMap<>();
        mainServer.put("url", "/api");
        root.put("servers", List.of(mainServer));

        var paths = new LinkedHashMap<>();
        root.put("paths", paths);
        var components = new LinkedHashMap<>();
        root.put("components", components);
        var schemas = new LinkedHashMap<>();
        components.put("schemas", schemas);
        rmr.getRemotings().values().stream().filter(it -> record.getRestIds().contains(it.getId())).forEach(remoting -> {
            remoting.getGroups().values().forEach(group -> {
                group.getServices().values().forEach(service -> {
                    var path = "/%s/%s/%s".formatted(remoting.getId(), group.getId(), service.getPath() == null ? service.getId() : service.getPath());
                    var methods = (LinkedHashMap) Optional.ofNullable(paths.get(path)).orElse(new LinkedHashMap<>());
                    paths.put(path, methods);
                    final String rqClassName = service.getRequestClassName();
                    final String rsClassName = service.getResponseClassName();
                    switch (service.getMethod()) {
                        case GET, DELETE -> {
                            var method = new LinkedHashMap<>();
                            methods.put(service.getMethod().name().toLowerCase(), method);
                            method.put("tags", List.of("/%s/%s".formatted(remoting.getId(), group.getId())));
                            if (rqClassName != null) {
                                method.put("parameters", getParameters(rqClassName, rmr, dmr));
                            }
                            if (rsClassName != null) {
                                method.put("responses", getResponses(rsClassName));
                            }
                        }
                        case POST, PUT -> {
                            var method = new LinkedHashMap<>();
                            methods.put(service.getMethod().name().toLowerCase(), method);
                            method.put("tags", List.of("/%s/%s".formatted(remoting.getId(), group.getId())));
                            if (rqClassName != null) {
                                EntityDescription ed = getEntityDescription(rqClassName, rmr, dmr);
                                method.put("requestBody", Map.of("content", getContent(rqClassName, service.isMultipartRequest())));
                            }
                            if (rsClassName != null) {
                                method.put("responses", getResponses(rsClassName));
                            }
                        }
                    }
                    if (rqClassName != null && !schemas.containsKey(rqClassName)) {
                        addClassSchema(schemas, rqClassName, rmr, dmr);
                    }
                    if (rsClassName != null && !schemas.containsKey(rsClassName)) {
                        addClassSchema(schemas, rsClassName, rmr, dmr);
                    }
                });
            });
        });
        addEntityReferenceClassSchema(schemas);

        StringBuilder gen = new StringBuilder();
        process(gen, root, false, 0);
        var file = JavaCodeGeneratorUtils.saveIfDiffers(gen.toString(), openApiFile.getName(), openApiFile.getParentFile());
    }

    private ArrayList<Map<Object, Object>> getParameters(final String rqClassName, final RemotingMetaRegistry rmr,
                                                         final DomainMetaRegistry dmr) {
        List<StandardPropertyDescription> properties = getPropertiesWithInherited(rqClassName, rmr, dmr);
        var parameters = new ArrayList<Map<Object, Object>>();
        properties.forEach(prop -> {
            var param = new LinkedHashMap<>();
            parameters.add(param);
            param.put("name", prop.getId());
            param.put("in", "query");
            param.put("required", Boolean.toString(prop.isNonNullable()));
            param.put("schema", getSchema(prop, rmr, dmr));
        });
        return parameters;
    }

    private List<StandardPropertyDescription> getPropertiesWithInherited(final String className, final RemotingMetaRegistry rmr,
                                                                         final DomainMetaRegistry dmr) {
        EntityDescription ed = getEntityDescription(className, rmr, dmr);
        if (!ed.getCollections().isEmpty()) {
            throw new IllegalArgumentException("collection is not supported in request entity " + className);
        }
        if (!ed.getMaps().isEmpty()) {
            throw new IllegalArgumentException("map is not supported in request entity " + className);
        }
        List<StandardPropertyDescription> properties = new ArrayList<>();
        if (ed.getExtendsId() != null) {
            properties.addAll(getPropertiesWithInherited(ed.getExtendsId(), rmr, dmr));
        }
        properties.addAll(ed.getProperties().values());
        return properties;
    }

    private LinkedHashMap getResponses(final String responseClassName) {
        var responses = new LinkedHashMap<>();
        var code200 = new LinkedHashMap<>();
        responses.put("'200'", code200);
        code200.put("description", "''");
        code200.put("content", getContent(responseClassName));
        return responses;
    }

    private LinkedHashMap getContent(final String responseClassName) {
        return getContent(responseClassName, false);
    }

    private LinkedHashMap getContent(final String responseClassName, boolean isMultipart) {
        var content = new LinkedHashMap<>();
        var mimeType = new LinkedHashMap<>();
        boolean isBinaryData = (responseClassName != null) && responseClassName.endsWith("BinaryData");
        String contentKey;
        if (isMultipart) {
            contentKey = "multipart/form-data";
        } else if (isBinaryData) {
            contentKey = "application/octet-stream";
        } else {
            contentKey = "application/json";
        }
        content.put(contentKey, mimeType);
        var schema = new LinkedHashMap<>();
        mimeType.put("schema", schema);
        if (isBinaryData) {
            schema.put("type", "string");
            schema.put("format", "binary");
        } else {
            schema.put("$ref", "'%s/%s'".formatted(schemaPrefix, responseClassName));
        }
        return content;
    }

    private Map<Object, Object> getSchema(BaseModelElementDescription desc, RemotingMetaRegistry rmr, DomainMetaRegistry dmr) {
        var schema = new LinkedHashMap<>();
        StandardValueType valueType;
        String className;
        if (desc instanceof StandardPropertyDescription propDesc) {
            valueType = propDesc.getType();
            className = propDesc.getClassName();
        } else if (desc instanceof StandardCollectionDescription collDesc) {
            valueType = collDesc.getElementType();
            className = collDesc.getElementClassName();
        } else if (desc instanceof StandardMapDescription mapDesc) {
            valueType = mapDesc.getValueType();
            className = mapDesc.getValueClassName();
        } else {
            throw new IllegalArgumentException("unsupported description type " + desc.getClass().getSimpleName());
        }
        boolean isBinaryData = (className != null) && className.endsWith("BinaryData");

        switch (valueType) {
            case STRING -> schema.put("type", "string");
            case LOCAL_DATE -> {
                schema.put("type", "string");
                schema.put("format", "date");
            }
            case LOCAL_DATE_TIME, INSTANT -> {
                schema.put("type", "string");
                schema.put("format", "date-time");
            }
            case ENUM -> {
                schema.put("type", "string");
                EnumDescription ed = getEnumDescription(className, rmr, dmr);
                schema.put("enum", new ArrayList<>(ed.getItems().keySet()));
            }
            case CLASS -> {
                schema.put("type", "string");
                schema.put("format", "class");
            }
            case BOOLEAN -> schema.put("type", "boolean");
            case BYTE_ARRAY -> {
                schema.put("type", "string");
                schema.put("format", "binary");
            }
            case ENTITY -> {
                if (isBinaryData) {
                    schema.put("type", "string");
                    schema.put("format", "binary");
                } else {
                    schema.put("$ref", "'%s/%s'".formatted(schemaPrefix, className));
                }
            }
            case ENTITY_REFERENCE -> schema.put("$ref",
                    "'%s/com.gridnine.platform.elsa.common.core.model.domain.EntityReference'".formatted(schemaPrefix));
            case LONG -> {
                schema.put("type", "integer");
                schema.put("format", "int64");
            }
            case INT -> {
                schema.put("type", "integer");
                schema.put("format", "int32");
            }
            case BIG_DECIMAL -> schema.put("type", "number");
            case UUID -> {
                schema.put("type", "string");
                schema.put("format", "uuid");
            }
            case TEXT_SEARCH -> throw new IllegalArgumentException("TEXT_SEARCH is not supported in OpenAPI codegen");
            default -> throw new IllegalArgumentException("unsupported type %s".formatted(valueType));
        }
        return schema;
    }

    private EnumDescription getEnumDescription(String className, RemotingMetaRegistry rmr, DomainMetaRegistry dmr) {
        EnumDescription ed = rmr.getEnums().get(className);
        if (ed != null) {
            return ed;
        }
        ed = dmr.getEnums().get(className);
        if (ed != null) {
            return ed;
        }
        throw new IllegalArgumentException("unsupported enum " + className);
    }

    private EntityDescription getEntityDescription(String className, RemotingMetaRegistry rmr, DomainMetaRegistry dmr) {
        EntityDescription ed = rmr.getEntities().get(className);
        if (ed != null) {
            return ed;
        }
        ed = dmr.getEntities().get(className);
        if (ed != null) {
            return ed;
        }
        throw new IllegalArgumentException("unsupported entity " + className);
    }

    private void addClassSchema(final LinkedHashMap<Object, Object> schemas, final String className,
                                final RemotingMetaRegistry rmr, final DomainMetaRegistry dmr) {
        if ((className != null) && className.endsWith("BinaryData")) {
            return;
        }
        EntityDescription ed = getEntityDescription(className, rmr, dmr);
        var classSchema = new LinkedHashMap<>();
        if (ed.getExtendsId() != null) {
            ArrayList<Map<Object, Object>> allOf = new ArrayList<>();
            schemas.put(className, Map.of("allOf", allOf));
            var ref = new LinkedHashMap<>();
            allOf.add(ref);
            ref.put("$ref", "'%s/%s'".formatted(schemaPrefix, ed.getExtendsId()));
            allOf.add(classSchema);
            if (!schemas.containsKey(ed.getExtendsId())) {
                addClassSchema(schemas, ed.getExtendsId(), rmr, dmr);
            }
        } else {
            schemas.put(className, classSchema);
        }
        classSchema.put("type", "object");
        var properties = new LinkedHashMap<>();
        classSchema.put("properties", properties);
        ed.getProperties().forEach((name, desc) -> {
            properties.put(name, getSchema(desc, rmr, dmr));
            if (desc.getType() == StandardValueType.ENTITY && !schemas.containsKey(desc.getClassName())) {
                addClassSchema(schemas, desc.getClassName(), rmr, dmr);
            }
        });
        ed.getCollections().forEach((name, desc) -> {
            var collection = new LinkedHashMap<>();
            properties.put(name, collection);
            collection.put("type", "array");
            collection.put("items", getSchema(desc, rmr, dmr));
            if (desc.getElementType() == StandardValueType.ENTITY && !schemas.containsKey(desc.getElementClassName())) {
                addClassSchema(schemas, desc.getElementClassName(), rmr, dmr);
            }
        });
        ed.getMaps().forEach((name, desc) -> {
            var map = new LinkedHashMap<>();
            properties.put(name, map);
            map.put("type", "object");
            map.put("additionalProperties", getSchema(desc, rmr, dmr));
            if (desc.getKeyType() == StandardValueType.ENTITY && !schemas.containsKey(desc.getKeyClassName())) {
                addClassSchema(schemas, desc.getKeyClassName(), rmr, dmr);
            }
            if (desc.getValueType() == StandardValueType.ENTITY && !schemas.containsKey(desc.getValueClassName())) {
                addClassSchema(schemas, desc.getValueClassName(), rmr, dmr);
            }
        });
        if (properties.isEmpty()) {
            classSchema.remove("properties");
        }
    }

    private void addEntityReferenceClassSchema(final LinkedHashMap<Object, Object> schemas) {
        var classSchema = new LinkedHashMap<>();
        schemas.put("com.gridnine.platform.elsa.common.core.model.domain.EntityReference", classSchema);
        classSchema.put("type", "object");
        var properties = new LinkedHashMap<>();
        classSchema.put("properties", properties);
        properties.put("id", Map.of("type", "string"));
        properties.put("type", Map.of("type", "string"));
        properties.put("caption", Map.of("type", "string"));
    }

    private void indentation(StringBuilder gen, int indent) {
        gen.append(" ".repeat(Math.max(0, indent)));
    }

    private void process(StringBuilder gen, Map<Object, Object> root, boolean isCollectionElement, int startingIndent) {
        int indent = startingIndent;
        boolean firstMapEntry = true;
        for (Map.Entry<Object, Object> entry : root.entrySet()) {
            if (!isCollectionElement || !firstMapEntry) {
                if (!gen.isEmpty()) {
                    gen.append("\n");
                }
                indentation(gen, indent);
            }
            firstMapEntry = false;
            Object key = entry.getKey();
            gen.append("%s:".formatted(key));
            Object value = entry.getValue();
            if (value instanceof Map<?, ?>) {
                indent += 2;
                process(gen, (Map<Object, Object>) value, false, indent);
                indent -= 2;
            } else if (value instanceof List<?> lst) {
                indent += 2;
                for (Object item : lst) {
                    gen.append("\n");
                    indentation(gen, indent);
                    gen.append("- ");
                    if (item instanceof Map<?, ?> map) {
                        indent += 2;
                        process(gen, (Map<Object, Object>) map, true, indent);
                        indent -= 2;
                    } else {
                        gen.append(item.toString());
                    }
                }
                indent -= 2;
            } else {
                gen.append(" %s".formatted(value));
            }
        }
    }
}
