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

package com.gridnine.platform.elsa.gradle.codegen.adminUi.common;

import com.gridnine.platform.elsa.gradle.codegen.adminUi.div.JavaAdminUiDivConfiguratorHelper;
import com.gridnine.platform.elsa.gradle.codegen.adminUi.div.JavaAdminUiDivEditorHelper;
import com.gridnine.platform.elsa.gradle.codegen.adminUi.form.JavaAdminUiFormConfiguratorHelper;
import com.gridnine.platform.elsa.gradle.codegen.adminUi.form.JavaAdminUiFormEditorHelper;
import com.gridnine.platform.elsa.gradle.codegen.adminUi.grid.JavaAdminUiGridConfiguratorHelper;
import com.gridnine.platform.elsa.gradle.codegen.adminUi.grid.JavaAdminUiGridEditorHelper;
import com.gridnine.platform.elsa.gradle.codegen.adminUi.standalone.JavaAdminUiStandaloneConfiguratorHelper;
import com.gridnine.platform.elsa.gradle.codegen.adminUi.standalone.JavaAdminUiStandaloneEditorHelper;
import com.gridnine.platform.elsa.gradle.codegen.adminUi.tabs.JavaAdminUiTabsConfiguratorHelper;
import com.gridnine.platform.elsa.gradle.codegen.adminUi.tabs.JavaAdminUiTabsEditorHelper;
import com.gridnine.platform.elsa.gradle.codegen.common.JavaCodeGenerator;
import com.gridnine.platform.elsa.gradle.meta.adminUi.BaseAdminUiElementDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.div.NestedDivDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.form.NestedFormDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.grid.NestedGridDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.standalone.StandaloneButtonDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.standalone.StandaloneEditableTableDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.standalone.StandaloneRichTextEditorDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.standalone.StandaloneTextFieldDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.tabs.NestedTabsDescription;

import java.util.Locale;
import java.util.Map;

public class JavaAdminUiCodeGenUtils {
    public static void updateTitle(Map<Locale, String> map, String compName, JavaCodeGenerator generator) {
        for (Map.Entry<Locale, String> entry : map.entrySet()) {
            generator.addImport("com.gridnine.platform.elsa.common.core.utils.LocaleUtils");
            generator.printLine("%s.getTitle().put(LocaleUtils.getLocale(\"%s\", null), \"%s\");".formatted(compName, entry.getKey().getLanguage(), entry.getValue()));
        }
    }

    public static void generateDescription(BaseAdminUiElementDescription cd, String containerDescriptionName, int nestedCount, JavaCodeGenerator gen) throws Exception {
        switch (cd.getType()) {
            case ROOT_FORM, NESTED_FORM -> {
                JavaAdminUiFormConfiguratorHelper.generateDescription((NestedFormDescription) cd, containerDescriptionName, nestedCount, gen);
            }
            case ROOT_GRID,NESTED_GRID -> {
                JavaAdminUiGridConfiguratorHelper.generateDescription((NestedGridDescription) cd, containerDescriptionName, nestedCount, gen);
            }
            case ROOT_TABS,NESTED_TABS -> {
                JavaAdminUiTabsConfiguratorHelper.generateDescription((NestedTabsDescription) cd, containerDescriptionName, nestedCount, gen);
            }
            case ROOT_DIV,NESTED_DIV -> {
                JavaAdminUiDivConfiguratorHelper.generateDescription((NestedDivDescription) cd, containerDescriptionName, nestedCount, gen);
            }
            case STANDALONE_RICH_TEXT_EDITOR -> {
                JavaAdminUiStandaloneConfiguratorHelper.generateNestedDescription((StandaloneRichTextEditorDescription) cd, containerDescriptionName, nestedCount, gen);
            }
            case STANDALONE_EDITABLE_TABLE -> {
                JavaAdminUiStandaloneConfiguratorHelper.generateNestedDescriptionEditableTable((StandaloneEditableTableDescription) cd, containerDescriptionName, nestedCount, gen);
            }
            case STANDALONE_BUTTON -> {
                JavaAdminUiStandaloneConfiguratorHelper.generateNestedDescriptionButton((StandaloneButtonDescription) cd, containerDescriptionName, nestedCount, gen);
            }
            case STANDALONE_TEXT_FIELD -> {
                JavaAdminUiStandaloneConfiguratorHelper.generateNestedDescriptionTextField((StandaloneTextFieldDescription) cd, containerDescriptionName, nestedCount, gen);
            }
        }
    }

    public static void collectFields(BaseAdminUiElementDescription cd, Map<String,String> fields) throws Exception {
        switch (cd.getType()) {
            case ROOT_FORM,NESTED_FORM -> {
                JavaAdminUiFormEditorHelper.collectFields((NestedFormDescription) cd, fields);
            }
            case ROOT_GRID,NESTED_GRID -> {
                JavaAdminUiGridEditorHelper.collectFields((NestedGridDescription) cd, fields);
            }
            case ROOT_TABS,NESTED_TABS -> {
                JavaAdminUiTabsEditorHelper.collectFields((NestedTabsDescription) cd, fields);
            }
            case ROOT_DIV,NESTED_DIV -> {
                JavaAdminUiDivEditorHelper.collectFields((NestedDivDescription) cd, fields);
            }
            case STANDALONE_RICH_TEXT_EDITOR -> {
                JavaAdminUiStandaloneEditorHelper.collectFields((StandaloneRichTextEditorDescription) cd, fields);
            }
            case STANDALONE_EDITABLE_TABLE -> {
                JavaAdminUiStandaloneEditorHelper.collectEditableTableFields((StandaloneEditableTableDescription) cd, fields);
            }
            case STANDALONE_BUTTON -> {
                JavaAdminUiStandaloneEditorHelper.collectButtonFields((StandaloneButtonDescription) cd, fields);
            }
            case STANDALONE_TEXT_FIELD -> {
                JavaAdminUiStandaloneEditorHelper.collectTextFieldFields((StandaloneTextFieldDescription) cd, fields);
            }
        }
    }

    public static void generateNestedComponent(BaseAdminUiElementDescription cd, String  containerName, JavaCodeGenerator gen) {
        switch (cd.getType()) {
            case NESTED_FORM -> {
                JavaAdminUiFormEditorHelper.generateNestedComponent((NestedFormDescription) cd, containerName, gen);
            }
            case NESTED_GRID -> {
                JavaAdminUiGridEditorHelper.generateNestedComponent((NestedGridDescription) cd,  containerName, gen);
            }
            case NESTED_TABS -> {
                JavaAdminUiTabsEditorHelper.generateNestedComponent((NestedTabsDescription) cd, containerName, gen);
            }
            case NESTED_DIV -> {
                JavaAdminUiDivEditorHelper.generateNestedComponent((NestedDivDescription) cd, containerName, gen);
            }
            case STANDALONE_RICH_TEXT_EDITOR -> {
                JavaAdminUiStandaloneEditorHelper.generateEditor((StandaloneRichTextEditorDescription) cd, containerName, gen);
            }
            case STANDALONE_EDITABLE_TABLE -> {
                JavaAdminUiStandaloneEditorHelper.generateEditableTableEditor((StandaloneEditableTableDescription) cd, containerName, gen);
            }
            case STANDALONE_BUTTON -> {
                JavaAdminUiStandaloneEditorHelper.generateButtonEditor((StandaloneButtonDescription) cd, containerName, gen);
            }
            case STANDALONE_TEXT_FIELD -> {
                JavaAdminUiStandaloneEditorHelper.generateTextFieldEditor((StandaloneTextFieldDescription) cd, containerName, gen);
            }
        }
    }
}