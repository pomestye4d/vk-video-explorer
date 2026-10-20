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
import com.gridnine.platform.elsa.gradle.codegen.common.JavaCodeGenerator;
import com.gridnine.platform.elsa.gradle.codegen.common.JavaCodeGeneratorUtils;
import com.gridnine.platform.elsa.gradle.meta.l10n.L10nMessageDescription;
import com.gridnine.platform.elsa.gradle.meta.l10n.L10nMessageParameterDescription;
import com.gridnine.platform.elsa.gradle.meta.l10n.L10nMessagesBundleDescription;
import com.gridnine.platform.elsa.gradle.meta.l10n.L10nMetaRegistry;
import com.gridnine.platform.elsa.gradle.parser.l10n.L10nMetaRegistryParser;
import com.gridnine.platform.elsa.gradle.utils.BuildExceptionUtils;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class JavaL10nCodeGenerator implements CodeGenerator<JavaL10nCodeGenRecord> {
    @Override
    public void generate(JavaL10nCodeGenRecord record, File destDir, Set<File> generatedFiles, Map<Object, Object> context) throws Exception {
        var parser = new L10nMetaRegistryParser();
        var reg = new L10nMetaRegistry();
        record.getSources().forEach(it -> BuildExceptionUtils.wrapException(() -> parser.updateMetaRegistry(reg, it)));
        if (!reg.getBundles().isEmpty()) {
            var bundle = reg.getBundles().values().iterator().next();
            generateConfigurator(record.getRegistryConfigurator(), bundle, destDir, generatedFiles);
            if (record.getFactory() != null) {
                generateFactory(record.getFactory(), bundle, destDir, generatedFiles);
            }
        }
    }

    private void generateConfigurator(String configurator, L10nMessagesBundleDescription bundle, File destDir, Set<File> generatedFiles) throws Exception {
        var gen = new JavaCodeGenerator();
        gen.setPackageName(JavaCodeGeneratorUtils.getPackage(configurator));
        gen.addImport("com.gridnine.platform.elsa.common.meta.l10n.L10nMetaRegistryConfigurator");
        gen.addImport("com.gridnine.platform.elsa.common.meta.l10n.L10nMetaRegistry");
        gen.wrapWithBlock("public class %s implements L10nMetaRegistryConfigurator".formatted(JavaCodeGeneratorUtils.getSimpleName(configurator)), () -> {
            gen.blankLine();
            gen.printLine("@Override");
            gen.wrapWithBlock("public void updateMetaRegistry(L10nMetaRegistry registry)", () -> {
                gen.addImport("com.gridnine.platform.elsa.common.meta.l10n.L10nMessagesBundleDescription");
                gen.wrapWithBlock(null, () -> {
                    gen.printLine("var bundleDescription = registry.getBundles().computeIfAbsent(\"%s\", (id) -> new L10nMessagesBundleDescription(id));".formatted(bundle.getId()));
                    for (L10nMessageDescription md : bundle.getMessages().values()) {
                        gen.addImport("com.gridnine.platform.elsa.common.meta.l10n.L10nMessageDescription");
                        gen.wrapWithBlock(null, () -> {
                            gen.printLine("var messageDescription = new L10nMessageDescription(\"%s\");".formatted(md.getId()));
                            for (Map.Entry<String, L10nMessageParameterDescription> entry : md.getParameters().entrySet()) {
                                gen.wrapWithBlock(null, () -> {
                                    gen.addImport("com.gridnine.platform.elsa.common.meta.l10n.L10nMessageParameterDescription");
                                    gen.addImport("com.gridnine.platform.elsa.common.meta.common.StandardValueType");
                                    gen.printLine("var paramDescription = new L10nMessageParameterDescription(\"%s\");".formatted(entry.getKey()));
                                    var param = entry.getValue();
                                    gen.printLine("paramDescription.setType(StandardValueType.%s);".formatted(param.getType().name()));
                                    if (param.isCollection()) {
                                        gen.printLine("paramDescription.setCollection(true);");
                                    }
                                    if (param.getClassName() != null) {
                                        gen.printLine("paramDescription.setClassName(\"%s\");".formatted(param.getClassName()));
                                    }
                                    gen.printLine("messageDescription.getParameters().put(paramDescription.getId(), paramDescription);");
                                });
                            }
                            for (Map.Entry<Locale, String> entry : md.getDisplayNames().entrySet()) {
                                gen.addImport("com.gridnine.platform.elsa.common.core.utils.LocaleUtils");
                                gen.printLine("messageDescription.getDisplayNames().put(LocaleUtils.getLocale(\"%s\",\"%s\"), \"%s\");".formatted(entry.getKey().getLanguage(), entry.getKey().getCountry(), entry.getValue()));
                            }
                            gen.printLine("bundleDescription.getMessages().put(messageDescription.getId(), messageDescription);");
                        });
                    }
                });
            });

        });
        var file = JavaCodeGeneratorUtils.saveIfDiffers(gen.toString(), configurator + ".java", destDir);
        generatedFiles.add(file);
    }

    private void generateFactory(String factory, L10nMessagesBundleDescription bundle, File destDir, Set<File> generatedFiles) throws Exception {
        var gen = new JavaCodeGenerator();
        gen.setPackageName(JavaCodeGeneratorUtils.getPackage(factory));
        gen.addImport("com.gridnine.platform.elsa.common.core.l10n.Localizer");
        gen.addImport("com.gridnine.platform.elsa.common.core.model.common.L10nMessage");
        gen.addImport("org.springframework.beans.factory.annotation.Autowired");
        gen.wrapWithBlock("public class %s".formatted(JavaCodeGeneratorUtils.getSimpleName(factory)), () -> {
            gen.blankLine();
            gen.printLine("@Autowired");
            gen.printLine("private Localizer localizer;");

            for (L10nMessageDescription message : bundle.getMessages().values()) {
                gen.blankLine();
                var parameters = new StringBuilder();
                var arguments = new StringBuilder();
                for (L10nMessageParameterDescription param : message.getParameters().values()) {
                    if (!parameters.isEmpty()) {
                        parameters.append(", ");
                        arguments.append(", ");
                    }
                    if (param.isCollection()) {
                        gen.addImport("java.util.List");
                        parameters.append("List<%s> %s".formatted(JavaCodeGeneratorUtils.getPropertyType(param.getType(), param.getClassName(), true, gen), param.getId()));
                    } else {
                        parameters.append("%s %s".formatted(JavaCodeGeneratorUtils.getPropertyType(param.getType(), param.getClassName(), true, gen), param.getId()));
                    }
                    arguments.append(param.getId());
                }
                gen.wrapWithBlock("public String %s(%s)".formatted(message.getId(), parameters), () -> {
                    if (!arguments.isEmpty()) {
                        gen.printLine("return localizer.toString(\"%s\", \"%s\", null, %s);".formatted(bundle.getId(), message.getId(), arguments));
                    } else {
                        gen.printLine("return localizer.toString(\"%s\", \"%s\", null);".formatted(bundle.getId(), message.getId()));
                    }
                });
                gen.blankLine();
                gen.wrapWithBlock("public static L10nMessage %sMessage(%s)".formatted(message.getId(), parameters), () -> {
                    if (!arguments.isEmpty()) {
                        gen.printLine("return new L10nMessage(\"%s\", \"%s\", %s);".formatted(bundle.getId(), message.getId(), arguments));
                    } else {
                        gen.printLine("return new L10nMessage(\"%s\", \"%s\");".formatted(bundle.getId(), message.getId()));
                    }
                });
            }
        });
        var file = JavaCodeGeneratorUtils.saveIfDiffers(gen.toString(), factory + ".java", destDir);
        generatedFiles.add(file);
    }
}
