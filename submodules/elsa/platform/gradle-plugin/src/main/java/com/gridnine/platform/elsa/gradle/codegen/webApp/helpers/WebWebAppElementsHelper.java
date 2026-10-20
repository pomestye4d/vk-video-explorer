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

import com.gridnine.platform.elsa.gradle.codegen.common.JavaCodeGeneratorUtils;
import com.gridnine.platform.elsa.gradle.codegen.common.TypeScriptCodeGenerator;
import com.gridnine.platform.elsa.gradle.codegen.common.WebCodeGeneratorUtils;
import com.gridnine.platform.elsa.gradle.meta.common.StandardValueType;
import com.gridnine.platform.elsa.gradle.meta.webApp.*;
import com.gridnine.platform.elsa.gradle.parser.webApp.WebAppMetadataHelper;
import com.gridnine.platform.elsa.gradle.utils.BuildExceptionUtils;
import com.gridnine.platform.elsa.gradle.utils.BuildTextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;

public class WebWebAppElementsHelper {
    public static void generate(WebAppMetaRegistry registry, File destDir, File sourceDir, String commonPackageName, String moduleName, Set<File> generatedFiles) throws Exception {
        registry.getElements().values().forEach(element -> {
            BuildExceptionUtils.wrapException(() -> {
                var elm = WebAppMetadataHelper.toCustomEntity(element);
                {
                    var className = elm.getClassName();
                    var simpleClassName = JavaCodeGeneratorUtils.getSimpleName(className);
                    var gen = new TypeScriptCodeGenerator();
                    var stateStr = new StringBuilder();
                    var stateKeys = new ArrayList<String>();
                    stateKeys.addAll(elm.getServerManagedState().getProperties().keySet());
                    stateKeys.addAll(elm.getServerManagedState().getCollections().keySet());
                    if(elm.getInput() != null){
                      stateKeys.add("value");
                      stateKeys.add("hasValueChangeListener");
                    }
                    stateStr.append(BuildTextUtils.joinToString(stateKeys.stream().map("\"%s\""::formatted).toList(), ", "));
                    var ca = new StringBuilder();
                    ca.append(BuildTextUtils.joinToString(elm.getCommandsFromClient().values().stream().map(it -> "\"%s\"".formatted(it.getId())).toList(), ", "));
                    gen.addImport("{BaseReactUiElement} from '%s/src/common/component'".formatted(moduleName));

                    if(elm.getInput() != null){
                        if(elm.getInput().getValue().getProperties().size()+elm.getInput().getValue().getCollections().size() > 1){
                            var inputValueSimpleClassName = "%sInputValue".formatted(simpleClassName);
                            var inputValueImport = WebCodeGeneratorUtils.getImportName(className+"InputValue", commonPackageName, moduleName);
                            gen.addImport("{%s} from '%s'".formatted(inputValueSimpleClassName, inputValueImport));
                        } else {
                           var id = WebAppMetadataHelper.getSimpleInputValueDescription(elm.getInput());
                           if(id.collection()){
                               if(id.coll().getElementType() == StandardValueType.ENTITY){
                                   var inputValueClassName = id.coll().getElementClassName();
                                   var inputValueImport = WebCodeGeneratorUtils.getImportName(inputValueClassName, commonPackageName, moduleName);
                                   gen.addImport("{%s} from '%s'".formatted(JavaCodeGeneratorUtils.getSimpleName(inputValueClassName), inputValueImport));
                               }
                           } else {
                               if(id.prop().getType() == StandardValueType.ENTITY){
                                   var inputValueClassName = id.prop().getClassName();
                                   var inputValueImport = WebCodeGeneratorUtils.getImportName(inputValueClassName, commonPackageName, moduleName);
                                   gen.addImport("{%s} from '%s'".formatted(JavaCodeGeneratorUtils.getSimpleName(inputValueClassName), inputValueImport));
                               }
                           }
                        }
                    }
                    var rq = BuildTextUtils.joinToString(elm.getServices().values().stream().map(it -> "\"%s\"".formatted(it.getId())).toList(), ", ");
                    gen.wrapWithBlock("export abstract class %sSkeleton extends BaseReactUiElement".formatted(simpleClassName), () -> {
                        gen.wrapWithBlock("constructor(model: any)", () -> {
                            gen.printLine("""
                                    super({
                                       state: [%s],
                                       actionsFromClient: [%s],
                                       input: %s,
                                       services: [%s]
                                    }, model);
                                    """.formatted(stateStr, ca, elm.getInput() == null? "undefined": "'%s'".formatted(elm.getInput().getType().name()), rq));
                            for (var prop : elm.getServerManagedState().getProperties().values()) {
                                gen.printLine("this.state.set('%s', model.%s);".formatted(prop.getId(), prop.getId()));
                            }
                            for (var coll : elm.getServerManagedState().getCollections().values()) {
                                gen.printLine("this.state.set('%s', model.%s??[]);".formatted(coll.getId(), coll.getId()));
                            }
                            if(elm.getInput() != null){
                                gen.printLine("this.state.set('value', model.value);");
                                gen.printLine("this.state.set('hasValueChangeListener', model.hasValueChangeListener);");
                            }
                        });
                        gen.blankLine();
                        for (var prop : elm.getServerManagedState().getProperties().values()) {
                            gen.wrapWithBlock("%s%s()".formatted(prop.getType() == StandardValueType.BOOLEAN && prop.isNonNullable()? "is": "get",BuildTextUtils.capitalize(prop.getId())), () -> {
                                gen.printLine("return this?.state?.get('%s') as %s%s".formatted(prop.getId(),
                                        getType(prop.getType(), registry, prop.getClassName(), gen, commonPackageName, moduleName),
                                        prop.isNonNullable() ? "" : " | undefined"));
                            });
                        }
                        for (var coll : elm.getServerManagedState().getCollections().values()) {
                            gen.wrapWithBlock("get%s()".formatted(BuildTextUtils.capitalize(coll.getId())), () -> {
                                gen.printLine("return this?.state?.get('%s') as %s[] || [] ".formatted(coll.getId(),
                                        getType(coll.getElementType(), registry, coll.getElementClassName(), gen, commonPackageName, moduleName)));
                            });
                        }
                        if(!elm.getCommandsFromServer().isEmpty()){
                            gen.wrapWithBlock("processCommandFromServer(commandId: string, data?: any)", ()->{
                               for(var command : elm.getCommandsFromServer().values()){
                                   gen.wrapWithBlock("if (commandId === '%s')".formatted(command.getId()), ()->{
                                       if(command.getProperties().isEmpty() && command.getCollections().isEmpty()){
                                           gen.printLine("this.process%s();".formatted(BuildTextUtils.capitalize(command.getId())));
                                       } else {
                                           gen.printLine("this.process%s(data);".formatted(BuildTextUtils.capitalize(command.getId())));
                                       }
                                       gen.printLine("return;");
                                   });
                               }
                               gen.printLine("super.processCommandFromServer(commandId, data);");
                            });
                        }
                        if(elm.getInput() != null){
                            if(elm.getInput().getValue().getProperties().size()+elm.getInput().getValue().getCollections().size() > 1){
                                var inputValueSimpleClassName = "%sInputValue".formatted(simpleClassName);
                                gen.wrapWithBlock("getValue()", () -> {
                                    gen.printLine("return this?.state?.get('value') as %s".formatted(inputValueSimpleClassName));
                                });
                                gen.wrapWithBlock("setValue(value?: %s)".formatted(inputValueSimpleClassName), () -> {
                                    gen.printLine("this.stateSetters.get('value')!(value)");
                                    gen.printLine("""
                                        this.sendCommand('pc', {
                                           pn : 'value',
                                           pv: value
                                        }, !this?.state?.get('hasValueChangeListener') && !!this?.state?.get('deferred'));""");
                                });
                            } else {
                                var id = WebAppMetadataHelper.getSimpleInputValueDescription(elm.getInput());
                                var cn1 = JavaCodeGeneratorUtils.getSimpleName(id.valueClassName());
                                if(id.prop() != null) {
                                    if ("Boolean".equals(cn1)) {
                                        cn1 = "boolean";
                                    }else if ("String".equals(cn1)) {
                                        cn1 = "string";
                                    }else if ("BigDecimal".equals(cn1)) {
                                        cn1 = "number";
                                    }else if ("Integer".equals(cn1)) {
                                        cn1 = "number";
                                    } else if ("LocalDate".equals(cn1)) {
                                        cn1 = "string";
                                    }
                                }
                                var cn = cn1;
                                gen.wrapWithBlock("getValue()", () -> {
                                    gen.printLine("return this?.state?.get('value') as %s%s".formatted(cn, id.collection()? "[]":""));
                                });
                                gen.wrapWithBlock("setValue(value?: %s%s)".formatted(cn, id.collection()? "[]":""), () -> {
                                    gen.printLine("this.stateSetters.get('value')!(value)");
                                    gen.printLine("""
                                        this.sendCommand('pc', {
                                           pn : 'value',
                                           pv: value
                                        }, !this.state.get('hasValueChangeListener') && !!this.state.get('deferred'));""");
                                });
                            }
                        }
                        for(var action: elm.getCommandsFromClient().values()){
                            if(action.getCollections().isEmpty() && action.getProperties().isEmpty()){
                                gen.wrapWithBlock("async send%s()".formatted(BuildTextUtils.capitalize(action.getId())), ()->{
                                    gen.printLine("await this.sendCommand('%s');".formatted(action.getId()));
                                });
                            } else {
                                var actionClassName = "%s%sAction".formatted(elm.getClassName(),BuildTextUtils.capitalize(action.getId()));
                                gen.wrapWithBlock("async send%s(value: %s)".formatted(BuildTextUtils.capitalize(action.getId()), getType(StandardValueType.ENTITY, registry, actionClassName, gen, commonPackageName, moduleName)), ()->{
                                    gen.printLine("await this.sendCommand('%s', value);".formatted(action.getId()));
                                });
                            }
                        }
                        for(var serv: elm.getServices().values()){
                            var requestClassName = "%s%sRequest".formatted(elm.getClassName(),BuildTextUtils.capitalize(serv.getId()));
                            var responseClassName = "%s%sResponse".formatted(elm.getClassName(),BuildTextUtils.capitalize(serv.getId()));
                            gen.wrapWithBlock("async do%s(value: %s)".formatted(BuildTextUtils.capitalize(serv.getId()), getType(StandardValueType.ENTITY, registry, requestClassName, gen, commonPackageName, moduleName)), ()->{
                                gen.printLine("return (await this.makeRequest('%s', value)) as %s;".formatted(serv.getId(), getType(StandardValueType.ENTITY, registry, responseClassName, gen, commonPackageName, moduleName)));
                            });
                        }
                        for(var command : elm.getCommandsFromServer().values()){
                            if(command.getProperties().isEmpty() && command.getCollections().isEmpty()){
                                gen.printLine("abstract process%s(): void;".formatted(BuildTextUtils.capitalize(command.getId())));
                            } else {
                                var cn = "%s%sAction".formatted(elm.getClassName(),BuildTextUtils.capitalize(command.getId()));
                                var type = getType(StandardValueType.ENTITY, null, cn, gen, commonPackageName, moduleName);
                                gen.printLine("abstract process%s(value: %s): void;".formatted(BuildTextUtils.capitalize(command.getId()), type));
                            }
                        }

                    });

                    var file = WebCodeGeneratorUtils.saveIfDiffers(gen.toString(), WebCodeGeneratorUtils.getFile(className + "Skeleton.tsx", destDir));
                    generatedFiles.add(file);
                }
                switch (element.getType()) {
                    case CONTAINER -> WebContainerHelper.generateContainer((ContainerWebElementDescription) element, sourceDir, commonPackageName, moduleName);
                    case CUSTOM_CONTAINER -> WebCustomContainerHelper.generateContainer((CustomContainerWebElementDescription) element, sourceDir, commonPackageName, moduleName);
                    case BUTTON -> WebButtonHelper.generateButton((ButtonWebElementDescription) element, sourceDir, commonPackageName, moduleName);
                    case SELECT -> WebSelectHelper.generateSelect((SelectWebElementDescription) element, sourceDir, commonPackageName, moduleName);
                    case ROUTER -> WebRouterHelper.generateRouter((RouterWebElementDescription)  element, sourceDir, commonPackageName, moduleName);
                    case CUSTOM -> WebCustomHelper.generateCustom((CustomWebElementDescription) element, sourceDir, commonPackageName, moduleName);
                    case TEXT_AREA ->  WebTextAreaHelper.generateTextArea((TextAreaWebElementDescription) element, sourceDir, commonPackageName, moduleName);
                    case TEXT_FIELD ->  WebTextFieldHelper.generateTextField((TextFieldWebElementDescription) element, sourceDir, commonPackageName, moduleName);
                    case NESTED_ROUTER -> WebNestedRouterHelper.generateNestedRouter((NestedRouterWebElementDescription) element, sourceDir, commonPackageName, moduleName);
                    case TABLE -> WebTableHelper.generateTable((TableWebElementDescription) element, sourceDir, commonPackageName, moduleName);
                    case AUTOCOMPLETE -> WebAutocompleteHelper.generateAutocomplete((AutocompleteWebElementDescription) element, sourceDir, commonPackageName, moduleName);
                    case LABEL -> WebLabelHelper.generateLabel((LabelWebElementDescription) element, sourceDir, commonPackageName, moduleName);
                    case MODAL ->  WebModalHelper.generateModal((ModalWebElementDescription) element, sourceDir, commonPackageName, moduleName);
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

    private static String getType(StandardValueType vt, WebAppMetaRegistry metaRegistry, String className, TypeScriptCodeGenerator gen, String commonPackageName, String moduleName) throws Exception {
        return switch (vt) {
            case LONG, INT, BIG_DECIMAL -> "number";
            case UUID, STRING, CLASS, LOCAL_DATE, INSTANT, LOCAL_DATE_TIME -> "string";
            case ENUM,ENTITY -> {
                var simpleName = JavaCodeGeneratorUtils.getSimpleName(className);
                if("Object".equals(simpleName)){
                    yield "any";
                }
                if("JsonObject".equals(simpleName)){
                    yield "any";
                }
                var importName = WebCodeGeneratorUtils.getImportName(className, commonPackageName, moduleName);
                gen.addImport("{%s} from '%s'".formatted(simpleName, importName));
                yield  simpleName;
            }
            case BOOLEAN -> "boolean";
            default -> throw new Exception("Unsupported type: " + vt);

        };
    }
}


