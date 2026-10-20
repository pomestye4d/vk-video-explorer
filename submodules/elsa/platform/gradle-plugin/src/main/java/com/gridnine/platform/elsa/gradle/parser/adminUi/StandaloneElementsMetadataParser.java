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

package com.gridnine.platform.elsa.gradle.parser.adminUi;

import com.gridnine.platform.elsa.gradle.meta.adminUi.AdminUiMetaRegistry;
import com.gridnine.platform.elsa.gradle.meta.adminUi.standalone.EditableTableButtonItemDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.standalone.EditableTableColumnDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.standalone.EditableTableSummaryColumnDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.standalone.EditableTableSummaryRowDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.standalone.StandaloneEditableTableDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.standalone.StandaloneEditableTableSummaryDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.standalone.StandaloneButtonDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.standalone.StandaloneRichTextEditorDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.standalone.StandaloneTextFieldDescription;
import com.gridnine.platform.elsa.gradle.meta.common.XmlNode;
import com.gridnine.platform.elsa.gradle.meta.webApp.CustomWebElementDescription;
import com.gridnine.platform.elsa.gradle.parser.common.CommonParserUtils;

public class StandaloneElementsMetadataParser {

    public static StandaloneRichTextEditorDescription parse(XmlNode node) {
        var desc = new StandaloneRichTextEditorDescription();
        desc.setStyle(node.getAttribute("style"));
        desc.setId(node.getAttribute("id"));
        desc.setId(CommonParserUtils.getIdAttribute(node));
        CommonParserUtils.updateLocalizationFromAttribute(node, "title", desc.getTitle());
        return desc;
    }

    public static StandaloneButtonDescription parseButton(XmlNode node) {
        var desc = new StandaloneButtonDescription();
        desc.setId(CommonParserUtils.getIdAttribute(node));
        desc.setTooltip(node.getAttribute("tooltip"));
        desc.setButtonTitle(node.getAttribute("title"));
        desc.setIcon(node.getAttribute("icon"));
        desc.setEnablingExpression(node.getAttribute("enabling-expression"));
        return desc;
    }

    public static StandaloneTextFieldDescription parseTextField(XmlNode node) {
        var desc = new StandaloneTextFieldDescription();
        desc.setId(CommonParserUtils.getIdAttribute(node));
        desc.setPlaceholder(node.getAttribute("placeholder"));
        desc.setEnablingExpression(node.getAttribute("enabling-expression"));
        var dt = node.getAttribute("debounce-time");
        if (dt != null && !dt.isBlank()) {
            desc.setDebounceTime(Integer.parseInt(dt));
        }
        desc.setDeferred("true".equals(node.getAttribute("deferred")));
        desc.setStyle(node.getAttribute("style"));
        return desc;
    }

    public static StandaloneEditableTableDescription parseEditableTable(XmlNode node, String containerClassName, AdminUiMetaRegistry registry) {
        var desc = new StandaloneEditableTableDescription();
        desc.setId(CommonParserUtils.getIdAttribute(node));
        registry.getEditableTables().put("%s-%s".formatted(containerClassName, desc.getId()), desc);
        desc.setContainerClassName(containerClassName);
        CommonParserUtils.updateLocalizationFromAttribute(node, "title", desc.getTitle());
        var selectionMode = node.getAttribute("selectionMode");
        if (selectionMode != null && !selectionMode.isBlank()) {
            desc.setSelectionMode(selectionMode);
        }
        for (var child : node.getChildren()) {
            if ("columns".equals(child.getName())) {
                for (var colNode : child.getChildren()) {
                    if ("column".equals(colNode.getName())) {
                        var col = new EditableTableColumnDescription();
                        col.setId(CommonParserUtils.getIdAttribute(colNode));
                        col.setType(colNode.getAttribute("type"));
                        col.setIcon(colNode.getAttribute("icon"));
                        col.setEnablingExpression(colNode.getAttribute("enabling-expression"));
                        CommonParserUtils.updateLocalizationFromAttribute(colNode, "title", col.getTitle());
                        var widthStr = colNode.getAttribute("width");
                        if (widthStr != null && !widthStr.isBlank()) {
                            col.setWidth(Integer.parseInt(widthStr));
                        }
                        col.setEditable("true".equals(colNode.getAttribute("editable")));
                        var precisionStr = colNode.getAttribute("precision");
                        if (precisionStr != null && !precisionStr.isBlank()) {
                            col.setPrecision(Integer.parseInt(precisionStr));
                        }
                        for (var buttonNode : colNode.getChildren()) {
                            if ("button".equals(buttonNode.getName())) {
                                var item = new EditableTableButtonItemDescription();
                                item.setId(CommonParserUtils.getIdAttribute(buttonNode));
                                CommonParserUtils.updateLocalizationFromAttribute(buttonNode, "title", item.getTitle());
                                item.setIcon(buttonNode.getAttribute("icon"));
                                item.setEnablingExpression(buttonNode.getAttribute("enabling-expression"));
                                col.getButtonItems().add(item);
                            }
                        }
                        desc.getColumns().add(col);
                    }
                }
            } else if ("summary".equals(child.getName())) {
                var summary = new StandaloneEditableTableSummaryDescription();
                for (var rowNode : child.getChildren()) {
                    if ("row".equals(rowNode.getName())) {
                        var row = new EditableTableSummaryRowDescription();
                        for (var sumColNode : rowNode.getChildren()) {
                            if ("column".equals(sumColNode.getName())) {
                                var sumCol = new EditableTableSummaryColumnDescription();
                                sumCol.setId(CommonParserUtils.getIdAttribute(sumColNode));
                                sumCol.setType(sumColNode.getAttribute("type"));
                                CommonParserUtils.updateLocalizationFromAttribute(sumColNode, "label", sumCol.getLabel());
                                var rowSpanStr = sumColNode.getAttribute("rowSpan");
                                if (rowSpanStr != null && !rowSpanStr.isBlank()) {
                                    sumCol.setRowSpan(Integer.parseInt(rowSpanStr));
                                }
                                var colSpanStr = sumColNode.getAttribute("columnSpan");
                                if (colSpanStr != null && !colSpanStr.isBlank()) {
                                    sumCol.setColumnSpan(Integer.parseInt(colSpanStr));
                                }
                                row.getColumns().add(sumCol);
                            }
                        }
                        summary.getRows().add(row);
                    }
                }
                desc.setSummaryDescription(summary);
            }
        }
        return desc;
    }
}
