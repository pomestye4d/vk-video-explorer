package com.gridnine.platform.elsa.common.meta.adminUi.grid;


import com.gridnine.platform.elsa.common.meta.adminUi.AdminUiElementType;
import com.gridnine.platform.elsa.common.meta.adminUi.BaseAdminUiElementDescription;

import java.util.ArrayList;
import java.util.List;

public class RootGridDescription extends NestedGridDescription {
    private String className;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public AdminUiElementType getType() {
        return AdminUiElementType.ROOT_GRID;
    }
}
