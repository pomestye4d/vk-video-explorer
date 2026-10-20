/*****************************************************************
 * Gridnine AB http://www.gridnine.com
 * Project: LDocs
 *****************************************************************/

package com.gridnine.platform.elsa.gradle.meta.webApp;

public class AutocompleteWebElementDescription extends BaseWebElementDescription {

    public AutocompleteWebElementDescription(String className) {
        super(className);
    }
    @Override
    public WebElementType getType() {
        return WebElementType.AUTOCOMPLETE;
    }


}
