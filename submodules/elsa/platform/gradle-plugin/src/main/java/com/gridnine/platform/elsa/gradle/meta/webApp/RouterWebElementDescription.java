/*****************************************************************
 * Gridnine AB http://www.gridnine.com
 * Project: LDocs
 *****************************************************************/

package com.gridnine.platform.elsa.gradle.meta.webApp;

public class RouterWebElementDescription extends BaseWebElementDescription {

    public RouterWebElementDescription(String className) {
        super(className);
    }

    @Override
    public WebElementType getType() {
        return WebElementType.ROUTER;
    }


}
