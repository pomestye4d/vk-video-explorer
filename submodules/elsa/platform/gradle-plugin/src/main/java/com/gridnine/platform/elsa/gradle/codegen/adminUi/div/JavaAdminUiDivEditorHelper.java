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

package com.gridnine.platform.elsa.gradle.codegen.adminUi.div;

import com.gridnine.platform.elsa.gradle.codegen.adminUi.common.JavaAdminUiCodeGenUtils;
import com.gridnine.platform.elsa.gradle.codegen.common.JavaCodeGenerator;
import com.gridnine.platform.elsa.gradle.codegen.common.JavaCodeGeneratorUtils;
import com.gridnine.platform.elsa.gradle.meta.adminUi.div.NestedDivDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.div.RootDivDescription;
import com.gridnine.platform.elsa.gradle.meta.common.StandardValueType;
import com.gridnine.platform.elsa.gradle.utils.BuildExceptionUtils;
import com.gridnine.platform.elsa.gradle.utils.BuildTextUtils;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class JavaAdminUiDivEditorHelper {

    public static void generateEditor(RootDivDescription dd, File destDir, Set<File> generatedFiles) throws Exception {
        var gen = new JavaCodeGenerator();
        var packageName = JavaCodeGeneratorUtils.getPackage(dd.getClassName());
        gen.setPackageName(packageName);
        gen.addImport("com.gridnine.platform.elsa.admin.web.common.Div");
        var simpleClassName = JavaCodeGeneratorUtils.getSimpleName(dd.getClassName());
        var allComps = new LinkedHashMap<String, String>();
        for(var child: dd.getChildren()){
            JavaAdminUiCodeGenUtils.collectFields(child, allComps);
        }
        gen.wrapWithBlock("public class %s extends Div".formatted(simpleClassName), () -> {
            for (var entry : allComps.entrySet()) {
                gen.blankLine();
                String className = entry.getValue();
                var scn = JavaCodeGeneratorUtils.getPropertyType(StandardValueType.ENTITY, className, false, gen);
                gen.printLine("private final %s %s;".formatted(scn, entry.getKey()));
            }
            gen.blankLine();
            gen.addImport("com.gridnine.webpeer.core.ui.OperationUiContext");
            gen.wrapWithBlock("public %s(String tag, OperationUiContext context)".formatted(simpleClassName), () -> {
                gen.printLine("super(tag, context);");
                if(dd.getStyle() != null){
                    gen.printLine("setStyle(\"%s\", context);".formatted(dd.getStyle()));
                }
                for(var n =0; n < dd.getChildren().size();n++){
                    var containerName = "content%s".formatted(n);
                    JavaAdminUiCodeGenUtils.generateNestedComponent(dd.getChildren().get(n), containerName, gen);
                    gen.printLine("addChild(context, %s, %s);".formatted(containerName, n));
                }
            });
            allComps.forEach((fieldName, className) -> {
                gen.blankLine();
                var scn = JavaCodeGeneratorUtils.getPropertyType(StandardValueType.ENTITY, className, false, gen);
                gen.wrapWithBlock("public %s get%s()".formatted(scn, BuildTextUtils.capitalize(fieldName)), ()->{
                    gen.printLine("return this.%s;".formatted(fieldName));
                });
            });
        });

        var file = JavaCodeGeneratorUtils.saveIfDiffers(gen.toString(), dd.getClassName() + ".java", destDir);
        generatedFiles.add(file);
    }

    public static void collectFields(NestedDivDescription dd, Map<String, String> fields) {
        if(dd.getId() != null){
            fields.put(dd.getId(), "com.gridnine.platform.elsa.admin.web.common.Div");
        }
        dd.getChildren().forEach(r ->{
                BuildExceptionUtils.wrapException(()->JavaAdminUiCodeGenUtils.collectFields(r, fields));
        });
    }

    public static void generateNestedComponent(NestedDivDescription cd, String containerName, JavaCodeGenerator gen) {
        gen.addImport("com.gridnine.platform.elsa.admin.web.common.Div");
        gen.printLine("var %s = new Div(\"%s\", context);".formatted(containerName, cd.getId() == null? "div":cd.getId()));
        if(cd.getId() != null){
            gen.printLine("%s = %s;".formatted(cd.getId(), containerName));
        }
        if(cd.getStyle() != null){
            gen.printLine("%s.setStyle(\"%s\", context);".formatted(containerName, cd.getStyle()));
        }
        var childNumber = new AtomicInteger(-1);
        for (var child : cd.getChildren()) {
            gen.wrapWithBlock("",()->{
                var cn = "%s_content%s".formatted(containerName, childNumber.incrementAndGet());
                JavaAdminUiCodeGenUtils.generateNestedComponent(child, cn, gen);
                gen.printLine("%s.addChild(context, %s, %s);".formatted(containerName, cn, childNumber.get()));
            });
        }
    }
}
