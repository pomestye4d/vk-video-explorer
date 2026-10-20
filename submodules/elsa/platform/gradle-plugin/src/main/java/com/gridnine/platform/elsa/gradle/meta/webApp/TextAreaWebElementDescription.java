/*****************************************************************
 * Gridnine AB http://www.gridnine.com
 * Project: LDocs
 *****************************************************************/

package com.gridnine.platform.elsa.gradle.meta.webApp;

public class TextAreaWebElementDescription extends BaseWebElementDescription {

    public TextAreaWebElementDescription(String className) {
        super(className);
    }

    @Override
    public WebElementType getType() {
        return WebElementType.TEXT_AREA;
    }


}
