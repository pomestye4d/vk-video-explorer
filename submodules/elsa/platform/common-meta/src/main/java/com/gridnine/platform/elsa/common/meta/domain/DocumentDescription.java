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

package com.gridnine.platform.elsa.common.meta.domain;

import com.gridnine.platform.elsa.common.meta.common.EntityDescription;

public class DocumentDescription extends EntityDescription {

    private boolean cacheResolve;

    private boolean cacheCaption;

    private String captionExpression;

    private String localizableCaptionExpression;

    public DocumentDescription() {
    }

    public DocumentDescription(String id) {
        super(id);
    }

    public boolean isCacheResolve() {
        return cacheResolve;
    }

    public void setCacheResolve(boolean cacheResolve) {
        this.cacheResolve = cacheResolve;
    }

    public boolean isCacheCaption() {
        return cacheCaption;
    }

    public void setCacheCaption(boolean cacheCaption) {
        this.cacheCaption = cacheCaption;
    }

    public void setLocalizableCaptionExpression(String localizableCaptionExpression) {
        this.localizableCaptionExpression = localizableCaptionExpression;
    }

    public String getLocalizableCaptionExpression() {
        return localizableCaptionExpression;
    }

    public void setCaptionExpression(String captionExpression) {
        this.captionExpression = captionExpression;
    }

    public String getCaptionExpression() {
        return captionExpression;
    }
}
