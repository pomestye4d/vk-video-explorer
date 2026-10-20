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

package com.gridnine.platform.elsa.gradle.parser.webApp;

import com.gridnine.platform.elsa.gradle.meta.common.*;
import com.gridnine.platform.elsa.gradle.meta.webApp.*;
import com.gridnine.platform.elsa.gradle.parser.common.CommonParserUtils;
import com.gridnine.platform.elsa.gradle.parser.common.MetaDataParsingResult;
import com.gridnine.platform.elsa.gradle.utils.BuildExceptionUtils;

import java.io.File;
import java.util.List;

public class WebAppMetaRegistryParser {

    public void updateMetaRegistry(WebAppMetaRegistry registry, boolean skipCommonClasses, List<File> sources) {
        sources.forEach(it -> BuildExceptionUtils.wrapException(() -> {
            MetaDataParsingResult pr = CommonParserUtils.parse(it);
            XmlNode node = pr.node();
            node.getChildren().forEach(child -> {
                if("enum".equals(child.getName())){
                    CommonParserUtils.updateEnum(registry.getEnums(), child, pr.localizations());
                } else if("entity".equals(child.getName())){
                    CommonParserUtils.updateEntity(registry.getEntities(), child);
                } else {
                    processElement(child, registry);
                }
            });
        }));
        if(!skipCommonClasses) {
            {
                var dialogButtonDescription = new ButtonWebElementDescription("com.gridnine.platform.elsa.webApp.common.DialogButton");
                registry.getElements().put(dialogButtonDescription.getClassName(), dialogButtonDescription);
            }
            {
                var confirmMessage = new LabelWebElementDescription("com.gridnine.platform.elsa.webApp.common.ConfirmMessage");
                registry.getElements().put(confirmMessage.getClassName(), confirmMessage);
            }
            {
                var wrapperCtr = new ContainerWebElementDescription("com.gridnine.platform.elsa.webApp.common.ContentWrapper");
                wrapperCtr.setManagedConfiguration(true);
                registry.getElements().put(wrapperCtr.getClassName(), wrapperCtr);
            }
            {
                var glueCtr = new ContainerWebElementDescription("com.gridnine.platform.elsa.webApp.common.Glue");
                registry.getElements().put(glueCtr.getClassName(), glueCtr);
            }
            {
                var flexDirectionEnum = new EnumDescription("com.gridnine.platform.elsa.webApp.common.FlexDirection");
                {
                    var item = new EnumItemDescription("ROW");
                    flexDirectionEnum.getItems().put(item.getId(), item);
                }
                {
                    var item = new EnumItemDescription("COLUMN");
                    flexDirectionEnum.getItems().put(item.getId(), item);
                }
                registry.getEnums().put(flexDirectionEnum.getId(), flexDirectionEnum);
            }
            {
                var sortOrderEnum = new EnumDescription("com.gridnine.platform.elsa.webApp.common.SortOrder");
                {
                    var item = new EnumItemDescription("ASC");
                    sortOrderEnum.getItems().put(item.getId(), item);
                }
                {
                    var item = new EnumItemDescription("DESC");
                    sortOrderEnum.getItems().put(item.getId(), item);
                }
                registry.getEnums().put(sortOrderEnum.getId(), sortOrderEnum);
            }
            {
                var entityListColumnType = new EnumDescription("com.gridnine.platform.elsa.webApp.common.EntityListColumnType");
                {
                    var item = new EnumItemDescription("TEXT");
                    entityListColumnType.getItems().put(item.getId(), item);
                }
                {
                    var item = new EnumItemDescription("MENU");
                    entityListColumnType.getItems().put(item.getId(), item);
                }
                {
                    var item = new EnumItemDescription("CUSTOM");
                    entityListColumnType.getItems().put(item.getId(), item);
                }
                {
                    var item = new EnumItemDescription("OPTION");
                    entityListColumnType.getItems().put(item.getId(), item);
                }
                registry.getEnums().put(entityListColumnType.getId(), entityListColumnType);
            }
            {
                var notificationType = new EnumDescription("com.gridnine.platform.elsa.webApp.common.NotificationType");
                {
                    var item = new EnumItemDescription("INFO");
                    notificationType.getItems().put(item.getId(), item);
                }
                {
                    var item = new EnumItemDescription("WARN");
                    notificationType.getItems().put(item.getId(), item);
                }
                {
                    var item = new EnumItemDescription("ERROR");
                    notificationType.getItems().put(item.getId(), item);
                }
                registry.getEnums().put(notificationType.getId(), notificationType);
            }
            {
                var sort = new EntityDescription("com.gridnine.platform.elsa.webApp.common.Sort");
                {
                    var item = new StandardPropertyDescription("field");
                    item.setType(StandardValueType.STRING);
                    item.setNonNullable(true);
                    sort.getProperties().put(item.getId(), item);
                }
                {
                    var item = new StandardPropertyDescription("sortOrder");
                    item.setType(StandardValueType.ENUM);
                    item.setNonNullable(true);
                    item.setClassName("com.gridnine.platform.elsa.webApp.common.SortOrder");
                    sort.getProperties().put(item.getId(), item);
                }
                registry.getEntities().put(sort.getId(), sort);
            }
            {
                var optionEntity = new EntityDescription("com.gridnine.platform.elsa.webApp.common.Option");
                {
                    var item = new StandardPropertyDescription("id");
                    item.setType(StandardValueType.STRING);
                    item.setNonNullable(true);
                    optionEntity.getProperties().put(item.getId(), item);
                }
                {
                    var item = new StandardPropertyDescription("displayName");
                    item.setType(StandardValueType.STRING);
                    item.setNonNullable(true);
                    optionEntity.getProperties().put(item.getId(), item);
                }
                registry.getEntities().put(optionEntity.getId(), optionEntity);
            }
            {
                var entityListColumnDescription = new EntityDescription("com.gridnine.platform.elsa.webApp.common.EntityListColumnDescription");
                {
                    var item = new StandardPropertyDescription("id");
                    item.setType(StandardValueType.STRING);
                    item.setNonNullable(true);
                    entityListColumnDescription.getProperties().put(item.getId(), item);
                }
                {
                    var item = new StandardPropertyDescription("title");
                    item.setType(StandardValueType.STRING);
                    item.setNonNullable(true);
                    entityListColumnDescription.getProperties().put(item.getId(), item);
                }
                {
                    var item = new StandardPropertyDescription("columnType");
                    item.setType(StandardValueType.ENUM);
                    item.setNonNullable(true);
                    item.setClassName("com.gridnine.platform.elsa.webApp.common.EntityListColumnType");
                    entityListColumnDescription.getProperties().put(item.getId(), item);
                }
                {
                    var item = new StandardPropertyDescription("customSubtype");
                    item.setType(StandardValueType.STRING);
                    entityListColumnDescription.getProperties().put(item.getId(), item);
                }
                {
                    var item = new StandardPropertyDescription("sortable");
                    item.setType(StandardValueType.BOOLEAN);
                    item.setNonNullable(true);
                    entityListColumnDescription.getProperties().put(item.getId(), item);
                }
                registry.getEntities().put(entityListColumnDescription.getId(), entityListColumnDescription);
            }
        }

    }

    private BaseWebElementDescription processElement(XmlNode child, WebAppMetaRegistry registry) {
        String tagName = child.getName();
        String className = child.getAttribute("class-name");
        if(registry.getElements().containsKey(className)){
            return registry.getElements().get(className);
        }
        var elm = switch (tagName){
            case "modal" -> {
                var result = new ModalWebElementDescription(className);
                updateBaseProperties(result, child);
                yield result;
            }
            case "container" -> {
                var result = new ContainerWebElementDescription(className);
                updateBaseProperties(result, child);
                result.setManagedConfiguration("true".equals(child.getAttribute("managed-configuration")));
                var children = child.getFirstChild("children");
                if(children != null){
                    children.getChildren().forEach(it -> {
                        result.getChildren().put(CommonParserUtils.getIdAttribute(it), processElement(it, registry));
                    });
                }
                yield result;
            }
            case "custom-container" -> {
                var result = new CustomContainerWebElementDescription(className);
                updateBaseProperties(result, child);
                result.setManagedConfiguration("true".equals(child.getAttribute("managed-configuration")));
                var children = child.getFirstChild("children");
                if(children != null){
                    children.getChildren().forEach(it -> {
                        result.getChildren().put(CommonParserUtils.getIdAttribute(it), processElement(it, registry));
                    });
                }
                yield result;
            }
            case "button" -> {
                var result = new ButtonWebElementDescription(className);
                updateBaseProperties(result, child);
                yield result;
            }
            case "select" -> {
                var result = new SelectWebElementDescription(className);
                updateBaseProperties(result, child);
                yield result;
            }
            case "router" -> {
                var result = new RouterWebElementDescription(className);
                updateBaseProperties(result, child);
                yield result;
            }
            case "nested-router" -> {
                var result = new NestedRouterWebElementDescription(className);
                updateBaseProperties(result, child);
                yield result;
            }
            case "text-area" -> {
                var result = new TextAreaWebElementDescription(className);
                updateBaseProperties(result, child);
                yield result;
            }
            case "text-field" -> {
                var result = new TextFieldWebElementDescription(className);
                updateBaseProperties(result, child);
                yield result;
            }
            case "table" -> {
                var result = new TableWebElementDescription(className);
                result.setManagedConfiguration("true".equals(child.getAttribute("managed-configuration")));
                updateBaseProperties(result, child);
                var row = child.getFirstChild("row");
                var ett = new EntityDescription("ett");
                CommonParserUtils.fillEntityDescription(row, ett);
                result.getRow().getProperties().putAll(ett.getProperties());
                result.getRow().getCollections().putAll(ett.getCollections());
                yield result;
            }
            case "autocomplete" -> {
                var result = new AutocompleteWebElementDescription(className);
                updateBaseProperties(result, child);
                yield result;
            }
            case "label" -> {
                var result = new LabelWebElementDescription(className);
                updateBaseProperties(result, child);
                yield result;
            }
            case "custom" -> {
                var result = new CustomWebElementDescription(className);
                updateBaseProperties(result, child);
                yield result;
            }
            case "element-ref" -> {
                yield  new RefWebElementDescription(className);
            }

            default -> throw new IllegalArgumentException("unsupported tag name " + tagName);
        };
        if(!(elm instanceof RefWebElementDescription)){
            registry.getElements().put(className, elm);
        }
        return elm;
    }

    private void updateBaseProperties(BaseWebElementDescription dd, XmlNode child) {
        var state = getElementExtension(child.getFirstChild("server-managed-state"));
        if(state != null) {
            dd.getServerManagedState().getProperties().putAll(state.getProperties());
            dd.getServerManagedState().getCollections().putAll(state.getCollections());
        }
        var scs = child.getFirstChild("commands-from-server");
        if (scs != null) {
            scs.getChildren().forEach(command -> {
                var item = new WebElementCommandDescription();
                item.setId(CommonParserUtils.getIdAttribute(command));
                var ett = new CustomWebElementDescription("test");
                var ext = getElementExtension(command);
                item.getProperties().putAll(ext.getProperties());
                item.getCollections().putAll(ext.getCollections());
                dd.getCommandsFromServer().put(item.getId(), item);
            });
        }
        var ccs = child.getFirstChild("commands-from-client");
        if (ccs != null) {
            ccs.getChildren().forEach(command -> {
                var item = new WebElementCommandDescription();
                item.setId(CommonParserUtils.getIdAttribute(command));
                var ett = new CustomWebElementDescription("test");
                var ext = getElementExtension(command);
                item.getProperties().putAll(ext.getProperties());
                item.getCollections().putAll(ext.getCollections());
                dd.getCommandsFromClient().put(item.getId(), item);
            });
        }
        var ss = child.getFirstChild("services");
        if (ss != null) {
            ss.getChildren().forEach(command -> {
                var item = new ServiceDescription();
                item.setId(CommonParserUtils.getIdAttribute(command));
                var req = command.getFirstChild("request");
                if(req != null){
                    var request = new WebAppEntity();
                    var ett = getElementExtension(req);
                    request.getProperties().putAll(ett.getProperties());
                    request.getCollections().putAll(ett.getCollections());
                    item.setRequest(request);
                }
                var rsp = command.getFirstChild("response");
                var ett = getElementExtension(rsp);
                var response = new WebAppEntity();
                response.getProperties().putAll(ett.getProperties());
                response.getCollections().putAll(ett.getCollections());
                item.setResponse(response);
                dd.getServices().put(item.getId(), item);
            });
        }
        var iv = child.getFirstChild("input-value");
        if (iv != null) {
            var item = new InputDescription();
            dd.setInput(item);
            item.setType(InputType.TEXT_FIELD);
            var ext = getElementExtension(iv);
            var ett = new WebAppEntity();
            item.setValue(ett);;
            ett.getProperties().putAll(ext.getProperties());
            ett.getCollections().putAll(ext.getCollections());
        }
    }

    private WebAppEntity getElementExtension(XmlNode child) {
        if (child == null) {
            return null;
        }
        var result = new WebAppEntity();
        child.getChildren("property").forEach(prop -> {
            var id = CommonParserUtils.getIdAttribute(prop);
            var pd = new StandardPropertyDescription(id);
            pd.setClassName(prop.getAttribute("class-name"));
            pd.setNonNullable("true".equals(prop.getAttribute("non-nullable")));
            pd.setType(StandardValueType.valueOf(prop.getAttribute("type")));
            result.getProperties().put(id, pd);
        });
        child.getChildren("collection").forEach(coll -> {
            var id = CommonParserUtils.getIdAttribute(coll);
            var cd = new StandardCollectionDescription(id);
            cd.setElementClassName(coll.getAttribute("element-class-name"));
            cd.setElementType(StandardValueType.valueOf(coll.getAttribute("element-type")));
            cd.setUnique("true".equals(coll.getAttribute("unique")));
            result.getCollections().put(id, cd);
        });
        child.getChildren("map").forEach(map -> {
            var md = new StandardMapDescription(CommonParserUtils.getIdAttribute(map));
            result.getMaps().put(md.getId(), md);
            md.setKeyClassName(map.getAttribute("key-class-name"));
            md.setKeyType(StandardValueType.valueOf(map.getAttribute("key-type")));
            md.setValueClassName(map.getAttribute("value-class-name"));
            md.setValueType(StandardValueType.valueOf(map.getAttribute("key-type")));
        });
        return result;
    }

}
