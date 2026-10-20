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
import com.gridnine.platform.elsa.gradle.codegen.common.TypeScriptCodeGenerator;
import com.gridnine.platform.elsa.gradle.codegen.common.WebCodeGeneratorUtils;
import com.gridnine.platform.elsa.gradle.meta.remoting.RemotingMetaRegistry;
import com.gridnine.platform.elsa.gradle.parser.remoting.RemotingMetaRegistryParser;
import com.gridnine.platform.elsa.gradle.utils.BuildExceptionUtils;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WebRemotingCodeGenerator implements CodeGenerator<WebRemotingCodeGenRecord> {
    @Override
    public void generate(WebRemotingCodeGenRecord record, File destDir, Set<File> generatedFiles, Map<Object, Object> context) {
            var parser = new RemotingMetaRegistryParser();
            var metaRegistry = new RemotingMetaRegistry();
            parser.updateMetaRegistry(metaRegistry, record.getSources());
            BuildExceptionUtils.wrapException(() -> {
                var modelsDir = new File(destDir, "models");
                modelsDir.mkdirs();
                var apisDir = new File(destDir, "api");
                apisDir.mkdirs();
                for (var en : metaRegistry.getEnums().values()) {
                    var gen = new TypeScriptCodeGenerator();
                    WebCodeGeneratorUtils.generateWebEnumCode(en, gen);
                    var file = JavaCodeGeneratorUtils.saveIfDiffers(gen.toString(), "%s.ts".formatted(JavaCodeGeneratorUtils.getSimpleName(en.getId())), modelsDir);
                    generatedFiles.add(file);
                }
                for (var ett : metaRegistry.getEntities().values()) {
                    var gen = new TypeScriptCodeGenerator();
                    WebCodeGeneratorUtils.generateWebEntityCode(ett, metaRegistry, gen, record.getCommonPackageName(), record.getModuleName());
                    var file = JavaCodeGeneratorUtils.saveIfDiffers(gen.toString(), "%s.ts".formatted(JavaCodeGeneratorUtils.getSimpleName(ett.getId())), modelsDir);
                    generatedFiles.add(file);
                }
                for(var remoting: metaRegistry.getRemotings().values()){
                    for(var group: remoting.getGroups().values()){
                        var gen = new TypeScriptCodeGenerator();
                        var imported = new HashSet<String>();
                        gen.printLine("import { BaseAPI, Configuration } from 'elsa-web-core';");
                        for(var service: group.getServices().values()){

                            if(service.getRequestClassName() != null && !imported.contains(service.getRequestClassName())){
                                if("com.gridnine.platform.elsa.core.remoting.standard.BinaryData".equals(service.getRequestClassName())){
                                    gen.printLine("import { BinaryData }  from 'elsa-web-core';");
                                } else  {
                                    gen.printLine("import { %s } from '../models/%s';".formatted(
                                            JavaCodeGeneratorUtils.getSimpleName(service.getRequestClassName()),
                                            JavaCodeGeneratorUtils.getSimpleName(service.getRequestClassName())));
                                }
                               imported.add(service.getRequestClassName());
                            }
                            if(service.getResponseClassName() != null && !imported.contains(service.getResponseClassName())){
                                if("com.gridnine.platform.elsa.core.remoting.standard.BinaryData".equals(service.getResponseClassName())){
                                    gen.printLine("import { BinaryData }  from 'elsa-web-core';");
                                } else  {
                                    gen.printLine("import { %s } from '../models/%s';".formatted(
                                            JavaCodeGeneratorUtils.getSimpleName(service.getResponseClassName()),
                                            JavaCodeGeneratorUtils.getSimpleName(service.getResponseClassName())));
                                }
                                imported.add(service.getResponseClassName());
                            }
                        }
                        gen.blankLine();
                        var apiName = JavaCodeGeneratorUtils.toCamelCased("_%s-%s-api".formatted(remoting.getId(), group.getId()));
                        gen.wrapWithBlock("export default class %s extends BaseAPI ".formatted(apiName), ()->{
                            gen.wrapWithBlock("constructor(configuration:Configuration) ", ()->{
                                gen.printLine("super(configuration, { remotingId: '%s', groupId: '%s' });".formatted(remoting.getId(), group.getId()));
                            });
                            for(var service: group.getServices().values()){
                                gen.blankLine();
                                var arg = service.getRequestClassName() == null? "": "request: %s".formatted(JavaCodeGeneratorUtils.getSimpleName(service.getRequestClassName()));
                                var result = service.getResponseClassName() == null? "void": JavaCodeGeneratorUtils.getSimpleName(service.getResponseClassName());
                                gen.wrapWithBlock("async %s(%s) ".formatted(JavaCodeGeneratorUtils.toCamelCased(service.getId()), arg), ()->{

                                    gen.printLine("return (await this.request({ request%s, serviceId: '%s' })).response as %s;".formatted(
                                            service.getRequestClassName() == null? ": null": "",
                                            service.getId(),
                                            result
                                    ));
                                });
                            }
                        });
                        gen.blankLine();
                        var file = JavaCodeGeneratorUtils.saveIfDiffers(gen.toString(), "%s.ts".formatted(apiName), apisDir);
                        generatedFiles.add(file);
                    }
                }
        });
    }
}
