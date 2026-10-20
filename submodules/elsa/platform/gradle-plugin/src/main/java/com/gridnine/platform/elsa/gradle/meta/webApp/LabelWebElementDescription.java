/*****************************************************************
 * Gridnine AB http://www.gridnine.com
 * Project: LDocs
 *****************************************************************/

package com.gridnine.platform.elsa.gradle.meta.webApp;

public class LabelWebElementDescription extends BaseWebElementDescription {

    public LabelWebElementDescription(String className) {
        super(className);
    }

    @Override
    public WebElementType getType() {
        return WebElementType.LABEL;
    }


}
