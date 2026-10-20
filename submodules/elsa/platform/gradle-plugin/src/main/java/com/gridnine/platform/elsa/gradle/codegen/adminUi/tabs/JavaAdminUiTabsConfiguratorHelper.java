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
import com.gridnine.platform.elsa.gradle.meta.adminUi.tabs.NestedTabsDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.tabs.RootTabsDescription;
import com.gridnine.platform.elsa.gradle.utils.BuildRunnableWithException;

import java.util.concurrent.atomic.AtomicInteger;

public class JavaAdminUiTabsConfiguratorHelper {

    public static void generateDescription(NestedTabsDescription cd, String containerDescriptionName, int nestedCount, JavaCodeGenerator gen) throws Exception {
        var runnable = new BuildRunnableWithException(){

            @Override
            public void run() throws Exception {
                if (nestedCount == 0) {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.tabs.RootTabsDescription");
                    gen.printLine("""
                        var %s = (RootTabsDescription) registry.getContainers().computeIfAbsent("%s", className->{
                                        var ctr = new RootTabsDescription();
                                        ctr.setClassName(className);
                                        return ctr;
                                    });""".formatted(containerDescriptionName, ((RootTabsDescription)cd).getClassName()));
                } else {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.tabs.NestedTabsDescription");
                    gen.printLine("var %s = new NestedTabsDescription();".formatted(containerDescriptionName));
                }
                var tabIdx = new AtomicInteger(0);
                for (var tab : cd.getTabs()) {
                    gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.tabs.TabDescription");
                    gen.wrapWithBlock(null, () -> {
                        var tabVarName = "tab" + tabIdx.getAndIncrement();
                        gen.printLine("var %s = new TabDescription();".formatted(tabVarName));
                        gen.printLine("%s.setId(\"%s\");".formatted(tabVarName, tab.getId()));
                        if(tab.isDedicatedAcl()){
                            gen.printLine("%s.setDedicatedAcl(true);".formatted(tabVarName));
                        }
                        JavaAdminUiCodeGenUtils.updateTitle(tab.getTitle(), tabVarName, gen);
                        var compName = "comp" + nestedCount;
                        JavaAdminUiCodeGenUtils.generateDescription(tab.getContent(), compName, nestedCount+1, gen);
                        gen.printLine("%s.setContent(%s);".formatted(tabVarName, compName));
                        gen.printLine("%s.getTabs().add(%s);".formatted(containerDescriptionName, tabVarName));
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
