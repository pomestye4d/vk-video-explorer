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

package com.gridnine.platform.elsa.gradle.codegen.adminUi.standalone;

import com.gridnine.platform.elsa.gradle.codegen.common.JavaCodeGenerator;
import com.gridnine.platform.elsa.gradle.meta.adminUi.standalone.StandaloneButtonDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.standalone.StandaloneEditableTableDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.standalone.StandaloneRichTextEditorDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.standalone.StandaloneTextFieldDescription;

public class JavaAdminUiStandaloneConfiguratorHelper {

    public static void generateNestedDescriptionTextField(StandaloneTextFieldDescription cd, String containerDescriptionName, int nestedCount, JavaCodeGenerator gen) throws Exception {
        gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.standalone.StandaloneTextFieldDescription");
        gen.printLine("var %s = new StandaloneTextFieldDescription();".formatted(containerDescriptionName));
    }

    public static void generateNestedDescriptionButton(StandaloneButtonDescription cd, String containerDescriptionName, int nestedCount, JavaCodeGenerator gen) throws Exception {
        gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.standalone.StandaloneButtonDescription");
        gen.printLine("var %s = new StandaloneButtonDescription();".formatted(containerDescriptionName));
    }

    public static void generateNestedDescription(StandaloneRichTextEditorDescription cd, String containerDescriptionName, int nestedCount, JavaCodeGenerator gen) throws Exception {
        gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.standalone.StandaloneRichTextEditorDescription");
        gen.printLine("var %s = new StandaloneRichTextEditorDescription();".formatted(containerDescriptionName));
    }

    public static void generateNestedDescriptionEditableTable(StandaloneEditableTableDescription cd, String containerDescriptionName, int nestedCount, JavaCodeGenerator gen) throws Exception {
        var registryKey = "%s.%s".formatted(cd.getContainerClassName(), cd.getId());
        gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.standalone.StandaloneEditableTableDescription");
            gen.printLine("var %s = new StandaloneEditableTableDescription();".formatted(containerDescriptionName));
            gen.printLine("%s.setId(\"%s\");".formatted(containerDescriptionName, cd.getId()));
            gen.printLine("%s.setContainerClassName(\"%s\");".formatted(containerDescriptionName, cd.getContainerClassName()));
            if (cd.getSelectionMode() != null) {
                gen.printLine("%s.setSelectionMode(\"%s\");".formatted(containerDescriptionName, cd.getSelectionMode()));
            }
            if (!cd.getTitle().isEmpty()) {
                gen.addImport("com.gridnine.platform.elsa.common.core.utils.LocaleUtils");
                cd.getTitle().forEach((locale, value) ->
                    gen.printLine("%s.getTitle().put(LocaleUtils.getLocale(\"%s\", null), \"%s\");".formatted(containerDescriptionName, locale.getLanguage(), value))
                );
            }
            if (!cd.getColumns().isEmpty()) {
                gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.standalone.EditableTableColumnDescription");
                gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.standalone.EditableTableColumnType");
                cd.getColumns().forEach(col -> {
                    gen.wrapWithBlock(null, () -> {
                        gen.printLine("var col = new EditableTableColumnDescription();");
                        gen.printLine("col.setId(\"%s\");".formatted(col.getId()));
                        if(col.getIcon() != null){
                            gen.printLine("col.setIcon(\"%s\");".formatted(col.getIcon()));
                        }
                        if(col.getEnablingExpression() != null){
                            gen.printLine("col.setEnablingExpression(\"%s\");".formatted(col.getEnablingExpression()));
                        }
                        gen.printLine("col.setType(EditableTableColumnType.%s);".formatted(col.getType()));
                        if (col.getWidth() != null) {
                            gen.printLine("col.setWidth(%s);".formatted(col.getWidth()));
                        }
                        if(col.isEditable()){
                            gen.printLine("col.setEditable(true);");
                        }
                        if (col.getPrecision() != null) {
                            gen.printLine("col.setPrecision(%s);".formatted(col.getPrecision()));
                        }
                        if (!col.getTitle().isEmpty()) {
                            gen.addImport("com.gridnine.platform.elsa.common.core.utils.LocaleUtils");
                            col.getTitle().forEach((locale, value) ->
                                gen.printLine("col.getTitle().put(LocaleUtils.getLocale(\"%s\", null), \"%s\");".formatted(locale.getLanguage(), value))
                            );
                        }
                        if ("DROP_DOWN_BUTTON".equals(col.getType()) && !col.getButtonItems().isEmpty()) {
                            gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.standalone.EditableTableButtonItemDescription");
                            col.getButtonItems().forEach(item -> {
                                gen.wrapWithBlock(null, () -> {
                                    gen.printLine("var btn = new EditableTableButtonItemDescription();");
                                    gen.printLine("btn.setId(\"%s\");".formatted(item.getId()));
                                    if (item.getIcon() != null) {
                                        gen.printLine("btn.setIcon(\"%s\");".formatted(item.getIcon()));
                                    }
                                    if (item.getEnablingExpression() != null) {
                                        gen.printLine("btn.setEnablingExpression(\"%s\");".formatted(item.getEnablingExpression()));
                                    }
                                    if (!item.getTitle().isEmpty()) {
                                        gen.addImport("com.gridnine.platform.elsa.common.core.utils.LocaleUtils");
                                        item.getTitle().forEach((locale, value) ->
                                            gen.printLine("btn.getTitle().put(LocaleUtils.getLocale(\"%s\", null), \"%s\");".formatted(locale.getLanguage(), value))
                                        );
                                    }
                                    gen.printLine("col.getButtonItems().add(btn);");
                                });
                            });
                        }
                        gen.printLine("%s.getColumns().add(col);".formatted(containerDescriptionName));
                    });
                });
            }
            if (cd.getSummaryDescription() != null) {
                gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.standalone.StandaloneEditableTableSummaryDescription");
                gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.standalone.EditableTableSummaryRowDescription");
                gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.standalone.EditableTableSummaryColumnDescription");
                gen.addImport("com.gridnine.platform.elsa.common.meta.adminUi.standalone.EditableTableSummaryColumnType");
                gen.wrapWithBlock(null, ()->{
                    gen.printLine("var summary = new StandaloneEditableTableSummaryDescription();");
                    cd.getSummaryDescription().getRows().forEach(row -> {
                        gen.wrapWithBlock(null, () -> {
                            gen.printLine("var summaryRow = new EditableTableSummaryRowDescription();");
                            row.getColumns().forEach(sumCol -> {
                                gen.wrapWithBlock(null, () -> {
                                    gen.printLine("var summaryCol = new EditableTableSummaryColumnDescription();");
                                    gen.printLine("summaryCol.setId(\"%s\");".formatted(sumCol.getId()));
                                    gen.printLine("summaryCol.setType(EditableTableSummaryColumnType.%s);".formatted(sumCol.getType()));
                                    if (sumCol.getRowSpan() != null) {
                                        gen.printLine("summaryCol.setRowSpan(%s);".formatted(sumCol.getRowSpan()));
                                    }
                                    if (sumCol.getColumnSpan() != null) {
                                        gen.printLine("summaryCol.setColumnSpan(%s);".formatted(sumCol.getColumnSpan()));
                                    }
                                    if (!sumCol.getLabel().isEmpty()) {
                                        gen.addImport("com.gridnine.platform.elsa.common.core.utils.LocaleUtils");
                                        sumCol.getLabel().forEach((locale, value) ->
                                                gen.printLine("summaryCol.getLabel().put(LocaleUtils.getLocale(\"%s\", null), \"%s\");".formatted(locale.getLanguage(), value))
                                        );
                                    }
                                    gen.printLine("summaryRow.getColumns().add(summaryCol);");
                                });
                            });
                            gen.printLine("summary.getRows().add(summaryRow);");
                        });
                    });
                    gen.printLine("%s.setSummaryDescription(summary);".formatted(containerDescriptionName));
                });
            }
            gen.printLine("registry.getEditableTables().put(\"%s\", %s);".formatted(registryKey, containerDescriptionName));
    }
}
