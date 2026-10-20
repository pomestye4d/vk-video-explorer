/*
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.gridnine.platform.elsa.common.meta.adminUi.standalone;

import com.gridnine.platform.elsa.common.meta.adminUi.AdminUiElementType;

import java.util.ArrayList;
import java.util.List;

public class StandaloneEditableTableDescription extends BaseAdminUiStandaloneComponentDescription {

    private String selectionMode;

    private String containerClassName;

    private final List<EditableTableColumnDescription> columns = new ArrayList<>();

    private StandaloneEditableTableSummaryDescription summaryDescription;

    public String getSelectionMode() {
        return selectionMode;
    }

    public void setSelectionMode(String selectionMode) {
        this.selectionMode = selectionMode;
    }

    public String getContainerClassName() {
        return containerClassName;
    }

    public void setContainerClassName(String containerClassName) {
        this.containerClassName = containerClassName;
    }

    public List<EditableTableColumnDescription> getColumns() {
        return columns;
    }

    public StandaloneEditableTableSummaryDescription getSummaryDescription() {
        return summaryDescription;
    }

    public void setSummaryDescription(StandaloneEditableTableSummaryDescription summaryDescription) {
        this.summaryDescription = summaryDescription;
    }

    @Override
    public AdminUiElementType getType() {
        return AdminUiElementType.STANDALONE_EDITABLE_TABLE;
    }
}
