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
import com.gridnine.platform.elsa.gradle.meta.adminUi.form.*;
import com.gridnine.platform.elsa.gradle.meta.adminUi.div.RootDivDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.grid.RootGridDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.tabs.RootTabsDescription;
import com.gridnine.platform.elsa.gradle.meta.common.XmlNode;
import com.gridnine.platform.elsa.gradle.parser.common.CommonParserUtils;
import com.gridnine.platform.elsa.gradle.utils.BuildTextUtils;

import java.util.Map;

public class FormMetadataParser {
    public static NestedFormDescription parseForm(XmlNode child, boolean root, String rootContainerName, AdminUiMetaRegistry registry){
        var formDescription = root? new RootFormDescription(): new NestedFormDescription();
        if(formDescription instanceof RootFormDescription rfd){
            rfd.setClassName(rootContainerName);
            registry.getContainers().put(rfd.getClassName(), formDescription);
        }
        CommonParserUtils.updateLocalizationFromAttribute(child, "title", formDescription.getTitle());
        parseFormControls(child, formDescription.getComponents(), rootContainerName, registry);
        return  formDescription;
    }

    public static void parseFormControls(XmlNode child, Map<String, BaseAdminUiFormComponentDescription> components, String rootContainerName, AdminUiMetaRegistry registry){
        child.getChildren().forEach(childNode -> {
            var tag = childNode.getName();
            if("text-field".equals(tag)){
                var textFieldDescription = new FormTextFieldDescription();
                textFieldDescription.setId(CommonParserUtils.getIdAttribute(childNode));
                CommonParserUtils.updateDisplayName(childNode,  textFieldDescription.getTitle());
                components.put(textFieldDescription.getId(), textFieldDescription);
                return;
            }
            if("hidden-field".equals(tag)){
                var hiddenFieldDescription = new FormHiddenFieldDescription();
                hiddenFieldDescription.setId(CommonParserUtils.getIdAttribute(childNode));
                components.put(hiddenFieldDescription.getId(), hiddenFieldDescription);
                return;
            }
            if("int-field".equals(tag)){
                var intFieldDescription = new FormIntFieldDescription();
                intFieldDescription.setId(CommonParserUtils.getIdAttribute(childNode));
                CommonParserUtils.updateDisplayName(childNode,  intFieldDescription.getTitle());
                components.put(intFieldDescription.getId(), intFieldDescription);
                return;
            }
            if("big-decimal-field".equals(tag)){
                var bigDecimalFieldDescription = new FormBigDecimalFieldDescription();
                bigDecimalFieldDescription.setId(CommonParserUtils.getIdAttribute(childNode));
                CommonParserUtils.updateDisplayName(childNode,  bigDecimalFieldDescription.getTitle());
                var precisionAttr = childNode.getAttribute("precision");
                if(precisionAttr != null){
                    bigDecimalFieldDescription.setPrecision(Integer.parseInt(precisionAttr));
                }
                components.put(bigDecimalFieldDescription.getId(), bigDecimalFieldDescription);
                return;
            }
            if("enum-select".equals(tag)){
                var enumSelectDescription = new FormEnumSelectDescription();
                enumSelectDescription.setId(CommonParserUtils.getIdAttribute(childNode));
                CommonParserUtils.updateDisplayName(childNode,  enumSelectDescription.getTitle());
                enumSelectDescription.setClassName(childNode.getAttribute("class-name"));
                components.put(enumSelectDescription.getId(), enumSelectDescription);
                return;
            }
            if("files-field".equals(tag)){
                var filesFieldDescription = new FormFilesFieldDescription();
                filesFieldDescription.setId(CommonParserUtils.getIdAttribute(childNode));
                CommonParserUtils.updateDisplayName(childNode,  filesFieldDescription.getTitle());
                components.put(filesFieldDescription.getId(), filesFieldDescription);
                return;
            }
            if("references-list".equals(tag)){
                var referencesListDescription = new FormReferencesListDescription();
                referencesListDescription.setId(CommonParserUtils.getIdAttribute(childNode));
                CommonParserUtils.updateDisplayName(childNode,  referencesListDescription.getTitle());
                components.put(referencesListDescription.getId(), referencesListDescription);
                return;
            }
            if("link-field".equals(tag)){
                var linkFieldDescription = new FormLinkFieldDescription();
                linkFieldDescription.setId(CommonParserUtils.getIdAttribute(childNode));
                CommonParserUtils.updateDisplayName(childNode,  linkFieldDescription.getTitle());
                components.put(linkFieldDescription.getId(), linkFieldDescription);
                return;
            }
            if("text-area".equals(tag)){
                var textAreaDescription = new FormTextAreaDescription();
                textAreaDescription.setId(CommonParserUtils.getIdAttribute(childNode));
                CommonParserUtils.updateDisplayName(childNode,  textAreaDescription.getTitle());
                components.put(textAreaDescription.getId(), textAreaDescription);
                return;
            }
            if("boolean-field".equals(tag)){
                var booleanFieldDescription = new FormBooleanFieldDescription();
                booleanFieldDescription.setId(CommonParserUtils.getIdAttribute(childNode));
                CommonParserUtils.updateDisplayName(childNode,  booleanFieldDescription.getTitle());
                components.put(booleanFieldDescription.getId(), booleanFieldDescription);
                return;
            }
            if("date-interval-field".equals(tag)){
                var dateIntervalFieldDescription = new FormDateIntervalFieldDescription();
                dateIntervalFieldDescription.setId(CommonParserUtils.getIdAttribute(childNode));
                CommonParserUtils.updateDisplayName(childNode,  dateIntervalFieldDescription.getTitle());
                components.put(dateIntervalFieldDescription.getId(), dateIntervalFieldDescription);
                return;
            }
            if("local-date-field".equals(tag)){
                var localDateFieldDescription = new FormLocalDateFieldDescription();
                localDateFieldDescription.setId(CommonParserUtils.getIdAttribute(childNode));
                CommonParserUtils.updateDisplayName(childNode,  localDateFieldDescription.getTitle());
                components.put(localDateFieldDescription.getId(), localDateFieldDescription);
                return;
            }
            if("local-time-field".equals(tag)){
                var localTimeFieldDescription = new FormLocalTimeFieldDescription();
                localTimeFieldDescription.setId(CommonParserUtils.getIdAttribute(childNode));
                CommonParserUtils.updateDisplayName(childNode,  localTimeFieldDescription.getTitle());
                components.put(localTimeFieldDescription.getId(), localTimeFieldDescription);
                return;
            }
            if("local-period-field".equals(tag)){
                var localPeriodFieldDescription = new FormLocalPeriodFieldDescription();
                localPeriodFieldDescription.setId(CommonParserUtils.getIdAttribute(childNode));
                CommonParserUtils.updateDisplayName(childNode,  localPeriodFieldDescription.getTitle());
                components.put(localPeriodFieldDescription.getId(), localPeriodFieldDescription);
                return;
            }
            if("label-with-tools".equals(tag)){
                var labelWithToolsDescription = new FormLabelWithToolsDescription();
                labelWithToolsDescription.setId(CommonParserUtils.getIdAttribute(childNode));
                labelWithToolsDescription.setStyle(childNode.getAttribute("style"));
                CommonParserUtils.updateDisplayName(childNode,  labelWithToolsDescription.getTitle());
                components.put(labelWithToolsDescription.getId(), labelWithToolsDescription);
                return;
            }
            if("div".equals(tag)){
                var formDivDescription = new FormDivDescription();
                formDivDescription.setId(CommonParserUtils.getIdAttribute(childNode));
                var content = DivMetadataParser.parseDiv(childNode, false, rootContainerName, registry);
                content.setId(formDivDescription.getId());
                formDivDescription.setContent(content);
                components.put(formDivDescription.getId(), formDivDescription);
                return;
            }
            if("custom".equals(tag)){
                var customFieldDescription = new FormCustomElementDescription();
                customFieldDescription.setId(CommonParserUtils.getIdAttribute(childNode));
                customFieldDescription.setClassName(childNode.getAttribute("class-name"));
                CommonParserUtils.updateDisplayName(childNode,  customFieldDescription.getTitle());
                components.put(customFieldDescription.getId(), customFieldDescription);
                return;
            }
            if("remote-select".equals(tag)){
                var remoteSelectDescription = new FormRemoteSelectDescription();
                remoteSelectDescription.setId(CommonParserUtils.getIdAttribute(childNode));
                remoteSelectDescription.setClassName(childNode.getAttribute("class-name"));
                remoteSelectDescription.setAllowAddNewValue("true".equals(childNode.getAttribute("allow-add-new-item")));
                CommonParserUtils.updateDisplayName(childNode,  remoteSelectDescription.getTitle());
                components.put(remoteSelectDescription.getId(), remoteSelectDescription);
                return;
            }
            if("remote-multi-select".equals(tag)){
                var remoteMultiSelectDescription = new FormRemoteMultiSelectDescription();
                remoteMultiSelectDescription.setId(CommonParserUtils.getIdAttribute(childNode));
                remoteMultiSelectDescription.setClassName(childNode.getAttribute("class-name"));
                remoteMultiSelectDescription.setAllowAddNewValue("true".equals(childNode.getAttribute("allow-add-new-item")));
                remoteMultiSelectDescription.setOrderEditable("true".equals(childNode.getAttribute("order-editable")));
                CommonParserUtils.updateDisplayName(childNode,  remoteMultiSelectDescription.getTitle());
                components.put(remoteMultiSelectDescription.getId(), remoteMultiSelectDescription);
                return;
            }
            if("select".equals(tag)){
                var selectDescription = new FormSelectDescription();
                selectDescription.setId(CommonParserUtils.getIdAttribute(childNode));
                CommonParserUtils.updateDisplayName(childNode,  selectDescription.getTitle());
                components.put(selectDescription.getId(), selectDescription);
                return;
            }
            if("multi-select".equals(tag)){
                var multiSelectDescription = new FormMultiSelectDescription();
                multiSelectDescription.setId(CommonParserUtils.getIdAttribute(childNode));
                CommonParserUtils.updateDisplayName(childNode,  multiSelectDescription.getTitle());
                components.put(multiSelectDescription.getId(), multiSelectDescription);
                return;
            }
            if("enum-multi-select".equals(tag)){
                var enumMultiSelectDescription = new FormEnumMultiSelectDescription();
                enumMultiSelectDescription.setId(CommonParserUtils.getIdAttribute(childNode));
                CommonParserUtils.updateDisplayName(childNode,  enumMultiSelectDescription.getTitle());
                enumMultiSelectDescription.setClassName(childNode.getAttribute("class-name"));
                components.put(enumMultiSelectDescription.getId(), enumMultiSelectDescription);
                return;
            }
            if("rich-text-editor".equals(tag)){
                var richTextEditorDescription = new FormRichTextEditorDescription();
                richTextEditorDescription.setId(CommonParserUtils.getIdAttribute(childNode));
                CommonParserUtils.updateDisplayName(childNode, richTextEditorDescription.getTitle());
                components.put(richTextEditorDescription.getId(), richTextEditorDescription);
                return;
            }
            if("entities-list".equals(tag)){
                var entitiesListDescription = new FormEntitiesListDescription();
                entitiesListDescription.setId(CommonParserUtils.getIdAttribute(childNode));
                CommonParserUtils.updateDisplayName(childNode,  entitiesListDescription.getTitle());
                var editorElm = childNode.getChildren().get(0);
                var className = "%s%s".formatted(rootContainerName, BuildTextUtils.capitalize(entitiesListDescription.getId()));
                var editorDescription = AdminUiParserHelper.parse(editorElm, true, className, registry);
                if(editorDescription instanceof  RootFormDescription fd){
                    fd.setClassName(className);
                } else if(editorDescription instanceof RootGridDescription gd){
                    gd.setClassName(className);
                } else if(editorDescription instanceof RootDivDescription gd){
                    gd.setClassName(className);
                }  else if(editorDescription instanceof RootTabsDescription gd){
                    gd.setClassName(className);
                }else {
                    throw new IllegalArgumentException("Unsupported editor description type: " + editorDescription.getClass().getName());
                }
                entitiesListDescription.setEntityEditor(editorDescription);
                components.put(entitiesListDescription.getId(), entitiesListDescription);
                return;
            }
        });
    }
}
