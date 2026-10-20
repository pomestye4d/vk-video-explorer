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

package com.gridnine.platform.elsa.gradle.codegen.webApp.helpers;

import com.gridnine.platform.elsa.gradle.codegen.common.JavaCodeGenerator;
import com.gridnine.platform.elsa.gradle.codegen.common.JavaCodeGeneratorUtils;
import com.gridnine.platform.elsa.gradle.meta.common.StandardValueType;
import com.gridnine.platform.elsa.gradle.meta.webApp.*;
import com.gridnine.platform.elsa.gradle.parser.webApp.WebAppMetadataHelper;
import com.gridnine.platform.elsa.gradle.utils.BuildExceptionUtils;
import com.gridnine.platform.elsa.gradle.utils.BuildTextUtils;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import java.util.Set;

public class JavaWebAppElementsHelper {
    public static void generate(WebAppMetaRegistry registry, File destDir, File sourceDir, Set<File> generatedFiles) throws Exception {
        registry.getElements().values().forEach(element -> {
            BuildExceptionUtils.wrapException(() -> {
                var elm = WebAppMetadataHelper.toCustomEntity(element);
                var managedConfiguration = switch (element.getType()) {
                    case CONTAINER -> ((ContainerWebElementDescription) element).isManagedConfiguration();
                    case CUSTOM_CONTAINER -> ((CustomContainerWebElementDescription) element).isManagedConfiguration();
                    case TABLE -> ((TableWebElementDescription) element).isManagedConfiguration();
                    case ROUTER, NESTED_ROUTER -> false;
                    default -> true;
                };
                {
                    var fields = new ArrayList<Field>();
                    if (element instanceof WebElementWithChildren ctr) {
                        for (var entry : ctr.getChildren().entrySet()) {
                            var child = entry.getValue();
                            fields.add(new Field(entry.getKey(), StandardValueType.ENTITY, child.getClassName(), false, WebAppMetadataHelper.isManagedConfiguration(child) ? FieldType.MANAGED_CHILD : FieldType.CHILD, true));
                        }
                    }
                    for (var command : elm.getCommandsFromClient().values()) {
                        if(command.getProperties().isEmpty() && command.getCollections().isEmpty()){
                            fields.add(new Field("%sListener".formatted(command.getId()), StandardValueType.ENTITY, "com.gridnine.platform.elsa.common.core.model.common.RunnableWithExceptionAndArgument<com.gridnine.webpeer.core.ui.OperationUiContext>", false, FieldType.COMMAND_FROM_CLIENT, false));
                        } else {
                            fields.add(new Field("%sListener".formatted(command.getId()), StandardValueType.ENTITY, "com.gridnine.platform.elsa.common.core.model.common.RunnableWithExceptionAnd2Arguments<%s%sAction, com.gridnine.webpeer.core.ui.OperationUiContext>".formatted(element.getClassName(),BuildTextUtils.capitalize(command.getId())), false, FieldType.COMMAND_FROM_CLIENT, false));
                        }
                    }
                    for(var serv: elm.getServices().values()){
                        var requestClassName = "%s%sRequest".formatted(element.getClassName(), BuildTextUtils.capitalize(serv.getId()));
                        var responseClassName = "%s%sResponse".formatted(element.getClassName(), BuildTextUtils.capitalize(serv.getId()));
                        fields.add(new Field("%sServiceHandler".formatted(serv.getId()), StandardValueType.ENTITY, "com.gridnine.platform.elsa.webApp.WebAppServiceHandler<%s, %s>".formatted(requestClassName, responseClassName), false, FieldType.SERVICE, false));
                    }
                    for (var property : elm.getServerManagedState().getProperties().values()) {
                        fields.add(new Field(property.getId(), property.getType(), property.getClassName(), false, FieldType.STANDARD, property.isNonNullable()));
                    }

                    for (var coll : elm.getServerManagedState().getCollections().values()) {
                        fields.add(new Field(coll.getId(), coll.getElementType(), coll.getElementClassName(), true, FieldType.STANDARD, false));
                    }
                    if (elm.getInput() != null) {
                        if(elm.getInput().getValue().getProperties().size() + elm.getInput().getValue().getCollections().size() == 1){
                            if(elm.getInput().getValue().getProperties().size() == 1){
                                var id = WebAppMetadataHelper.getSimpleInputValueDescription(elm.getInput());
                                fields.add(new Field("value", id.prop().getType(), id.prop().getClassName(), false, FieldType.INPUT, false));
                                fields.add(new Field("valueChangeListener", StandardValueType.ENTITY, "com.gridnine.platform.elsa.webApp.WebAppValueChangeListener<%s>".formatted(id.valueClassName()), false, FieldType.INPUT_CHANGE_LISTENER, false));
                            } else {
                                var id = WebAppMetadataHelper.getSimpleInputValueDescription(elm.getInput());
                                fields.add(new Field("value", id.coll().getElementType(), id.coll().getElementClassName(), true, FieldType.INPUT, false));
                                fields.add(new Field("valueChangeListener", StandardValueType.ENTITY, "com.gridnine.platform.elsa.webApp.WebAppValueChangeListener<java.util.List<%s>>".formatted(id.valueClassName()), false, FieldType.INPUT_CHANGE_LISTENER, false));
                            }
                            fields.add(new Field("hasValueChangeListener", StandardValueType.BOOLEAN, null, false, FieldType.INPUT_HAS_CHANGE_LISTENER, true));
                        } else {
                            var valueClassName = "%sInputValue".formatted(elm.getClassName());
                            fields.add(new Field("value", StandardValueType.ENTITY, valueClassName, false, FieldType.INPUT, false));
                            fields.add(new Field("valueChangeListener", StandardValueType.ENTITY, "com.gridnine.platform.elsa.webApp.WebAppValueChangeListener<%s>".formatted(valueClassName), false, FieldType.INPUT_CHANGE_LISTENER, false));
                            fields.add(new Field("hasValueChangeListener", StandardValueType.BOOLEAN, null, false, FieldType.INPUT_HAS_CHANGE_LISTENER, true));
                        }
                    }
                    var className = elm.getClassName();
                    var simpleClassName = JavaCodeGeneratorUtils.getSimpleName(className);
                    var gen = new JavaCodeGenerator();
                    var packageName = JavaCodeGeneratorUtils.getPackage(className);
                    gen.setPackageName(packageName);
                    gen.addImport("com.gridnine.webpeer.core.ui.OperationUiContext");
                    var impl = "";
                    if(element.getType() == WebElementType.NESTED_ROUTER){
                        gen.addImport("com.gridnine.platform.elsa.webApp.NestedRouter");
                        impl = " implements NestedRouter";
                    }
                    gen.wrapWithBlock("public abstract class %sSkeleton extends BaseUiElement%s".formatted(simpleClassName, impl), () -> {
                        gen.blankLine();
                        gen.addImport("java.util.HashMap");
                        gen.addImport("java.util.Map");
                        gen.printLine("private final Map<String, Object> state = new HashMap<>();");
                        gen.addImport("com.gridnine.webpeer.core.ui.BaseUiElement");
                        for (var field : fields) {
                            switch (field.type) {
                                case INPUT, INPUT_CHANGE_LISTENER, COMMAND_FROM_CLIENT,SERVICE -> {
                                    if (field.collection) {
                                        gen.addImport("java.util.List");
                                        gen.addImport("java.util.ArrayList");
                                        gen.printLine("private List<%s> %s = new ArrayList<>();".formatted(JavaCodeGeneratorUtils.getPropertyType(field.valueType, field.className, false, gen), field.id));
                                    } else {
                                        gen.printLine("private %s %s;".formatted(JavaCodeGeneratorUtils.getPropertyType(field.valueType, field.className, field.nonNullable, gen), field.id));
                                    }
                                }

                                case MANAGED_CHILD, CHILD -> {
                                    gen.printLine("private final %s %s;".formatted(JavaCodeGeneratorUtils.getPropertyType(field.valueType, field.className, field.nonNullable, gen), field.id));
                                }
                            }
                        }
                        var cns = managedConfiguration ? "public %sSkeleton(String tag, %sConfiguration config, OperationUiContext ctx)".formatted(simpleClassName, simpleClassName) :
                                "public %sSkeleton(String tag, OperationUiContext ctx)".formatted(simpleClassName);
                        gen.blankLine();
                        gen.wrapWithBlock(cns, () -> {
                            gen.printLine("super(\"%s\", tag, ctx);".formatted(className));
                            if (!managedConfiguration) {
                                gen.printLine("var config = createConfiguration();");
                            }
                            var childrenCount = 0;
                            for (var field : fields) {
                                switch (field.type) {
                                    case CHILD -> {
                                        gen.printLine("%s = new %s(\"%s\", ctx);".formatted(field.id, JavaCodeGeneratorUtils.getSimpleName(field.className), field.id));
                                        gen.printLine("addChild(ctx, %s, %s);".formatted(field.id, childrenCount));
                                        childrenCount++;
                                    }
                                    case MANAGED_CHILD -> {
                                        gen.printLine("%s = new %s(\"%s\", config.get%s(), ctx);".formatted(field.id, JavaCodeGeneratorUtils.getSimpleName(field.className), field.id, BuildTextUtils.capitalize(field.id)));
                                        gen.printLine("addChild(ctx, %s, %s);".formatted(field.id, childrenCount));
                                        childrenCount++;
                                    }
                                    case INPUT_HAS_CHANGE_LISTENER -> {
                                        //noops
                                    }
                                    case COMMAND_FROM_CLIENT,SERVICE -> {
                                        gen.printLine("set%s(config.%s%s());".formatted(BuildTextUtils.capitalize(field.id), field.valueType == StandardValueType.BOOLEAN ? "is" : "get", BuildTextUtils.capitalize(field.id)));
                                    }
                                    default ->
                                            gen.printLine("set%s(config.%s%s(), ctx);".formatted(BuildTextUtils.capitalize(field.id), field.valueType == StandardValueType.BOOLEAN ? "is" : "get", BuildTextUtils.capitalize(field.id)));
                                }
                            }
                        });
                        for (var field : fields) {
                            if (field.type == FieldType.INPUT_HAS_CHANGE_LISTENER) {
                                continue;
                            }
                            if (field.collection) {
                                switch (field.type) {
                                    case INPUT -> {
                                        gen.blankLine();
                                        gen.addImport("java.util.List");
                                        gen.wrapWithBlock("public List<%s> get%s()".formatted(JavaCodeGeneratorUtils.getPropertyType(field.valueType, field.className, false, gen), BuildTextUtils.capitalize(field.id)), () -> {
                                            gen.printLine("return (List<%s>) state.get(\"%s\");".formatted(JavaCodeGeneratorUtils.getPropertyType(field.valueType, field.className, false, gen), field.id));
                                        });
                                        gen.blankLine();
                                        gen.wrapWithBlock("public void set%s(List<%s> value, OperationUiContext context)".formatted(BuildTextUtils.capitalize(field.id), JavaCodeGeneratorUtils.getPropertyType(field.valueType, field.className, false, gen)), () -> {
                                            gen.addImport("com.gridnine.platform.elsa.webApp.WebAppUtils");
                                            gen.wrapWithBlock("if(WebAppUtils.equals(value, this.%s) && isInitialized())".formatted(field.id), () -> {
                                                gen.printLine("return;");
                                            });
                                            gen.printLine("state.put(\"%s\", value);".formatted(field.id));
                                            gen.printLine("this.%s = value;".formatted(field.id));
                                            gen.wrapWithBlock("if(isInitialized())", () -> {
                                                gen.addImport("com.google.gson.JsonObject");
                                                gen.printLine("var data = new JsonObject();");
                                                gen.printLine("data.addProperty(\"pn\", \"%s\");".formatted(field.id));
                                                gen.addImport("com.gridnine.platform.elsa.webApp.WebAppUtils");
                                                gen.addImport("com.gridnine.platform.elsa.common.meta.common.StandardValueType");
                                                gen.printLine("data.add(\"pv\", WebAppUtils.toJsonValue(value, StandardValueType.%s));".formatted(field.valueType.name()));
                                                gen.printLine("sendCommand(context, \"pc\", data);");
                                            });
                                        });
                                    }
                                    default -> {
                                        gen.blankLine();
                                        gen.addImport("java.util.List");
                                        gen.wrapWithBlock("public List<%s> get%s()".formatted(JavaCodeGeneratorUtils.getPropertyType(field.valueType, field.className, field.nonNullable, gen), BuildTextUtils.capitalize(field.id)), () -> {
                                            gen.printLine("return (List<%s>) state.get(\"%s\");".formatted(JavaCodeGeneratorUtils.getPropertyType(field.valueType, field.className, field.nonNullable, gen), field.id));
                                        });
                                        gen.blankLine();
                                        gen.wrapWithBlock("public void set%s(List<%s> value, OperationUiContext context)".formatted(BuildTextUtils.capitalize(field.id), JavaCodeGeneratorUtils.getPropertyType(field.valueType, field.className, field.nonNullable, gen)), () -> {
                                            gen.addImport("com.gridnine.platform.elsa.webApp.WebAppUtils");
                                            gen.wrapWithBlock("if(WebAppUtils.equals(value, state.get(\"%s\")) && isInitialized())".formatted(field.id), () -> {
                                                gen.printLine("return;");
                                            });
                                            gen.printLine("state.put(\"%s\", value);".formatted(field.id));
                                            gen.wrapWithBlock("if(isInitialized())", () -> {
                                                gen.addImport("com.google.gson.JsonObject");
                                                gen.printLine("var data = new JsonObject();");
                                                gen.printLine("data.addProperty(\"pn\", \"%s\");".formatted(field.id));
                                                gen.addImport("com.gridnine.platform.elsa.webApp.WebAppUtils");
                                                gen.addImport("com.gridnine.platform.elsa.common.meta.common.StandardValueType");
                                                gen.printLine("data.add(\"pv\", WebAppUtils.toJsonValue(value, StandardValueType.%s));".formatted(field.valueType.name()));
                                                gen.printLine("sendCommand(context, \"pc\", data);");
                                            });
                                        });
                                    }
                                }

                            } else {

                                gen.blankLine();
                                switch (field.type) {
                                    case COMMAND_FROM_CLIENT, SERVICE -> {
                                        gen.blankLine();
                                        gen.wrapWithBlock("public %s %s%s()".formatted(JavaCodeGeneratorUtils.getPropertyType(field.valueType, field.className, field.nonNullable, gen), field.valueType == StandardValueType.BOOLEAN ? "is" : "get", BuildTextUtils.capitalize(field.id)), () -> {
                                            gen.printLine("return this.%s;".formatted(field.id));
                                        });
                                        gen.blankLine();
                                        gen.wrapWithBlock("public void set%s(%s value)".formatted(BuildTextUtils.capitalize(field.id), JavaCodeGeneratorUtils.getPropertyType(field.valueType, field.className, field.nonNullable, gen)), () -> {
                                            gen.printLine("this.%s = value;".formatted(field.id));
                                        });
                                    }
                                    case INPUT_CHANGE_LISTENER -> {
                                        gen.blankLine();
                                        gen.wrapWithBlock("public %s get%s()".formatted(JavaCodeGeneratorUtils.getPropertyType(field.valueType, field.className, field.nonNullable, gen), BuildTextUtils.capitalize(field.id)), () -> {
                                            gen.printLine("return %s;".formatted(field.id));
                                        });
                                        gen.blankLine();
                                        gen.wrapWithBlock("public void set%s(%s value, OperationUiContext context)".formatted(BuildTextUtils.capitalize(field.id), JavaCodeGeneratorUtils.getPropertyType(field.valueType, field.className, field.nonNullable, gen)), () -> {
                                            gen.printLine("this.%s = value;".formatted(field.id));
                                            gen.addImport("com.gridnine.platform.elsa.webApp.WebAppUtils");
                                            var propertyName = "has%s".formatted(BuildTextUtils.capitalize(field.id));
                                            gen.wrapWithBlock("if(WebAppUtils.equals(value != null, state.get(\"%s\")) && isInitialized())".formatted(propertyName), () -> {
                                                gen.printLine("return;");
                                            });
                                            gen.printLine("state.put(\"%s\", value != null);".formatted(propertyName));
                                            gen.wrapWithBlock("if(isInitialized())", () -> {
                                                gen.addImport("com.google.gson.JsonObject");
                                                gen.printLine("var data = new JsonObject();");
                                                gen.printLine("data.addProperty(\"pn\", \"%s\");".formatted(propertyName));
                                                gen.addImport("com.gridnine.platform.elsa.webApp.WebAppUtils");
                                                gen.addImport("com.gridnine.platform.elsa.common.meta.common.StandardValueType");
                                                gen.printLine("data.add(\"pv\", WebAppUtils.toJsonValue(value != null, StandardValueType.BOOLEAN));");
                                                gen.printLine("sendCommand(context, \"pc\", data);");
                                            });
                                        });
                                    }
                                    case CHILD, MANAGED_CHILD -> {
                                        gen.blankLine();
                                        gen.wrapWithBlock("public %s get%s()".formatted(JavaCodeGeneratorUtils.getPropertyType(field.valueType, field.className, field.nonNullable, gen), BuildTextUtils.capitalize(field.id)), () -> {
                                            gen.printLine("return this.%s;".formatted(field.id));
                                        });
                                    }
                                    default -> {
                                        gen.blankLine();
                                        gen.wrapWithBlock("public %s %s%s()".formatted(JavaCodeGeneratorUtils.getPropertyType(field.valueType, field.className, field.nonNullable, gen), field.valueType == StandardValueType.BOOLEAN ? "is" : "get", BuildTextUtils.capitalize(field.id)), () -> {
                                            gen.printLine("return (%s) state.get(\"%s\");".formatted(JavaCodeGeneratorUtils.getPropertyType(field.valueType, field.className, field.nonNullable, gen), field.id));
                                        });
                                        gen.blankLine();
                                        gen.wrapWithBlock("public void set%s(%s value, OperationUiContext context)".formatted(BuildTextUtils.capitalize(field.id), JavaCodeGeneratorUtils.getPropertyType(field.valueType, field.className, field.nonNullable, gen)), () -> {
                                            gen.addImport("com.gridnine.platform.elsa.webApp.WebAppUtils");
                                            gen.wrapWithBlock("if(WebAppUtils.equals(value, state.get(\"%s\")) && isInitialized())".formatted(field.id), () -> {
                                                gen.printLine("return;");
                                            });
                                            gen.printLine("state.put(\"%s\", value);".formatted(field.id));
                                            gen.wrapWithBlock("if(isInitialized())", () -> {
                                                gen.addImport("com.google.gson.JsonObject");
                                                gen.printLine("var data = new JsonObject();");
                                                gen.printLine("data.addProperty(\"pn\", \"%s\");".formatted(field.id));
                                                gen.addImport("com.gridnine.platform.elsa.webApp.WebAppUtils");
                                                gen.addImport("com.gridnine.platform.elsa.common.meta.common.StandardValueType");
                                                gen.printLine("data.add(\"pv\", WebAppUtils.toJsonValue(value, StandardValueType.%s));".formatted(field.valueType.name()));
                                                gen.printLine("sendCommand(context, \"pc\", data);");
                                            });
                                        });
                                    }
                                }
                            }
                        }
                        var inputs = fields.stream().filter(it -> it.type == FieldType.INPUT).toList();
                            gen.printLine("@Override");
                            gen.addImport("com.google.gson.JsonElement");
                            gen.wrapWithBlock("public void processCommand(OperationUiContext ctx, String commandId, JsonElement data)  throws Exception", () -> {
                                    gen.wrapWithBlock("if(commandId.equals(\"pc\"))", () -> {
                                        gen.printLine("var obj = data.getAsJsonObject();");
                                        gen.addImport("com.gridnine.webpeer.core.utils.WebPeerUtils");
                                        gen.printLine("var pn = WebPeerUtils.getString(obj, \"pn\");");
                                        gen.printLine("var pv = obj.get(\"pv\");");
                                        for (var input : inputs) {
                                            gen.wrapWithBlock("if(pn.equals(\"%s\"))".formatted(input.id), () -> {
                                                gen.addImport("com.gridnine.platform.elsa.common.meta.common.StandardValueType");
                                                gen.addImport("com.gridnine.platform.elsa.webApp.WebAppUtils");
                                                gen.printLine("var oldValue = this.%s;".formatted(input.id));
                                                if (input.collection) {
                                                    gen.printLine("var newValue = WebAppUtils.fromJsonArrayValue(pv, StandardValueType.%s, %s.class);".formatted(input.valueType, getPropertyType(input.valueType, input.className, gen)));
                                                } else {
                                                    gen.printLine("var newValue = WebAppUtils.fromJsonValue(pv, StandardValueType.%s, %s.class);".formatted(input.valueType, getPropertyType(input.valueType, input.className, gen)));
                                                }
                                                gen.printLine("this.%s = newValue;".formatted(input.id));
                                                gen.printLine("state.put(\"%s\", value);".formatted(input.id));
                                                gen.wrapWithBlock("if(this.%sChangeListener != null)".formatted(input.id), () -> {
                                                    gen.printLine("this.%sChangeListener.onValueChange(oldValue, newValue, ctx);".formatted(input.id));
                                                });
                                                gen.printLine("return;");
                                            });
                                        }
                                        for(var prop: elm.getServerManagedState().getProperties().values()){
                                            gen.wrapWithBlock("if(pn.equals(\"%s\"))".formatted(prop.getId()), () -> {
                                                gen.addImport("com.gridnine.platform.elsa.common.meta.common.StandardValueType");
                                                gen.addImport("com.gridnine.platform.elsa.webApp.WebAppUtils");
                                                gen.printLine("var value = WebAppUtils.fromJsonValue(pv, StandardValueType.%s, %s.class);".formatted(prop.getType(), getPropertyType(prop.getType(), prop.getClassName(), gen)));
                                                gen.printLine("state.put(\"%s\", value);".formatted(prop.getId()));
                                                gen.printLine("return;");
                                            });
                                        }
                                        for(var coll: elm.getServerManagedState().getCollections().values()){
                                            gen.wrapWithBlock("if(pn.equals(\"%s\"))".formatted(coll.getId()), () -> {
                                                gen.addImport("com.gridnine.platform.elsa.common.meta.common.StandardValueType");
                                                gen.addImport("com.gridnine.platform.elsa.webApp.WebAppUtils");
                                                gen.printLine("var value = WebAppUtils.fromJsonArrayValue(pv, StandardValueType.%s, %s.class);".formatted(coll.getElementType(), getPropertyType(coll.getElementType(), coll.getElementClassName(), gen)));
                                                gen.printLine("state.put(\"%s\", value);".formatted(coll.getId()));
                                                gen.printLine("return;");
                                            });
                                        }
                                    });


                                for (var action : elm.getCommandsFromClient().values()) {
                                        gen.wrapWithBlock("if(commandId.equals(\"%s\"))".formatted(action.getId()), () -> {
                                            if (action.getCollections().isEmpty() && action.getProperties().isEmpty()) {
                                                gen.printLine("this.%sListener.run(ctx);".formatted(action.getId()));
                                                gen.printLine("return;");
                                                return;
                                            }
                                            gen.addImport("com.gridnine.platform.elsa.webApp.WebAppUtils");
                                            gen.addImport("com.gridnine.platform.elsa.common.meta.common.StandardValueType");
                                            var actionClassName = getPropertyType(StandardValueType.ENTITY, "%s%sAction".formatted(className, BuildTextUtils.capitalize(action.getId())), gen);
                                            gen.printLine("var actionValue = WebAppUtils.fromJsonValue(data, StandardValueType.ENTITY, %s.class);".formatted(actionClassName));
                                            gen.printLine("this.%sListener.run(actionValue, ctx);".formatted(action.getId()));
                                            gen.printLine("return;");
                                            return;
                                        });
                                 }
                                gen.printLine("super.processCommand(ctx, commandId, data);");
                            });
                        for (var action : elm.getCommandsFromServer().values()) {
                            if(action.getProperties().isEmpty() && action.getCollections().isEmpty()) {
                                gen.wrapWithBlock("public void %s(OperationUiContext ctx, boolean postProcess)".formatted(action.getId()), ()->{
                                    gen.wrapWithBlock("if(postProcess)", ()->{
                                        gen.printLine("sendPostProcessCommand(ctx, \"%s\", null);".formatted(action.getId()));
                                        gen.printLine("return;");
                                    });
                                    gen.printLine("sendCommand(ctx, \"%s\", null);".formatted(action.getId()));
                                });
                            } else {
                                var cn = "%s%sAction".formatted(elm.getClassName(),BuildTextUtils.capitalize(action.getId()));
                                gen.addImport(cn);
                                gen.wrapWithBlock("public void %s(%s data, OperationUiContext ctx, boolean postProcess)".formatted(action.getId(), JavaCodeGeneratorUtils.getSimpleName(cn)), ()->{
                                    gen.addImport("com.gridnine.platform.elsa.webApp.WebAppUtils");
                                    gen.addImport("com.gridnine.platform.elsa.common.meta.common.StandardValueType");
                                    gen.printLine("var value = WebAppUtils.toJsonValue(data, StandardValueType.ENTITY);");
                                    gen.wrapWithBlock("if(postProcess)", ()->{
                                        gen.printLine("sendPostProcessCommand(ctx, \"%s\", value);".formatted(action.getId()));
                                        gen.printLine("return;");
                                    });
                                    gen.printLine("sendCommand(ctx, \"%s\", value);".formatted(action.getId()));
                                });
                            }
                        }
                        var services = fields.stream().filter(it -> it.type == FieldType.SERVICE).toList();
                        if(!services.isEmpty()){
                            gen.blankLine();
                            gen.printLine("@Override");
                            gen.addImport("com.google.gson.JsonElement");
                            gen.wrapWithBlock("public JsonElement doService(String commandId, JsonElement request, OperationUiContext context) throws Exception", () -> {
                               for(var serv: services){
                                   var servId = serv.id.replace("ServiceHandler", "");
                                   gen.wrapWithBlock("if(\"%s\".equals(commandId))".formatted(servId), ()->{
                                       gen.addImport("com.gridnine.platform.elsa.webApp.WebAppUtils");
                                       gen.addImport("com.gridnine.platform.elsa.common.meta.common.StandardValueType");
                                       var rqn = "%s%sRequest".formatted(element.getClassName(),BuildTextUtils.capitalize(servId));
                                       gen.addImport(rqn);
                                       gen.printLine("var req = WebAppUtils.fromJsonValue(request, StandardValueType.ENTITY, %s.class);".formatted(JavaCodeGeneratorUtils.getSimpleName(rqn)));
                                       gen.printLine("var resp = %s.doService(req, context);".formatted(serv.id));
                                       gen.printLine("return WebAppUtils.toJsonValue(resp, StandardValueType.ENTITY);");
                                   });
                               }
                               gen.printLine("return super.doService(commandId, request, context);");
                            });
                        }
                        gen.blankLine();
                        gen.printLine("@Override");
                        gen.addImport("com.google.gson.JsonObject");
                        gen.wrapWithBlock("public JsonObject buildState(OperationUiContext context)", () -> {
                            gen.printLine("var result =  super.buildState(context);");
                            for (var field : fields) {
                                switch (field.type) {
                                    case STANDARD, INPUT, INPUT_HAS_CHANGE_LISTENER -> {
                                        if (field.collection) {
                                            gen.wrapWithBlock("if(state.get(\"%s\") != null && !((List<?>)state.get(\"%s\")).isEmpty())".formatted(field.id, field.id), () -> {
                                                gen.addImport("com.google.gson.JsonArray");
                                                gen.printLine("var coll = new JsonArray();");
                                                gen.wrapWithBlock("for(var elm: (List<?>) state.get(\"%s\"))".formatted(field.id), () -> {
                                                    gen.addImport("com.gridnine.platform.elsa.webApp.WebAppUtils");
                                                    gen.addImport("com.gridnine.platform.elsa.common.meta.common.StandardValueType");
                                                    gen.printLine("coll.add(WebAppUtils.toJsonValue(elm, StandardValueType.%s));".formatted(field.valueType.name()));
                                                });
                                                gen.addImport("com.gridnine.platform.elsa.webApp.WebAppUtils");
                                                gen.addImport("com.gridnine.platform.elsa.common.meta.common.StandardValueType");
                                                gen.printLine("result.add(\"%s\", coll);".formatted(field.id));
                                            });
                                        } else {
                                            gen.wrapWithBlock("if(state.get(\"%s\") != null)".formatted(field.id), () -> {
                                                gen.addImport("com.gridnine.platform.elsa.webApp.WebAppUtils");
                                                gen.addImport("com.gridnine.platform.elsa.common.meta.common.StandardValueType");
                                                gen.printLine("result.add(\"%s\", WebAppUtils.toJsonValue(state.get(\"%s\"), StandardValueType.%s));".formatted(field.id, field.id, field.valueType.name()));
                                            });
                                        }
                                    }
                                    default -> {

                                    }
                                }

                            }
                            gen.printLine("return result;");
                        });
                        if (!managedConfiguration) {
                            gen.blankLine();
                            gen.printLine("protected abstract %sConfiguration createConfiguration();".formatted(simpleClassName));
                        }
                    });
                    var file = JavaCodeGeneratorUtils.saveIfDiffers(gen.toString(), className + "Skeleton.java", destDir);
                    generatedFiles.add(file);
                }
                {
                    var sourceFile = getSourceFile(sourceDir, element.getClassName());
                    if (!sourceFile.exists()) {
                        var className = elm.getClassName();
                        var simpleClassName = JavaCodeGeneratorUtils.getSimpleName(className);
                        var gen = new JavaCodeGenerator();
                        var packageName = JavaCodeGeneratorUtils.getPackage(className);
                        gen.setPackageName(packageName);
                        gen.addImport("com.gridnine.webpeer.core.ui.OperationUiContext");
                        gen.wrapWithBlock("public class %s extends %sSkeleton".formatted(simpleClassName, simpleClassName), () -> {
                            if (element.getType() == WebElementType.ROUTER || element.getType() == WebElementType.NESTED_ROUTER) {
                                gen.addImport("com.gridnine.platform.elsa.webApp.StandardParameters");
                                gen.addImport("org.springframework.beans.factory.ListableBeanFactory");
                                gen.addImport("com.gridnine.webpeer.core.ui.BaseUiElement");
                                gen.printLine("private String currentPath;");
                                gen.printLine("private final ListableBeanFactory factory;");
                            }
                            var cns = managedConfiguration ? "public %s(String tag, %sConfiguration config, OperationUiContext ctx)".formatted(simpleClassName, simpleClassName) :
                                    "public %s(String tag, OperationUiContext ctx)".formatted(simpleClassName);
                            gen.blankLine();
                            gen.wrapWithBlock(cns, () -> {
                                gen.printLine(managedConfiguration ? "super(tag, config, ctx);" : "super(tag, ctx);");
                                if(element.getType() == WebElementType.MODAL){
                                    gen.printLine("setCloseListener(this::removeChildren);");
                                }
                                if (element.getType() == WebElementType.ROUTER || element.getType() == WebElementType.NESTED_ROUTER) {
                                    gen.printLine("factory = ctx.getParameter(StandardParameters.BEAN_FACTORY);");
                                    if (element.getType() == WebElementType.ROUTER) {
                                        gen.printLine("currentPath = getPath();");
                                    } else {
                                        gen.printLine("var config = createConfiguration();");
                                        gen.printLine("currentPath = config.getPath();");
                                    }
                                    gen.printLine("ctx.setParameter(StandardParameters.ROUTER_PATH, currentPath);");
                                    if (element.getType() == WebElementType.ROUTER) {
                                        gen.printLine("var viewId = getViewId(getPath());");
                                    } else {
                                        gen.printLine("var viewId = getViewId(config.getPath());");
                                    }
                                    gen.printLine("var elm = createElement(viewId, ctx);");
                                    gen.printLine("addChild(ctx, elm, 0);");
                                }
                            });
                            if (element.getType() == WebElementType.ROUTER || element.getType() == WebElementType.NESTED_ROUTER) {
                                gen.addImport("com.google.gson.JsonElement");
                                gen.addImport("com.gridnine.webpeer.core.utils.WebPeerUtils");
                                gen.addImport("com.gridnine.platform.elsa.webApp.NestedRouter");
                                gen.addImport("java.util.List");
                                gen.addImport("java.util.ArrayList");
                                gen.addImport("com.gridnine.webpeer.core.ui.BaseUiElement");
                                gen.wrapWithBlock("private String getViewId(String path)", () -> {
                                    gen.printLine("throw new Exception(\"not implemented\");");
                                });

                                gen.wrapWithBlock("private BaseUiElement createElement(String viewId, OperationUiContext ctx)", () -> {
                                    gen.printLine("throw new Exception(\"not implemented\");");
                                });
                                if (element.getType() == WebElementType.ROUTER) {
                                    gen.printLine("@Override");
                                    gen.wrapWithBlock("public void processCommand(OperationUiContext ctx, String commandId, JsonElement data) throws Exception", () -> {
                                        gen.wrapWithBlock("if(\"navigate\".equals(commandId))", () -> {
                                            gen.printLine("var path = WebPeerUtils.getString(data.getAsJsonObject(), \"path\");");
                                            gen.printLine("var force = WebPeerUtils.getBoolean(data.getAsJsonObject(), \"force\", false);");
                                            gen.printLine("navigate(path, force, ctx);");
                                            gen.printLine("return;");
                                        });
                                        gen.printLine("super.processCommand(ctx, commandId, data);");
                                    });
                                }
                                gen.blankLine();
                                gen.wrapWithBlock("private void collectNestedRouters(List<NestedRouter> nestedRouters, BaseUiElement elm)", () -> {
                                    gen.wrapWithBlock("if(elm instanceof NestedRouter)", () -> {
                                        gen.printLine("nestedRouters.add((NestedRouter) elm);");
                                    });
                                    gen.printLine("elm.getUnmodifiableListOfChildren().forEach(child -> collectNestedRouters(nestedRouters, child));");
                                });
                                if(element.getType() == WebElementType.ROUTER) {
                                    gen.blankLine();
                                    gen.wrapWithBlock("private void confirm()", () -> {
                                        gen.printLine("throw new Exception(\"not implemented\")");
                                    });
                                }
                                gen.blankLine();
                                var navigate = element.getType() == WebElementType.NESTED_ROUTER? "public void navigate(String path, OperationUiContext ctx)": "public void navigate(String path, boolean force, OperationUiContext ctx)";
                                gen.wrapWithBlock(navigate, () -> {
                                    gen.printLine("ctx.setParameter(StandardParameters.ROUTER_PATH, path);");
                                    gen.wrapWithBlock("if(path.equals(currentPath))", () -> {
                                        gen.printLine("return;");
                                    });
                                    if (element.getType() == WebElementType.ROUTER) {
                                        gen.wrapWithBlock("if(Boolean.TRUE.equals(isHasChanges()) && !force)", () -> {
                                            gen.printLine("confirm();");
                                            gen.printLine("return;");
                                        });
                                    }
                                    if (element.getType() == WebElementType.ROUTER) {
                                        gen.printLine("setPath(path, ctx);");
                                        gen.printLine("setHasChanges(false, ctx);");
                                    }
                                    gen.printLine("var viewId = getViewId(path);");
                                    gen.printLine("var oldViewId = getViewId(currentPath);");
                                    gen.printLine("currentPath = path;");
                                    gen.wrapWithBlock("if(viewId.equals(oldViewId))", () -> {
                                        gen.printLine("var nestedRouters = new ArrayList<NestedRouter>();");
                                        gen.printLine("collectNestedRouters(nestedRouters, this);");
                                        gen.wrapWithBlock("for(var nestedRouter : nestedRouters)", () -> {
                                            gen.printLine("nestedRouter.navigate(path, ctx);");
                                        });
                                        gen.printLine("return;");
                                    });
                                    gen.printLine("var elm = getUnmodifiableListOfChildren().getFirst();");
                                    gen.printLine("removeChild(ctx, elm);");
                                    gen.printLine("elm = createElement(viewId, ctx);");
                                    gen.printLine("addChild(ctx, elm, 0);");
                                });
                                if (element.getType() == WebElementType.NESTED_ROUTER) {
                                    gen.blankLine();
                                    gen.printLine("@Override");
                                    gen.wrapWithBlock("protected %sConfiguration createConfiguration()".formatted(simpleClassName), () -> {
                                        gen.printLine("var result = new %sConfiguration();".formatted(simpleClassName));
                                        gen.addImport("com.gridnine.platform.elsa.webApp.StandardParameters");
                                        gen.printLine("var params = ctx.getParameter(OperationUiContext.PARAMS);");
                                        gen.printLine("var path = ctx.getParameter(StandardParameters.ROUTER_PATH);");
                                        gen.addImport("com.gridnine.webpeer.core.utils.WebPeerUtils");
                                        gen.printLine(" result.setPath(path == null? WebPeerUtils.getString(params, \"initPath\"): path);");
                                        gen.printLine("return result;");
                                    });
                                }
                            }
                            if(element.getType() == WebElementType.MODAL) {
                                gen.wrapWithBlock("public void closeDialog(OperationUiContext context)", ()->{
                                    gen.printLine("removeChildren(context);");
                                    gen.printLine("setVisible(false, context);");
                                });
                                gen.addImport("com.gridnine.platform.elsa.common.core.model.common.RunnableWithExceptionAndArgument");
                                gen.wrapWithBlock("public void showConfirm(String message, RunnableWithExceptionAndArgument<OperationUiContext> okCallback, OperationUiContext context)", ()->{
                                    gen.addImport("com.gridnine.platform.elsa.webApp.common.ConfirmMessageConfiguration");
                                    gen.printLine("var confirmMessageConfig = new ConfirmMessageConfiguration();");
                                    gen.printLine("confirmMessageConfig.setTitle(message);");
                                    gen.addImport("com.gridnine.platform.elsa.webApp.common.ConfirmMessage");
                                    gen.printLine("var confirmMessage = new ConfirmMessage(\"message\", confirmMessageConfig, context);");
                                    gen.addImport("com.gridnine.platform.elsa.webApp.common.DialogButton");
                                    gen.addImport("java.util.ArrayList");
                                    gen.printLine("var buttons = new ArrayList<DialogButton>();");
                                    gen.addImport("com.gridnine.platform.elsa.webApp.common.DialogButtonConfiguration");
                                    gen.wrapWithBlock("", ()->{
                                        gen.printLine("var buttonConfig = new DialogButtonConfiguration();");
                                        gen.printLine("buttonConfig.setTitle(\"OK\");");
                                        gen.wrapWithBlock("buttonConfig.setClickListener(ctx ->", ()->{
                                            gen.printLine("okCallback.run(ctx);");
                                            gen.printLine("closeDialog(ctx);");
                                        });
                                        gen.printLine(");");
                                        gen.printLine("var okButton = new DialogButton(\"ok\", buttonConfig, context);");
                                        gen.printLine("buttons.add(okButton);");
                                    });
                                    gen.wrapWithBlock("", ()->{
                                        gen.printLine("var buttonConfig = new DialogButtonConfiguration();");
                                        gen.printLine("buttonConfig.setTitle(\"Cancel\");");
                                        gen.wrapWithBlock("buttonConfig.setClickListener(ctx ->", ()->{
                                            gen.printLine("closeDialog(ctx);");
                                        });
                                        gen.printLine(");");
                                        gen.printLine("var cancelButton = new DialogButton(\"cancel\", buttonConfig, context);");
                                        gen.printLine("buttons.add(cancelButton);");
                                    });
                                    gen.printLine("showDialog(\"Confirm\", confirmMessage, buttons, context);");
                                });
                                gen.addImport("com.gridnine.platform.elsa.webApp.common.FlexDirection");
                                gen.addImport("com.gridnine.platform.elsa.webApp.WebAppUtils");
                                gen.wrapWithBlock("private void removeChildren(OperationUiContext context)", ()->{
                                    gen.printLine("var content = WebAppUtils.findChildByTag(this, \"content\");");
                                    gen.wrapWithBlock("if(content != null)", ()->{
                                        gen.printLine(" removeChild(context, content);");
                                    });
                                    gen.printLine("var buttons = WebAppUtils.findChildByTag(this, \"contebuttonsnt\");");
                                    gen.wrapWithBlock("if(buttons != null)", ()->{
                                        gen.printLine(" removeChild(context, buttons);");
                                    });
                                });
                                gen.addImport("java.util.List");
                                gen.addImport("com.gridnine.webpeer.core.ui.BaseUiElement");
                                gen.addImport("com.gridnine.platform.elsa.webApp.common.ContentWrapperConfiguration");
                                gen.addImport("com.gridnine.platform.elsa.webApp.common.ContentWrapper");
                                gen.wrapWithBlock("public void showDialog(String title, BaseUiElement content, List<DialogButton> buttons, OperationUiContext context)", ()->{
                                     gen.printLine("removeChildren(context);");
                                     gen.printLine("var config = new ContentWrapperConfiguration();");
                                     gen.printLine("config.setFlexDirection(FlexDirection.COLUMN);");
                                     gen.printLine("var contentWrapper = new ContentWrapper(\"content\", config, context);");
                                     gen.printLine("contentWrapper.addChild(context, content, 0);");
                                     gen.printLine("var buttonsWrapper = new ContentWrapper(\"buttons\", config, context);");
                                     gen.wrapWithBlock("for(var button : buttons)", ()->{
                                         gen.printLine("buttonsWrapper.addChild(context, button, 0);");
                                     });
                                     gen.printLine("setTitle(title, context);");
                                     gen.printLine("setVisible(true, context);");
                                     gen.printLine("addChild(context, contentWrapper,0);");
                                     gen.printLine("addChild(context, buttonsWrapper,0);");

                                });
                            }
                        });
                        if (!sourceFile.getParentFile().exists()) {
                            sourceFile.getParentFile().mkdirs();
                        }
                        Files.writeString(sourceFile.toPath(), gen.toString());
                    }
                }
            });

        });
    }

    private static File getSourceFile(File sourceDir, String className) {
        var parts = className.split("\\.");
        var currentFile = sourceDir;
        var length = parts.length;
        for (int n = 0; n < length - 1; n++) {
            currentFile = new File(currentFile, parts[n] + "/");
        }
        return new File(currentFile, parts[parts.length - 1] + ".java");
    }

    record Field(String id, StandardValueType valueType, String className, boolean collection, FieldType type,
                 boolean nonNullable) {

    }

    enum FieldType {
        STANDARD,
        INPUT,
        INPUT_CHANGE_LISTENER,
        INPUT_HAS_CHANGE_LISTENER,
        CHILD,
        MANAGED_CHILD,
        COMMAND_FROM_CLIENT,
        SERVICE
    }

    public static String getPropertyType(StandardValueType type, String className, JavaCodeGenerator gen) {
        return switch (type) {
            case STRING, CLASS -> "String";
            case LOCAL_DATE -> {
                gen.addImport(LocalDate.class.getName());
                yield LocalDate.class.getSimpleName();
            }
            case LOCAL_DATE_TIME -> {
                gen.addImport(LocalDateTime.class.getName());
                yield LocalDateTime.class.getName();
            }
            case INSTANT -> {
                gen.addImport(Instant.class.getName());
                yield Instant.class.getName();
            }
            case BOOLEAN -> "Boolean";
            case ENUM, ENTITY -> {
                gen.addImport(className);
                yield JavaCodeGeneratorUtils.getSimpleName(className);
            }
            case LONG -> "Long";
            case INT -> "Integer";
            case BIG_DECIMAL -> {
                gen.addImport(BigDecimal.class.getName());
                yield BigDecimal.class.getName();
            }
            default -> throw new IllegalStateException("Unknown property type: " + type);
        };
    }
}
