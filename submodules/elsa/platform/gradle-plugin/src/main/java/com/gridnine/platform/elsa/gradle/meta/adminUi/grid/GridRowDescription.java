package com.gridnine.platform.elsa.gradle.meta.adminUi.grid;

import java.util.ArrayList;
import java.util.List;

public class GridRowDescription {
    private final List<GridColumnDescription> columns = new ArrayList<>();

    public List<GridColumnDescription> getColumns() {
        return columns;
    }
}
