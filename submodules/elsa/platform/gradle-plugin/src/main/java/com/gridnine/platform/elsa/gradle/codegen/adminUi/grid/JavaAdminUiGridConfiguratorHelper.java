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

package com.gridnine.platform.elsa.gradle.codegen.adminUi.grid;

import com.gridnine.platform.elsa.gradle.codegen.adminUi.common.JavaAdminUiCodeGenUtils;
import com.gridnine.platform.elsa.gradle.codegen.common.JavaCodeGenerator;
import com.gridnine.platform.elsa.gradle.meta.adminUi.form.RootFormDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.grid.NestedGridDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.grid.RootGridDescription;
import com.gridnine.platform.elsa.gradle.utils.BuildRunnableWithException;

public class JavaAdminUiGridConfiguratorHelper {

    public static void generateDescription(NestedGridDescription cd, String containerDescriptionName, int nestedCount, JavaCodeGenerator gen) throws Exception {
        var runnable = new BuildRunnableWithException(){

            @Override
            public void run() throws Exception {
                if(nestedCount == 0) {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.grid.RootGridDescription");
                    gen.printLine("""
                        var %s = (RootGridDescription) registry.getContainers().computeIfAbsent("%s", className->{
                                        var ctr = new RootGridDescription();
                                        ctr.setClassName(className);
                                        return ctr;
                                    });""".formatted(containerDescriptionName, ((RootGridDescription)cd).getClassName()));
                } else {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.grid.NestedGridDescription");
                    gen.printLine("var %s = new NestedGridDescription();".formatted(containerDescriptionName));
                }
                for(var row : cd.getRows()){
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.grid.GridRowDescription");
                    gen.wrapWithBlock(null, () -> {
                        gen.printLine("var row = new GridRowDescription();");
                        gen.printLine("%s.getRows().add(row);".formatted(containerDescriptionName));
                        for(var col : row.getColumns()){
                            gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.grid.GridColumnDescription");
                            gen.wrapWithBlock(null, () -> {
                                gen.printLine("var column = new GridColumnDescription();");
                                gen.printLine("column.setSmallWidth(%s);".formatted(col.getSmallWidth()));
                                gen.printLine("column.setStandardWidth(%s);".formatted(col.getStandardWidth()));
                                gen.printLine("column.setLargeWidth(%s);".formatted(col.getLargeWidth()));
                                gen.printLine("row.getColumns().add(column);");
                                JavaAdminUiCodeGenUtils.generateDescription(col.getContent(), "nestedContent", nestedCount+1, gen);
                                gen.printLine("column.setContent(nestedContent);");
                            });
                        }
                    });
                }
            }
        };
        if(nestedCount == 0) {
            gen.wrapWithBlock(null,runnable);
        } else {
            runnable.run();
        }
    }
}
