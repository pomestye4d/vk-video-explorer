/*****************************************************************
 * Gridnine AB http://www.gridnine.com
 * Project: LDocs
 *****************************************************************/

package com.gridnine.platform.elsa.gradle.meta.webApp;

public class TextFieldWebElementDescription extends BaseWebElementDescription {
    public TextFieldWebElementDescription(String className) {
        super(className);
    }
    @Override
    public WebElementType getType() {
        return WebElementType.TEXT_FIELD;
    }


}
