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

package com.gridnine.platform.elsa.gradle.codegen.adminUi.tabs;

import com.gridnine.platform.elsa.gradle.codegen.adminUi.common.JavaAdminUiCodeGenUtils;
import com.gridnine.platform.elsa.gradle.codegen.common.JavaCodeGenerator;
import com.gridnine.platform.elsa.gradle.codegen.common.JavaCodeGeneratorUtils;
import com.gridnine.platform.elsa.gradle.meta.adminUi.tabs.NestedTabsDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.tabs.RootTabsDescription;
import com.gridnine.platform.elsa.gradle.meta.common.StandardValueType;
import com.gridnine.platform.elsa.gradle.utils.BuildExceptionUtils;
import com.gridnine.platform.elsa.gradle.utils.BuildTextUtils;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class JavaAdminUiTabsEditorHelper {

    public static void generateEditor(RootTabsDescription fd, File destDir, Set<File> generatedFiles) throws Exception {
        var gen = new JavaCodeGenerator();
        var packageName = JavaCodeGeneratorUtils.getPackage(fd.getClassName());
        gen.setPackageName(packageName);
        gen.addImport("com.gridnine.platform.elsa.admin.web.tabs.TabsContainer");
        var simpleClassName = JavaCodeGeneratorUtils.getSimpleName(fd.getClassName());
        var fields = new LinkedHashMap<String, String>();
        JavaAdminUiCodeGenUtils.collectFields(fd, fields);
        var tabChildCount = new AtomicInteger(-1);
        gen.wrapWithBlock("public class %s extends TabsContainer".formatted(simpleClassName), () -> {
            fields.forEach((fieldName, className) -> {
                gen.blankLine();
                var scn = JavaCodeGeneratorUtils.getPropertyType(StandardValueType.ENTITY, className, false, gen);
                gen.printLine("private final %s %s;".formatted(scn, fieldName));
            });
            gen.blankLine();
            gen.addImport("com.gridnine.webpeer.core.ui.OperationUiContext");
            gen.wrapWithBlock("public %s(String tag, OperationUiContext context)".formatted(simpleClassName), () -> {
                gen.printLine("super(tag, context);");
                fd.getTabs().forEach(tab ->{
                    gen.wrapWithBlock("",()->{
                        gen.addImport("com.gridnine.platform.elsa.admin.web.tabs.TabComponent");
                        gen.addImport("com.gridnine.platform.elsa.common.core.utils.LocaleUtils");
                        var tabName = "tab%s".formatted(tabChildCount.incrementAndGet());
                        gen.printLine("var %s = new TabComponent(\"%s\", context);".formatted(tabName, tab.getId()));
                        gen.printLine("%s.setTitle(LocaleUtils.getLocalizedName(new String[]{%s}, new String[]{%s}, \"%s\"), context);"
                                .formatted(tabName, BuildTextUtils.joinToString(tab.getTitle().keySet().stream().map("\"%s\""::formatted).toList(), ","),
                                        BuildTextUtils.joinToString(tab.getTitle().values().stream().map("\"%s\""::formatted).toList(), ", "),
                                        tab.getId()));
                        if(tab.getContent() != null) {
                            JavaAdminUiCodeGenUtils.generateNestedComponent(tab.getContent(), "content", gen);
                            gen.printLine("tab%s.addChild(context, content, 0);".formatted(tabChildCount.get()));
                        }
                        gen.printLine("addChild(context, tab%s, %s);".formatted(tabChildCount.get(), tabChildCount.get()));
                    });
                });
            });
            fields.forEach((fieldName, className) -> {
                gen.blankLine();
                var scn = JavaCodeGeneratorUtils.getPropertyType(StandardValueType.ENTITY, className, false, gen);
                gen.wrapWithBlock("public %s get%s()".formatted(scn, BuildTextUtils.capitalize(fieldName)), ()->{
                    gen.printLine("return this.%s;".formatted(fieldName));
                });
            });

        });

        var file = JavaCodeGeneratorUtils.saveIfDiffers(gen.toString(), fd.getClassName() + ".java", destDir);
        generatedFiles.add(file);
    }


    public static void generateNestedComponent(NestedTabsDescription cd, String containerName, JavaCodeGenerator gen) {
        gen.addImport("com.gridnine.platform.elsa.admin.web.tabs.TabsContainer");
        gen.printLine("var %s = new TabsContainer(\"tabs\", context);".formatted(containerName));
        var tabChildCount = new AtomicInteger(-1);
        cd.getTabs().forEach(tab ->{
            gen.wrapWithBlock("",()->{
                gen.addImport("com.gridnine.platform.elsa.admin.web.tabs.TabComponent");
                gen.addImport("com.gridnine.platform.elsa.common.core.utils.LocaleUtils");
                var tabName = "%sTab%s".formatted(containerName, tabChildCount.incrementAndGet());
                gen.printLine("var %s = new TabComponent(\"%s\", context);".formatted(tabName, tab.getId()));
                gen.printLine("%s.setTitle(LocaleUtils.getLocalizedName(new String[]{%s}, new String[]{%s}, \"%s\"), context);"
                        .formatted(tabName, BuildTextUtils.joinToString(tab.getTitle().keySet().stream().map("\"%s\""::formatted).toList(), ","),
                                BuildTextUtils.joinToString(tab.getTitle().values().stream().map("\"%s\""::formatted).toList(), ", "),
                                tab.getId()));
                if(tab.getContent() != null) {
                    var contentName = "%sTab%sContent".formatted(containerName,tabChildCount.get());
                    JavaAdminUiCodeGenUtils.generateNestedComponent(tab.getContent(), contentName, gen);
                    gen.printLine("tab%s.addChild(context, content, 0);".formatted(tabChildCount.get()));
                }
                gen.printLine("%s.addChild(context, %s, %s);".formatted(containerName, tabName, tabChildCount.get()));
            });
        });
    }

    public static void collectFields(NestedTabsDescription cd, Map<String, String> fields) {
        cd.getTabs().forEach(r ->{
            if(r.getContent() != null){
                BuildExceptionUtils.wrapException(()->JavaAdminUiCodeGenUtils.collectFields(r.getContent(), fields));
            }
        });
    }
}
