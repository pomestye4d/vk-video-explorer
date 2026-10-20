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

package com.gridnine.platform.elsa.core.storage;

import com.gridnine.platform.elsa.common.core.model.common.HasPriority;
import com.gridnine.platform.elsa.common.core.model.domain.BaseAsset;
import com.gridnine.platform.elsa.common.core.model.domain.BaseDocument;

@SuppressWarnings("RedundantThrows")
public interface StorageInterceptor extends HasPriority {

    default boolean isApplicableOnSave(SaveAssetParameters params){
        return !params.isSkipInterceptors();
    }

    default boolean isApplicableOnSave(SaveDocumentParameters params){
        return !params.isSkipInterceptors();
    }

    default boolean isApplicableOnDelete(DeleteAssetParameters params){
        return !params.isSkipInterceptors();
    }

    default boolean isApplicableOnDelete(DeleteDocumentParameters params){
        return !params.isSkipInterceptors();
    }


    default <A extends BaseAsset> void onSave(A asset, OperationContext<A> context) throws Exception {
    }

    default <A extends BaseAsset> void onDelete(A oldAssset, OperationContext<A> operationContext) throws Exception {
    }

    default <D extends BaseDocument> void onSave(D doc, OperationContext<D> context) throws Exception {
    }

    default <D extends BaseDocument> void onDelete(D document, OperationContext<D> operationContext) throws Exception {
    }


}
