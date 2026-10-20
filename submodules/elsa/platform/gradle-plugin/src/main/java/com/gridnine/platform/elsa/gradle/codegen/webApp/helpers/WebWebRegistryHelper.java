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
import com.gridnine.platform.elsa.gradle.meta.webApp.WebAppMetaRegistry;
import com.gridnine.platform.elsa.gradle.utils.BuildExceptionUtils;

import java.io.File;
import java.util.Set;

public class WebWebRegistryHelper {
    public static void generate(WebAppMetaRegistry registry, File destDir, Set<File> generatedFiles, String configurator, String commonPackageName, String moduleName) throws Exception {
        var gen = new TypeScriptCodeGenerator();
        registry.getElements().values().forEach(element -> {
            BuildExceptionUtils.wrapException(() -> {
                var className = element.getClassName();
                var simpleClassName = JavaCodeGeneratorUtils.getSimpleName(className);
                gen.addImport("{registerFactory} from '%s/src/common/component'".formatted(moduleName));
                gen.addImport("{%sComponent} from '%s'".formatted(simpleClassName, WebCodeGeneratorUtils.getImportName(className, commonPackageName, moduleName).replace("/src-gen","/src")));
                gen.blankLine();
                gen.printLine("""
                        registerFactory('%s', {
                          createElement: (model: any) => new %sComponent(model)
                        });
                        """.formatted(className, simpleClassName));
            });
        });
        var file = WebCodeGeneratorUtils.getFile(configurator, destDir);
        WebCodeGeneratorUtils.saveIfDiffers(gen.toString(), file);
        generatedFiles.add(file);
    }
}


