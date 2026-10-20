/*****************************************************************
 * Gridnine AB http://www.gridnine.com
 * Project: LDocs
 *****************************************************************/

package com.gridnine.platform.elsa.gradle.meta.webApp;

public class TableWebElementDescription extends BaseWebElementDescription {

    private boolean managedConfiguration;

    private final WebAppEntity row = new WebAppEntity();

    public TableWebElementDescription(String className) {
        super(className);
    }

    public boolean isManagedConfiguration() {
        return managedConfiguration;
    }

    public void setManagedConfiguration(boolean managedConfiguration) {
        this.managedConfiguration = managedConfiguration;
    }

    @Override
    public WebElementType getType() {
        return WebElementType.TABLE;
    }

    public WebAppEntity getRow() {
        return row;
    }
}
