/*****************************************************************
 * Gridnine http://www.gridnine.com
 * Project: Elsa
 *****************************************************************/

package com.gridnine.platform.elsa.core.remoting;

import com.gridnine.platform.elsa.common.core.utils.TextUtils;
import com.gridnine.platform.elsa.common.meta.common.StandardPropertyDescription;
import com.gridnine.platform.elsa.common.meta.remoting.*;
import com.gridnine.platform.elsa.core.remoting.standard.BinaryData;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.TypeNameResolver;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.springframework.http.MediaType.*;

@SuppressWarnings({"SameParameterValue", "rawtypes", "DataFlowIssue"})
public class MetadataBasedOpenApiFactory {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final RemotingMetaRegistry registry;

    private final Map<String, PathItem> path2pathItem = new HashMap<>();

    private final String schemaPrefix = "#/components/schemas";

    private final static Set<HttpMethod> REQUEST_BODY_METHODS = Set.of(HttpMethod.POST, HttpMethod.PUT);

    public MetadataBasedOpenApiFactory(RemotingMetaRegistry registry) {
        this.registry = registry;
    }

    public OpenAPI createOpenApi() {
        var api = new OpenAPI()
                .info(new Info()
                        .title("OpenAPI definition")
                        .version("0.0.1")
                        .description("This page contains generated endpoint definitions for the LDocs project.")
                        .termsOfService("https://swagger.io/terms/")
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")))
                .components(new Components());
        TypeNameResolver.std.setUseFqn(true); // Use fully-qualified names.
        api = generateRemotingsDefinitions(api);
        return resolveRecursiveSchemas(api);
    }

    private OpenAPI generateRemotingsDefinitions(OpenAPI api) {
        Map<String, RemotingDescription> remotings = registry.getRemotings();
        for (var entry : remotings.entrySet()) {
            api = processRemotingDescription(api, entry.getValue());
        }
        return api;
    }

    private OpenAPI processRemotingDescription(OpenAPI api, RemotingDescription desc) {
        String path = "/" + desc.getId();
        Map<String, RemotingGroupDescription> groups = desc.getGroups();
        for (var entry : groups.entrySet()) {
            api = processRemotingGroupDescription(api, path, entry.getValue());
        }
        return api;
    }

    private OpenAPI processRemotingGroupDescription(OpenAPI api, String prefix, RemotingGroupDescription desc) {
        String path = String.format("%s/%s", prefix, desc.getId());

        Map<String, ServiceDescription> services = desc.getServices();
        for (var entry : services.entrySet()) {
            api = processServiceDescription(api, path, entry.getValue());
        }
        return api;
    }

    private OpenAPI processServiceDescription(final OpenAPI api, String prefix, ServiceDescription desc) {
        final String path = prefix + "/" + (StringUtils.isBlank(desc.getPath()) ? desc.getId() : desc.getPath());
        try {
            boolean needsRequestBody = StringUtils.isNotBlank(desc.getRequestClassName())
                    && REQUEST_BODY_METHODS.contains(desc.getMethod());
            boolean needsRequestParams = StringUtils.isNotBlank(desc.getRequestClassName())
                    && !needsRequestBody; // note: currently no support for both body and params
            List<Parameter> requestParams = null;
            RequestBody requestBody = null;

            if (needsRequestBody) {
                requestBody = generateRequestBody(api, desc);
            }
            if (needsRequestParams) {
                requestParams = generateRequestParams(desc);
            }
            generateResponseSchemas(api, desc);
            Operation operation = generateOperation(prefix, desc, requestParams, requestBody);

            switch (desc.getMethod()) {
                case GET -> path2pathItem.compute(path, (key, val) ->
                        Objects.requireNonNullElseGet(val, () -> addPathItem(api, path)).get(operation));
                case PUT -> path2pathItem.compute(path, (key, val) ->
                        Objects.requireNonNullElseGet(val, () -> addPathItem(api, path)).put(operation));
                case DELETE -> path2pathItem.compute(path, (key, val) ->
                        Objects.requireNonNullElseGet(val, () -> addPathItem(api, path)).delete(operation));
                case POST -> path2pathItem.compute(path, (key, val) ->
                        Objects.requireNonNullElseGet(val, () -> addPathItem(api, path)).post(operation));
            }
        } catch (ClassNotFoundException e) {
            log.error("Schema generation failed!", e);
        }
        return api;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Parameter> generateRequestParams(ServiceDescription desc) throws ClassNotFoundException {
        ModelConverters converter = ModelConverters.getInstance();
        List<Parameter> requestParams = new ArrayList<>();
        Map<String, Schema> prop2schema = converter.readAll(Class.forName(desc.getRequestClassName()))
                .get(desc.getRequestClassName()).getProperties();
        var entities = registry.getEntities()
                .get(desc.getRequestClassName());
        Map<String, StandardPropertyDescription> properties = entities.getProperties();
        for (var prop : prop2schema.keySet()) {
            requestParams.add(new Parameter().name(prop).schema(prop2schema.get(prop))
                    .in(ParameterIn.QUERY.toString()).required(Optional.ofNullable(properties.get(prop))
                            .map(StandardPropertyDescription::isNonNullable).orElse(false)));
        }
        return requestParams;
    }

    @SuppressWarnings("rawtypes")
    private RequestBody generateRequestBody(OpenAPI api, ServiceDescription desc) throws ClassNotFoundException {
        ModelConverters converter = ModelConverters.getInstance();
        Map<String, Schema> requestSchemas = converter.readAll(Class.forName(desc.getRequestClassName()));
        requestSchemas.forEach((clazz, schema) ->
                api.getComponents().addSchemas(clazz, BinaryData.class.getName().equals(clazz)
                        ? new BinarySchema() : schema));
        String typeName = desc.isMultipartRequest() ? MULTIPART_FORM_DATA_VALUE : APPLICATION_JSON_VALUE;
        return new RequestBody().required(true).content(new Content().addMediaType(typeName,
                new MediaType().schema(new ObjectSchema().$ref("%s/%s".formatted(schemaPrefix, desc.getRequestClassName())))));
    }

    @SuppressWarnings("rawtypes")
    private void generateResponseSchemas(OpenAPI api, ServiceDescription desc) throws ClassNotFoundException {
        if(TextUtils.isBlank(desc.getResponseClassName())){
            return;
        }
        ModelConverters converter = ModelConverters.getInstance();
        Map<String, Schema> responseSchemas = converter.readAll(Class.forName(desc.getResponseClassName()));
        responseSchemas.forEach((clazz, schema) ->
                api.getComponents().addSchemas(clazz, BinaryData.class.getName().equals(clazz)
                        ? new BinarySchema() : schema));
    }

    private Operation generateOperation(String prefix, ServiceDescription desc,
                                        List<Parameter> requestParams, RequestBody requestBody) throws ClassNotFoundException {
        String rsMediaType = BinaryData.class.getName().equals(desc.getResponseClassName())
                ? APPLICATION_OCTET_STREAM_VALUE : APPLICATION_JSON_VALUE;
        Schema rsSchema = BinaryData.class.getName().equals(desc.getResponseClassName())
                ? new BinarySchema() : new ObjectSchema().$ref("%s/%s".formatted(schemaPrefix, desc.getResponseClassName()));
        return new Operation().addTagsItem(prefix).parameters(requestParams).requestBody(requestBody)
                .responses(new ApiResponses().addApiResponse("200", new ApiResponse().description("")
                        .content(new Content().addMediaType(rsMediaType, new MediaType().schema(rsSchema)))));
    }

    private PathItem addPathItem(OpenAPI api, String path) {
        var pi = new PathItem();
        api.path(path, pi);
        return pi;
    }

    private OpenAPI resolveRecursiveSchemas(final OpenAPI api) {
        Map<String, Schema> class2schema = api.getComponents().getSchemas();
        registry.getEntities().forEach((entity, entityDesc) -> {
            Schema schemaToUpdate = class2schema.get(entity);
            if (schemaToUpdate == null) {
                return;
            }

            // case 1: recursive property schema
            entityDesc.getProperties().forEach((prop, propDesc) -> {
                if (!schemaToUpdate.getProperties().containsKey(prop)) {
                    Schema refSchema = new Schema();
                    refSchema.set$ref("%s/%s".formatted(schemaPrefix, entity));
                    schemaToUpdate.addProperty(prop, refSchema);
                }
            });

            // case 2: recursive collection schema
            entityDesc.getCollections().forEach((coll, collDesc) -> {
                if (!schemaToUpdate.getProperties().containsKey(coll)) {
                    Schema refSchema = new Schema();
                    refSchema.set$ref("%s/%s".formatted(schemaPrefix, collDesc.getElementClassName()));
                    ArraySchema arraySchema = new ArraySchema();
                    arraySchema.setName(coll);
                    arraySchema.setItems(refSchema);
                    schemaToUpdate.addProperty(coll, arraySchema);
                }
            });
        });
        return api;
    }
}
