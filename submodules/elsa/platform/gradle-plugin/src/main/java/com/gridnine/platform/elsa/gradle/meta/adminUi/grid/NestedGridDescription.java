package com.gridnine.platform.elsa.gradle.meta.adminUi.grid;

import com.gridnine.platform.elsa.gradle.meta.adminUi.AdminUiElementType;
import com.gridnine.platform.elsa.gradle.meta.adminUi.BaseAdminUiElementDescription;

import java.util.ArrayList;
import java.util.List;

public class NestedGridDescription extends BaseAdminUiElementDescription {
    private final List<GridRowDescription> rows = new ArrayList<>();

    public List<GridRowDescription> getRows() {
        return rows;
    }

    @Override
    public AdminUiElementType getType() {
        return AdminUiElementType.NESTED_GRID;
    }
}
