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
import com.gridnine.platform.elsa.gradle.meta.adminUi.div.NestedDivDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.div.RootDivDescription;
import com.gridnine.platform.elsa.gradle.utils.BuildRunnableWithException;

import java.util.concurrent.atomic.AtomicInteger;

public class JavaAdminUiDivConfiguratorHelper {

    public static void generateDescription(NestedDivDescription cd, String containerDescriptionName, int nestedCount, JavaCodeGenerator gen) throws Exception {
        var runnable = new BuildRunnableWithException() {
            @Override
            public void run() throws Exception {
                if (nestedCount == 0) {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.div.RootDivDescription");
                    gen.printLine("""
                            var %s = (RootDivDescription) registry.getContainers().computeIfAbsent("%s", className->{
                                            var ctr = new RootDivDescription();
                                            ctr.setClassName(className);
                                            return ctr;
                                        });""".formatted(containerDescriptionName, ((RootDivDescription)cd).getClassName()));
                } else {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.div.NestedDivDescription");
                    gen.printLine("var %s = new NestedDivDescription();".formatted(containerDescriptionName));
                }
                if(cd.getId() != null){
                    gen.printLine("%s.setId(\"%s\");".formatted(containerDescriptionName, cd.getId()));
                }
                if(cd.getStyle() != null){
                    gen.printLine("%s.setStyle(\"%s\");".formatted(containerDescriptionName, cd.getStyle()));
                }
                var childCount = new AtomicInteger();
                for(var child: cd.getChildren()){
                    var childName = nestedCount == 0?  "nestedContent%s".formatted(childCount.incrementAndGet()):"%s_nestedContent%s".formatted(containerDescriptionName, childCount.incrementAndGet());
                    JavaAdminUiCodeGenUtils.generateDescription(child, childName, nestedCount+1, gen);
                }
            }
        };
        if (nestedCount == 0) {
            gen.wrapWithBlock(null, runnable);
        } else {
            runnable.run();
        }
    }
}
