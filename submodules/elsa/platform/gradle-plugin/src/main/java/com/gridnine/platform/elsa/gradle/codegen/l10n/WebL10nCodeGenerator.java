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

package com.gridnine.platform.elsa.gradle.codegen.l10n;

import com.gridnine.platform.elsa.gradle.codegen.common.CodeGenerator;
import com.gridnine.platform.elsa.gradle.codegen.common.JavaCodeGeneratorUtils;
import com.gridnine.platform.elsa.gradle.codegen.common.TypeScriptCodeGenerator;
import com.gridnine.platform.elsa.gradle.codegen.common.WebCodeGeneratorUtils;
import com.gridnine.platform.elsa.gradle.meta.common.StandardValueType;
import com.gridnine.platform.elsa.gradle.meta.l10n.L10nMessageDescription;
import com.gridnine.platform.elsa.gradle.meta.l10n.L10nMessageParameterDescription;
import com.gridnine.platform.elsa.gradle.meta.l10n.L10nMessagesBundleDescription;
import com.gridnine.platform.elsa.gradle.meta.l10n.L10nMetaRegistry;
import com.gridnine.platform.elsa.gradle.meta.remoting.RemotingMetaRegistry;
import com.gridnine.platform.elsa.gradle.parser.l10n.L10nMetaRegistryParser;
import com.gridnine.platform.elsa.gradle.utils.BuildExceptionUtils;
import com.gridnine.platform.elsa.gradle.utils.BuildTextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class WebL10nCodeGenerator implements CodeGenerator<WebL10nCodeGenRecord> {
    @Override
    public void generate(WebL10nCodeGenRecord record, File destDir, Set<File> generatedFiles, Map<Object, Object> context) throws Exception {
        var parser = new L10nMetaRegistryParser();
        var reg = new L10nMetaRegistry();
        record.getSources().forEach(it -> BuildExceptionUtils.wrapException(() -> parser.updateMetaRegistry(reg, it)));
        var bundle = reg.getBundles().values().iterator().next();
        generateL10n(bundle, destDir, generatedFiles);
    }

    private void generateL10n(L10nMessagesBundleDescription bundle, File destDir, Set<File> generatedFiles) throws Exception {
        var gen = new TypeScriptCodeGenerator();
        var apiDir = new File(destDir, "api");
        if(!apiDir.exists()){
            apiDir.mkdirs();
        }
        var commonImports = new ArrayList<String>();
        commonImports.add("Configuration");
        for (L10nMessageDescription message : bundle.getMessages().values()) {
            for (L10nMessageParameterDescription param : message.getParameters().values()) {
                if (param.getType() == StandardValueType.ENTITY_REFERENCE && !commonImports.contains("EntityReference")) {
                    commonImports.add("EntityReference");
                }
            }
        }
        gen.printLine("/* eslint-disable camelcase */");
        gen.blankLine();
        gen.printLine("import { BaseL10nApi%s } from 'elsa-web-core';".formatted(commonImports.isEmpty() ? "" : ", " + BuildTextUtils.joinToString(commonImports, ", ")));
        gen.blankLine();
        var apiName = JavaCodeGeneratorUtils.toCamelCased("_%sL10nApi".formatted(bundle.getId()));
        gen.wrapWithBlock("export default class %s extends BaseL10nApi ".formatted(apiName), () -> {
            gen.wrapWithBlock("constructor(configuration:Configuration) ", ()->{
                gen.printLine("super(configuration, '%s');".formatted(bundle.getId()));
            });
           for (L10nMessageDescription message : bundle.getMessages().values()) {
                if (!message.getParameters().isEmpty()) {
                    gen.blankLine();
                    var sb1 = new StringBuilder();
                    var sb2 = new StringBuilder();
                    for (L10nMessageParameterDescription param : message.getParameters().values()) {
                        if (!sb2.isEmpty()) {
                            sb1.append(", ");
                            sb2.append(", ");
                        }
                        sb1.append("%s: %s".formatted(param.getId(), WebCodeGeneratorUtils.getType(param.getType(), new RemotingMetaRegistry(), param.getClassName())));
                        sb2.append(param.getId());
                    }
                    gen.wrapWithBlock("%s(%s) ".formatted(message.getId(), sb1), () -> gen.printLine("return this.getMessage('%s', %s);".formatted(message.getId(), sb2)));
                    continue;
                }
               gen.wrapWithBlock("%s() ".formatted(message.getId()), () -> gen.printLine("return this.getMessage('%s');".formatted(message.getId())));
            }
        });
        gen.blankLine();
        var file = JavaCodeGeneratorUtils.saveIfDiffers(gen.toString(), "%s.ts".formatted(apiName), apiDir);
        generatedFiles.add(file);
    }
}
