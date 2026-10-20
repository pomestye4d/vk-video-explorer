/*****************************************************************
 * Gridnine AB http://www.gridnine.com
 * Project: LDocs
 *****************************************************************/

package com.gridnine.platform.elsa.gradle.meta.webApp;

public class SelectWebElementDescription extends BaseWebElementDescription {
    public SelectWebElementDescription(String className) {
        super(className);
    }
    @Override
    public WebElementType getType() {
        return WebElementType.SELECT;
    }


}
