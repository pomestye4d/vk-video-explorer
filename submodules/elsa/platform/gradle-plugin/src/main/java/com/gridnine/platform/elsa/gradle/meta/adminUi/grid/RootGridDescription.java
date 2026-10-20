package com.gridnine.platform.elsa.gradle.meta.adminUi.grid;


import com.gridnine.platform.elsa.gradle.meta.adminUi.AdminUiElementType;

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
