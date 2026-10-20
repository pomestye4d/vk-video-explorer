/*****************************************************************
 * Gridnine AB http://www.gridnine.com
 * Project: LDocs
 *****************************************************************/

package com.gridnine.platform.elsa.gradle.meta.webApp;

public class RefWebElementDescription extends BaseWebElementDescription{
    public RefWebElementDescription(String className) {
        super(className);
    }
    @Override
    public WebElementType getType() {
        return WebElementType.REF;
    }
}
