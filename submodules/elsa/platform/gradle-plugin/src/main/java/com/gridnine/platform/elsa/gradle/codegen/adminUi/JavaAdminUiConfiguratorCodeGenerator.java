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

package com.gridnine.platform.elsa.gradle.codegen.adminUi;

import com.gridnine.platform.elsa.gradle.codegen.adminUi.common.JavaAdminUiCodeGenUtils;
import com.gridnine.platform.elsa.gradle.codegen.common.JavaCodeGenerator;
import com.gridnine.platform.elsa.gradle.codegen.common.JavaCodeGeneratorUtils;
import com.gridnine.platform.elsa.gradle.meta.adminUi.AdminUiMetaRegistry;
import com.gridnine.platform.elsa.gradle.meta.common.EntityDescription;
import com.gridnine.platform.elsa.gradle.meta.common.EnumDescription;

import java.io.File;
import java.util.Set;

public class JavaAdminUiConfiguratorCodeGenerator {
    public static void generate(AdminUiMetaRegistry registry, String configurator, File destDir, Set<File> generatedFiles) throws Exception {
        var gen = new JavaCodeGenerator();
        gen.setPackageName(JavaCodeGeneratorUtils.getPackage(configurator));
        gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.AdminUiMetaRegistryConfigurator");
        gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.AdminUiMetaRegistry");
        gen.wrapWithBlock("public class %s implements AdminUiMetaRegistryConfigurator".formatted(JavaCodeGeneratorUtils.getSimpleName(configurator)), () -> {
            gen.blankLine();
            gen.printLine("@Override");
            gen.wrapWithBlock("public void updateMetaRegistry(AdminUiMetaRegistry registry)", () -> {
                for (EnumDescription ed : registry.getEnums().values()) {
                    JavaCodeGeneratorUtils.generateJavaEnumConfiguratorCode(ed, gen);
                }
                for (EntityDescription ed : registry.getEntities().values()) {
                    JavaCodeGeneratorUtils.generateJavaEntityConfiguratorCode(ed, gen);
                }
                for (var cd : registry.getContainers().values()) {
                    JavaAdminUiCodeGenUtils.generateDescription(cd, "rootContainerDescription", 0, gen);
                }
            });

        });
        var file = JavaCodeGeneratorUtils.saveIfDiffers(gen.toString(), configurator + ".java", destDir);
        generatedFiles.add(file);
    }
}
