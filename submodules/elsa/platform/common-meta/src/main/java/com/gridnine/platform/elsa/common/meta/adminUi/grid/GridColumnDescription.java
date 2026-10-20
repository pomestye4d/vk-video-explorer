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

package com.gridnine.platform.elsa.common.meta.adminUi.grid;


import com.gridnine.platform.elsa.common.meta.adminUi.BaseAdminUiElementDescription;

public class GridColumnDescription {
    private BaseAdminUiElementDescription content;
    private int smallWidth;
    private int standardWidth;
    private int largeWidth;

    public int getSmallWidth() {
        return smallWidth;
    }

    public void setSmallWidth(int smallWidth) {
        this.smallWidth = smallWidth;
    }

    public int getStandardWidth() {
        return standardWidth;
    }

    public void setStandardWidth(int standardWidth) {
        this.standardWidth = standardWidth;
    }

    public int getLargeWidth() {
        return largeWidth;
    }

    public void setLargeWidth(int largeWidth) {
        this.largeWidth = largeWidth;
    }

    public void setContent(BaseAdminUiElementDescription content) {
        this.content = content;
    }

    public BaseAdminUiElementDescription getContent() {
        return content;
    }
}
