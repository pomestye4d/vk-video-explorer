/*****************************************************************
 * Gridnine AB http://www.gridnine.com
 * Project: LDocs
 *****************************************************************/

package com.gridnine.platform.elsa.gradle.meta.domain;

import com.gridnine.platform.elsa.gradle.meta.common.BaseModelElementDescription;

import java.util.ArrayList;
import java.util.List;

public class VirtualAssetDescription extends BaseModelElementDescription {
    private String baseAsset;
    private String includedFields;
    private String excludedFields;
    private final List<JoinDescription> joins = new ArrayList<>();

    public VirtualAssetDescription(String id) {
        super(id);
    }

    public String getBaseAsset() {
        return baseAsset;
    }

    public void setBaseAsset(String baseAsset) {
        this.baseAsset = baseAsset;
    }

    public String getIncludedFields() {
        return includedFields;
    }

    public void setIncludedFields(String includedFields) {
        this.includedFields = includedFields;
    }

    public String getExcludedFields() {
        return excludedFields;
    }

    public void setExcludedFields(String excludedFields) {
        this.excludedFields = excludedFields;
    }

    public List<JoinDescription> getJoins() {
        return joins;
    }
}
