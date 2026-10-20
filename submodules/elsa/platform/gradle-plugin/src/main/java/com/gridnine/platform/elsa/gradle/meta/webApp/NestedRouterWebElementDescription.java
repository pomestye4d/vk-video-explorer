/*****************************************************************
 * Gridnine AB http://www.gridnine.com
 * Project: LDocs
 *****************************************************************/

package com.gridnine.platform.elsa.gradle.meta.webApp;

public class NestedRouterWebElementDescription extends BaseWebElementDescription {

    public NestedRouterWebElementDescription(String className) {
        super(className);
    }

    @Override
    public WebElementType getType() {
        return WebElementType.NESTED_ROUTER;
    }


}
