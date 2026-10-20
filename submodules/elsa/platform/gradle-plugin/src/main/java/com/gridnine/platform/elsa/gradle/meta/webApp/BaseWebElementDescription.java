/*****************************************************************
 * Gridnine AB http://www.gridnine.com
 * Project: LDocs
 *****************************************************************/

package com.gridnine.platform.elsa.gradle.meta.webApp;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public abstract class BaseWebElementDescription {

    private String className;

    private final WebAppEntity serverManagedState = new  WebAppEntity();

    private final Map<String, WebElementCommandDescription> commandsFromServer = new LinkedHashMap<>();

    private final Map<String, WebElementCommandDescription> commandsFromClient = new LinkedHashMap<>();

    private final Map<String, ServiceDescription> services = new LinkedHashMap<>();

    private InputDescription input;

    public abstract WebElementType getType();

    public BaseWebElementDescription() {
    }

    public BaseWebElementDescription(String className) {
        this.className = className;
    }


    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public WebAppEntity getServerManagedState() {
        return serverManagedState;
    }

    public InputDescription getInput() {
        return input;
    }

    public void setInput(InputDescription input) {
        this.input = input;
    }

    public Map<String, WebElementCommandDescription> getCommandsFromServer() {
        return commandsFromServer;
    }

    public Map<String, WebElementCommandDescription> getCommandsFromClient() {
        return commandsFromClient;
    }

    public Map<String, ServiceDescription> getServices() {
        return services;
    }
}
