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

import com.gridnine.platform.elsa.gradle.meta.common.EntityDescription;
import com.gridnine.platform.elsa.gradle.meta.common.StandardCollectionDescription;
import com.gridnine.platform.elsa.gradle.meta.common.StandardPropertyDescription;
import com.gridnine.platform.elsa.gradle.meta.common.StandardValueType;
import com.gridnine.platform.elsa.gradle.meta.webApp.*;
import com.gridnine.platform.elsa.gradle.utils.BuildTextUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WebAppMetadataHelper {
    public static CustomWebElementDescription toCustomEntity(BaseWebElementDescription element) {
        var result = new CustomWebElementDescription(element.getClassName());
        result.getServerManagedState().getProperties().putAll(element.getServerManagedState().getProperties());
        result.getServerManagedState().getCollections().putAll(element.getServerManagedState().getCollections());
        element.getCommandsFromClient().forEach((key, value) -> {
            result.getCommandsFromClient().put(key, value);
        });
        element.getCommandsFromServer().forEach((key, value) -> {
            result.getCommandsFromServer().put(key, value);
        });
        result.setInput(element.getInput());
        element.getServices().forEach((key, value) -> {
            result.getServices().put(key, value);
        });
        switch (element.getType()) {
            case MODAL -> {
                {
                    var command = new WebElementCommandDescription();
                    command.setId("notify");
                    {
                        var prop = new StandardPropertyDescription();
                        prop.setId("type");
                        prop.setType(StandardValueType.ENUM);
                        prop.setNonNullable(true);
                        prop.setClassName("com.gridnine.platform.elsa.webApp.common.NotificationType");
                        command.getProperties().put(prop.getId(), prop);
                    }
                    {
                        var prop = new StandardPropertyDescription();
                        prop.setId("message");
                        prop.setType(StandardValueType.STRING);
                        prop.setNonNullable(true);
                        command.getProperties().put(prop.getId(), prop);
                    }
                    result.getCommandsFromServer().put(command.getId(), command);
                }
                {
                    var prop = new StandardPropertyDescription();
                    prop.setType(StandardValueType.INT);
                    prop.setId("notificationDuration");
                    result.getServerManagedState().getProperties().put(prop.getId(), prop);
                }
                {
                    var prop = new StandardPropertyDescription();
                    prop.setType(StandardValueType.BOOLEAN);
                    prop.setId("visible");
                    prop.setNonNullable(true);
                    result.getServerManagedState().getProperties().put(prop.getId(), prop);
                }
                {
                    var prop = new StandardPropertyDescription();
                    prop.setId("title");
                    prop.setType(StandardValueType.STRING);
                    prop.setNonNullable(true);
                    result.getServerManagedState().getProperties().put(prop.getId(), prop);
                }
                {
                    var command = new WebElementCommandDescription();
                    command.setId("close");
                    result.getCommandsFromClient().put(command.getId(), command);
                }
            }
            case CONTAINER -> {
                var prop = new StandardPropertyDescription();
                prop.setType(StandardValueType.ENUM);
                prop.setNonNullable(true);
                prop.setId("flexDirection");
                prop.setClassName("com.gridnine.platform.elsa.webApp.common.FlexDirection");
                result.getServerManagedState().getProperties().put(prop.getId(), prop);
            }
            case BUTTON -> {
                ButtonWebElementDescription bb = (ButtonWebElementDescription) element;
                {
                    var command = new WebElementCommandDescription();
                    command.setId("click");
                    result.getCommandsFromClient().put(command.getId(), command);
                }
                {
                    var prop = new StandardPropertyDescription();
                    prop.setType(StandardValueType.STRING);
                    prop.setId("title");
                    result.getServerManagedState().getProperties().put(prop.getId(), prop);
                }
                {
                    var prop = new StandardPropertyDescription();
                    prop.setType(StandardValueType.STRING);
                    prop.setId("icon");
                    result.getServerManagedState().getProperties().put(prop.getId(), prop);
                }
                {
                    var prop = new StandardPropertyDescription();
                    prop.setType(StandardValueType.BOOLEAN);
                    prop.setId("disabled");
                    result.getServerManagedState().getProperties().put(prop.getId(), prop);
                }
            }
            case SELECT -> {
                SelectWebElementDescription sd = (SelectWebElementDescription) element;
                {
                    var input = new InputDescription();
                    result.setInput(input);
                    input.setType(InputType.SELECT);
                    var value = new WebAppEntity();
                    input.setValue(value);
                    var prop = new StandardCollectionDescription();
                    prop.setId("values");
                    prop.setElementType(StandardValueType.STRING);
                    value.getCollections().put(prop.getId(), prop);
                }
                {
                    var options = new StandardCollectionDescription();
                    options.setElementType(StandardValueType.ENTITY);
                    options.setId("options");
                    options.setElementClassName("com.gridnine.platform.elsa.webApp.common.Option");
                    result.getServerManagedState().getCollections().put(options.getId(), options);
                }
                {
                    var multiple = new StandardPropertyDescription();
                    multiple.setType(StandardValueType.BOOLEAN);
                    multiple.setId("multiple");
                    result.getServerManagedState().getProperties().put(multiple.getId(), multiple);
                }
                addDeferred(sd, result);
            }
            case ROUTER -> {
                RouterWebElementDescription rd = (RouterWebElementDescription) element;
                {
                    var path = new StandardPropertyDescription();
                    path.setType(StandardValueType.STRING);
                    path.setId("path");
                    path.setNonNullable(true);
                    result.getServerManagedState().getProperties().put(path.getId(), path);
                }
                {
                    var hasChanges = new StandardPropertyDescription();
                    hasChanges.setType(StandardValueType.BOOLEAN);
                    hasChanges.setId("hasChanges");
                    result.getServerManagedState().getProperties().put(hasChanges.getId(), hasChanges);
                }
                {
                    var confirmMessage = new StandardPropertyDescription();
                    confirmMessage.setType(StandardValueType.STRING);
                    confirmMessage.setId("confirmMessage");
                    result.getServerManagedState().getProperties().put(confirmMessage.getId(), confirmMessage);
                }
            }
            case NESTED_ROUTER -> {
            }
            case TEXT_AREA -> {
                var tad = (TextAreaWebElementDescription) element;
                addValue(result, InputType.TEXT_AREA, StandardValueType.STRING);
                addValidation(tad, result);
                addDeferred(tad, result);
            }
            case TEXT_FIELD -> {
                var tfd = (TextFieldWebElementDescription) element;
                addValue(result, InputType.TEXT_FIELD, StandardValueType.STRING);
                addValidation(tfd, result);
                addDeferred(tfd, result);
                addDebounceTime(tfd, result);
            }
            case TABLE -> {
                var td = (TableWebElementDescription) element;
                {
                    var columns = new StandardCollectionDescription();
                    columns.setElementType(StandardValueType.ENTITY);
                    columns.setId("columns");
                    columns.setElementClassName("com.gridnine.platform.elsa.webApp.common.EntityListColumnDescription");
                    result.getServerManagedState().getCollections().put(columns.getId(), columns);
                }
                {
                    var data = new StandardCollectionDescription();
                    data.setElementType(StandardValueType.ENTITY);
                    data.setId("data");
                    data.setElementClassName("%sRow".formatted(td.getClassName()));
                    result.getServerManagedState().getCollections().put(data.getId(), data);
                }
                {
                    var sort = new StandardPropertyDescription();
                    sort.setType(StandardValueType.ENTITY);
                    sort.setId("sort");
                    sort.setClassName("com.gridnine.platform.elsa.webApp.common.Sort");
                    result.getServerManagedState().getProperties().put(sort.getId(), sort);
                }
                {
                    var loading = new StandardPropertyDescription();
                    loading.setType(StandardValueType.BOOLEAN);
                    loading.setId("loading");
                    result.getServerManagedState().getProperties().put(loading.getId(), loading);
                }
                {
                    var action = new WebElementCommandDescription();
                    result.getCommandsFromClient().put(action.getId(), action);
                    action.setId("action");
                    {
                        var arg = new  StandardPropertyDescription();
                        arg.setType(StandardValueType.STRING);
                        arg.setId("rowId");
                        arg.setNonNullable(true);
                        action.getProperties().put(arg.getId(), arg);
                    }
                    {
                        var arg = new  StandardPropertyDescription();
                        arg.setType(StandardValueType.STRING);
                        arg.setId("columnId");
                        arg.setNonNullable(true);
                        action.getProperties().put(arg.getId(), arg);
                    }
                    {
                        var arg = new  StandardPropertyDescription();
                        arg.setType(StandardValueType.STRING);
                        arg.setId("actionId");
                        arg.setNonNullable(true);
                        action.getProperties().put(arg.getId(), arg);
                    }
                    result.getCommandsFromClient().put(action.getId(), action);
                }
                {
                    var sort = new WebElementCommandDescription();
                    result.getCommandsFromClient().put(sort.getId(), sort);
                    sort.setId("sort");
                    {
                        var arg = new  StandardPropertyDescription();
                        arg.setType(StandardValueType.ENTITY);
                        arg.setId("sort");
                        arg.setClassName("com.gridnine.platform.elsa.webApp.common.Sort");
                        arg.setNonNullable(true);
                        sort.getProperties().put(arg.getId(), arg);
                    }

                }
                {
                    var loadMore = new WebElementCommandDescription();
                    loadMore.setId("loadMore");
                    result.getCommandsFromClient().put(loadMore.getId(), loadMore);
                }
                {
                    var refreshData = new WebElementCommandDescription();
                    refreshData.setId("refreshData");
                    result.getCommandsFromClient().put(refreshData.getId(), refreshData);
                }
                {
                    var refreshData = new WebElementCommandDescription();
                    refreshData.setId("refreshData");
                    result.getCommandsFromServer().put(refreshData.getId(), refreshData);
                }
            }
            case AUTOCOMPLETE -> {
                AutocompleteWebElementDescription sd = (AutocompleteWebElementDescription) element;
                {
                    var input = new InputDescription();
                    result.setInput(input);
                    input.setType(InputType.SELECT);
                    var value = new WebAppEntity();
                    input.setValue(value);
                    var prop = new StandardCollectionDescription();
                    prop.setId("values");
                    prop.setElementType(StandardValueType.ENTITY);
                    prop.setElementClassName("com.gridnine.platform.elsa.webApp.common.Option");
                    value.getCollections().put(prop.getId(), prop);
                }
                {
                    var multiple = new StandardPropertyDescription();
                    multiple.setType(StandardValueType.BOOLEAN);
                    multiple.setId("multiple");
                    result.getServerManagedState().getProperties().put(multiple.getId(), multiple);
                }
                addDeferred(sd, result);
                addDebounceTime(sd, result);
                addValidation(sd, result);
                {
                    var limit = new StandardPropertyDescription();
                    limit.setType(StandardValueType.INT);
                    limit.setId("limit");
                    limit.setNonNullable(true);
                    result.getServerManagedState().getProperties().put(limit.getId(), limit);
                }
                {
                    var getDataService = new ServiceDescription();
                    getDataService.setId("getData");
                    var request = new WebAppEntity();
                    {
                         var prop = new StandardPropertyDescription();
                         prop.setType(StandardValueType.STRING);
                         prop.setId("query");
                         request.getProperties().put(prop.getId(), prop);
                    }
                    {
                        var prop = new StandardPropertyDescription();
                        prop.setType(StandardValueType.INT);
                        prop.setId("limit");
                        prop.setNonNullable(true);
                        request.getProperties().put(prop.getId(), prop);
                    }
                    getDataService.setRequest(request);
                    var response = new WebAppEntity();
                    {
                        var coll = new StandardCollectionDescription();
                        coll.setElementType(StandardValueType.ENTITY);
                        coll.setElementClassName("com.gridnine.platform.elsa.webApp.common.Option");
                        coll.setId("items");
                        response.getCollections().put(coll.getId(), coll);
                    }
                    getDataService.setResponse(response);
                    result.getServices().put(getDataService.getId(), getDataService);
                }
            }
            case LABEL -> {
                var tad = (LabelWebElementDescription) element;
                var prop = new StandardPropertyDescription();
                prop.setId("title");
                prop.setType(StandardValueType.STRING);
                result.getServerManagedState().getProperties().put(prop.getId(), prop);
            }
            case CUSTOM -> {
                //noops
            }
        }
        return result;
    }

    private static void addValue(CustomWebElementDescription result, InputType inputType, StandardValueType standardValueType) {
        var input = new InputDescription();
        result.setInput(input);
        input.setType(inputType);
        var value = new WebAppEntity();
        input.setValue(value);
        var prop = new StandardPropertyDescription();
        prop.setId("value");
        prop.setType(standardValueType);
        value.getProperties().put(prop.getId(), prop);
    }

    private static void addValidation(BaseWebElementDescription tad, CustomWebElementDescription result) {
        var prop = new StandardPropertyDescription();
        prop.setId("validationMessage");
        prop.setType(StandardValueType.STRING);
        prop.setNonNullable(false);
        result.getServerManagedState().getProperties().put(prop.getId(), prop);
    }

    private static void addDebounceTime(BaseWebElementDescription sd, CustomWebElementDescription ce) {
        var prop = new StandardPropertyDescription();
        prop.setId("debounceTime");
        prop.setType(StandardValueType.INT);
        prop.setNonNullable(false);
        ce.getServerManagedState().getProperties().put(prop.getId(), prop);
    }

    private static void addDeferred(BaseWebElementDescription sd, CustomWebElementDescription ce) {
        var prop = new StandardPropertyDescription();
        prop.setId("deferred");
        prop.setType(StandardValueType.BOOLEAN);
        prop.setNonNullable(false);
        ce.getServerManagedState().getProperties().put(prop.getId(), prop);
    }
    public static EntityDescription getInputValueDescription(BaseWebElementDescription element) {
        WebAppEntity value = element.getInput().getValue();
        var cn = "%sInputValue".formatted(element.getClassName());
        var ed = new EntityDescription(cn);
        ed.getProperties().putAll(value.getProperties());
        ed.getCollections().putAll(value.getCollections());
        return ed;
    }
    public static List<EntityDescription> getCommandsDescription(BaseWebElementDescription element) {
        var result = new  ArrayList<EntityDescription>();
        var lst = new ArrayList<WebElementCommandDescription>();
        var ce = toCustomEntity(element);
        lst.addAll(ce.getCommandsFromClient().values());
        lst.addAll(ce.getCommandsFromServer().values());
        for(var command :lst) {
            if(command.getProperties().isEmpty() && command.getCollections().isEmpty()){
                continue;
            }
            var cn = "%s%sAction".formatted(ce.getClassName(),BuildTextUtils.capitalize(command.getId()));
            var ed = new EntityDescription(cn);
            ed.getProperties().putAll(command.getProperties());
            ed.getCollections().putAll(command.getCollections());
            result.add(ed);
        }
        return result;
    }
    public static List<EntityDescription> getServicesClasses(BaseWebElementDescription element) {
        var result = new  ArrayList<EntityDescription>();
        var ce = toCustomEntity(element);
        for(var service :ce.getServices().values()) {
            if(service.getRequest() != null){
                var cn = "%s%sRequest".formatted(element.getClassName(),BuildTextUtils.capitalize(service.getId()));
                var ed = new EntityDescription(cn);
                ed.getProperties().putAll(service.getRequest().getProperties());
                ed.getCollections().putAll(service.getRequest().getCollections());
                result.add(ed);
            }
            var cn = "%s%sResponse".formatted(element.getClassName(),BuildTextUtils.capitalize(service.getId()));
            var ed = new EntityDescription(cn);
            ed.getProperties().putAll(service.getResponse().getProperties());
            ed.getCollections().putAll(service.getResponse().getCollections());
            result.add(ed);
        }
        return result;
    }
    public static EntityDescription getConfigurationDescription(BaseWebElementDescription element) {
        var ce = toCustomEntity(element);
        var cn = "%sConfiguration".formatted(ce.getClassName());
        var ed = new EntityDescription(cn);
        ed.getParameters().put("no-serialization", "true");
        ed.getParameters().put("no-equals", "true");
        ed.getProperties().putAll(ce.getServerManagedState().getProperties());
        ed.getCollections().putAll(ce.getServerManagedState().getCollections());
        for(var cmd: ce.getCommandsFromClient().values()){
            var prop = new StandardPropertyDescription();
            prop.setType(StandardValueType.ENTITY);
            prop.setId("%sListener".formatted(cmd.getId()));
            if(cmd.getProperties().isEmpty() && cmd.getCollections().isEmpty()){
                prop.setClassName("com.gridnine.platform.elsa.common.core.model.common.RunnableWithExceptionAndArgument<com.gridnine.webpeer.core.ui.OperationUiContext>");
            } else {
                prop.setClassName("com.gridnine.platform.elsa.common.core.model.common.RunnableWithExceptionAnd2Arguments<%s%sAction, com.gridnine.webpeer.core.ui.OperationUiContext>".formatted(element.getClassName(), BuildTextUtils.capitalize(cmd.getId())));
            }
            ed.getProperties().put(prop.getId(), prop);
        }
        if(ce.getInput() != null){
            if(ce.getInput().getValue().getProperties().size()+ce.getInput().getValue().getCollections().size() > 1) {
                var inputValueClass = "%sInputValue".formatted(ce.getClassName());
                {
                    var prop = new StandardPropertyDescription();
                    prop.setType(StandardValueType.ENTITY);
                    prop.setId("valueChangeListener");
                    prop.getParameters().put("no-equals", "true");
                    prop.setClassName("com.gridnine.platform.elsa.webApp.WebAppValueChangeListener<%s>".formatted(inputValueClass));
                    ed.getProperties().put(prop.getId(), prop);
                }
                {
                    var prop = new StandardPropertyDescription();
                    prop.setType(StandardValueType.ENTITY);
                    prop.setId("value");
                    prop.setClassName(inputValueClass);
                    ed.getProperties().put(prop.getId(), prop);
                }
            } else {
                var sid = getSimpleInputValueDescription(ce.getInput());
                {
                    var prop = new StandardPropertyDescription();
                    prop.setType(StandardValueType.ENTITY);
                    prop.setId("valueChangeListener");
                    prop.getParameters().put("no-equals", "true");
                    if(sid.collection) {
                        prop.setClassName("com.gridnine.platform.elsa.webApp.WebAppValueChangeListener<java.util.List<%s>>".formatted(sid.valueClassName));
                    } else {
                        prop.setClassName("com.gridnine.platform.elsa.webApp.WebAppValueChangeListener<%s>".formatted(sid.valueClassName));
                    }
                    ed.getProperties().put(prop.getId(), prop);
                }
                if(sid.collection){
                    ed.getCollections().put(sid.coll.getId(), sid.coll);
                } else {
                    ed.getProperties().put(sid.prop.getId(), sid.prop);
                }
            }
        }
        for(var service: ce.getServices().values()){
            var prop = new StandardPropertyDescription();
            prop.setType(StandardValueType.ENTITY);
            prop.setId("%sServiceHandler".formatted(service.getId()));
            prop.getParameters().put("no-equals", "true");
            var requestClassName = "%s%sRequest".formatted(element.getClassName(), BuildTextUtils.capitalize(service.getId()));
            var responseClassName = "%s%sResponse".formatted(element.getClassName(), BuildTextUtils.capitalize(service.getId()));
            prop.setClassName("com.gridnine.platform.elsa.webApp.WebAppServiceHandler<%s, %s>".formatted(requestClassName, responseClassName));
            ed.getProperties().put(prop.getId(), prop);
        }
        if(element instanceof WebElementWithChildren hasChildren){
            for(var entry: hasChildren.getChildren().entrySet()){
                var child = entry.getValue();
                boolean managed = isManagedConfiguration(child);
                if(managed){
                    var prop = new StandardPropertyDescription();
                    prop.setType(StandardValueType.ENTITY);
                    prop.setId(entry.getKey());
                    prop.setClassName("%sConfiguration".formatted(child.getClassName()));
                    ed.getProperties().put(prop.getId(), prop);
                }
            }
        }
        if(element instanceof NestedRouterWebElementDescription){
            var prop = new StandardPropertyDescription();
            prop.setType(StandardValueType.STRING);
            prop.setId("path");
            ed.getProperties().put(prop.getId(), prop);
        }
        return ed;
    }



    public static boolean isManagedConfiguration(BaseWebElementDescription child) {
        return switch (child.getType()) {
            case CONTAINER -> {
                var ctr = (ContainerWebElementDescription)child;
                yield ctr.isManagedConfiguration();
            }
            case CUSTOM_CONTAINER -> {
                var ctr = (CustomContainerWebElementDescription)child;
                yield ctr.isManagedConfiguration();
            }
            case  TABLE -> {
                var ctr = (TableWebElementDescription)child;
                yield ctr.isManagedConfiguration();
            }
            case NESTED_ROUTER,ROUTER -> false;
            default -> true;
        };
    }

    public static String getPropertyType(StandardValueType type, String className) {
        return switch (type) {
            case STRING, CLASS -> "String";
            case LOCAL_DATE -> {
                yield LocalDate.class.getName();
            }
            case LOCAL_DATE_TIME -> {
                yield LocalDateTime.class.getName();
            }
            case INSTANT -> {
                yield Instant.class.getName();
            }
            case BOOLEAN -> "Boolean";
            case ENUM, ENTITY -> {
                yield className;
            }
            case LONG -> "Long";
            case INT -> "Integer";
            case BIG_DECIMAL -> {
                yield BigDecimal.class.getName();
            }
            default -> throw new IllegalStateException("Unknown property type: " + type);
        };
    }

    public static SimpleInputValueDescription getSimpleInputValueDescription(InputDescription ed){
        boolean collection = false;
        var valueClassName = "";
        StandardPropertyDescription prop = null;
        StandardCollectionDescription coll = null;
        if (ed.getValue().getProperties().size()==1){
            var pr = ed.getValue().getProperties().values().iterator().next();
            valueClassName = getPropertyType(pr.getType(), pr.getClassName());
            {
                prop = new StandardPropertyDescription();
                prop.setType(pr.getType());
                prop.setId("value");
                prop.setClassName(pr.getClassName());
                prop.setNonNullable(pr.isNonNullable());
            }
        } else {
            collection = true;
            var cl = ed.getValue().getCollections().values().iterator().next();
            valueClassName = getPropertyType(cl.getElementType(), cl.getElementClassName());
            {
                coll = new StandardCollectionDescription();
                coll.setElementType(cl.getElementType());
                coll.setId("value");
                coll.setElementClassName(cl.getElementClassName());;
            }
        }
        return new SimpleInputValueDescription(collection, valueClassName, prop, coll);
    }
    public record SimpleInputValueDescription(boolean collection, String valueClassName, StandardPropertyDescription prop, StandardCollectionDescription coll) {

    }

}
