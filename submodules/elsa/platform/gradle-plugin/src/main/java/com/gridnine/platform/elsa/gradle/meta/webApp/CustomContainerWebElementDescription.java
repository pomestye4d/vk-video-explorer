/*****************************************************************
 * Gridnine AB http://www.gridnine.com
 * Project: LDocs
 *****************************************************************/

package com.gridnine.platform.elsa.gradle.meta.webApp;

import java.util.LinkedHashMap;
import java.util.Map;

public class CustomContainerWebElementDescription extends BaseWebElementDescription implements WebElementWithChildren {

    private boolean managedConfiguration;

    private final Map<String , BaseWebElementDescription> children = new LinkedHashMap<>();

    public CustomContainerWebElementDescription(String className) {
        super(className);
    }

    @Override
    public Map<String, BaseWebElementDescription> getChildren() {
        return children;
    }

    @Override
    public WebElementType getType() {
        return WebElementType.CUSTOM_CONTAINER;
    }

    public void setManagedConfiguration(boolean managedConfiguration) {
        this.managedConfiguration = managedConfiguration;
    }

    public boolean isManagedConfiguration() {
        return managedConfiguration;
    }
}
