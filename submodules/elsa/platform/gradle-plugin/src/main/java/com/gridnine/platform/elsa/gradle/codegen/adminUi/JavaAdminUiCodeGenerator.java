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

import com.gridnine.platform.elsa.gradle.codegen.adminUi.div.JavaAdminUiDivEditorHelper;
import com.gridnine.platform.elsa.gradle.codegen.adminUi.form.JavaAdminUiFormEditorHelper;
import com.gridnine.platform.elsa.gradle.codegen.adminUi.grid.JavaAdminUiGridEditorHelper;
import com.gridnine.platform.elsa.gradle.codegen.adminUi.standalone.JavaAdminUiStandaloneEditorHelper;
import com.gridnine.platform.elsa.gradle.codegen.adminUi.tabs.JavaAdminUiTabsEditorHelper;
import com.gridnine.platform.elsa.gradle.codegen.common.CodeGenerator;
import com.gridnine.platform.elsa.gradle.meta.adminUi.AdminUiMetaRegistry;
import com.gridnine.platform.elsa.gradle.meta.adminUi.div.RootDivDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.form.RootFormDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.grid.RootGridDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.tabs.RootTabsDescription;
import com.gridnine.platform.elsa.gradle.parser.adminUi.AdminUiMetaRegistryParser;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class JavaAdminUiCodeGenerator implements CodeGenerator<JavaAdminUiCodeGenRecord> {
    @Override
    public void generate(JavaAdminUiCodeGenRecord record, File destDir, Set<File> generatedFiles, Map<Object, Object> context) throws Exception {
        var parser = new AdminUiMetaRegistryParser();
        {
            var coll = new ArrayList<>(record.getSources());
            var metaRegistry = new AdminUiMetaRegistry();
            parser.updateMetaRegistry(metaRegistry, coll);
            JavaAdminUiEntitiesCodeGenerator.generate(metaRegistry, destDir, generatedFiles);
            JavaAdminUiStandaloneEditorHelper.generateDataClasses(metaRegistry, destDir, generatedFiles);
            for(var container: metaRegistry.getContainers().values()){
                switch (container.getType()) {
                    case ROOT_FORM -> {
                        JavaAdminUiFormEditorHelper.generateEditor((RootFormDescription) container, destDir, generatedFiles);
                    }
                    case ROOT_GRID -> {
                        JavaAdminUiGridEditorHelper.generateEditor((RootGridDescription) container, destDir, generatedFiles);
                    }
                    case ROOT_TABS -> {
                        JavaAdminUiTabsEditorHelper.generateEditor((RootTabsDescription) container, destDir, generatedFiles);
                    }
                    case ROOT_DIV ->  {
                        JavaAdminUiDivEditorHelper.generateEditor((RootDivDescription) container, destDir, generatedFiles);
                    }
                }
            }
        }
        {
            var coll = new ArrayList<>(record.getSources());
            coll.addAll(record.getLocalInjections());
            var metaRegistry = new AdminUiMetaRegistry();
            parser.updateMetaRegistry(metaRegistry, coll);
            JavaAdminUiConfiguratorCodeGenerator.generate(metaRegistry, record.getRegistryConfigurator(), destDir, generatedFiles);
        }
    }
}
