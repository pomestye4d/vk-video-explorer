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

package com.gridnine.platform.elsa.server.core.cache;

import com.gridnine.platform.elsa.common.core.model.common.CallableWithExceptionAnd3Arguments;
import com.gridnine.platform.elsa.common.core.model.common.CallableWithExceptionAnd4Arguments;
import com.gridnine.platform.elsa.common.core.model.domain.BaseAsset;
import com.gridnine.platform.elsa.common.core.model.domain.BaseDocument;
import com.gridnine.platform.elsa.common.core.model.domain.BaseSearchableProjection;
import com.gridnine.platform.elsa.common.core.model.domain.EntityReference;
import com.gridnine.platform.elsa.common.core.search.ArgumentType;
import com.gridnine.platform.elsa.common.core.search.EqualitySupport;
import com.gridnine.platform.elsa.common.core.search.FieldNameSupport;
import com.gridnine.platform.elsa.core.storage.StorageAdvice;

import java.util.Set;

public class InvocationCountAdvice implements StorageAdvice {
    private int loadDocumentCount = 0;

    private int loadAssetCount = 0;

    private int findAssetCount = 0;

    private int findDocumentCount = 0;

    private int getAllDocumentsCount = 0;

    @Override
    public double getPriority() {
        return 30;
    }

    @Override
    public <T, D extends BaseDocument, I extends BaseSearchableProjection<D>, E extends FieldNameSupport & EqualitySupport & ArgumentType<T>>
    Set<EntityReference<D>> onGetAllDocumentReferences(Class<I> projClass, E property, T propertyValue,
                                                       CallableWithExceptionAnd3Arguments<Set<EntityReference<D>>, Class<I>, E, T> callback) throws Exception {
        getAllDocumentsCount++;
        return callback.call(projClass, property, propertyValue);
    }

    @Override
    public <T, D extends BaseDocument, I extends BaseSearchableProjection<D>, E extends FieldNameSupport & EqualitySupport & ArgumentType<T>> EntityReference<D>
    onFindUniqueDocumentReference(Class<I> projClass, E property, T propertyValue, CallableWithExceptionAnd3Arguments<EntityReference<D>, Class<I>, E, T> callback) throws Exception {
        findDocumentCount++;
        return callback.call(projClass, property, propertyValue);
    }

    @Override
    public <T, A extends BaseAsset, E extends FieldNameSupport & EqualitySupport & ArgumentType<T>> A onFindUniqueAsset(Class<A> cls, E property, T propertyValue,
                                                                                                                        boolean forModification, CallableWithExceptionAnd4Arguments<A, Class<A>, E, T, Boolean> callbackObject) throws Exception {
        findAssetCount++;
        return callbackObject.call(cls, property, propertyValue, forModification);
    }

    @Override
    public <A extends BaseAsset> A onLoadAsset(Class<A> cls, String id, boolean forModification, CallableWithExceptionAnd3Arguments<A, Class<A>, String, Boolean> callback) throws Exception {
        loadAssetCount++;
        return callback.call(cls, id, forModification);
    }

    @Override
    public <D extends BaseDocument> D onLoadDocument(Class<D> cls, String id, boolean forModification, CallableWithExceptionAnd3Arguments<D, Class<D>, String, Boolean> callback) throws Exception {
        loadDocumentCount++;
        return callback.call(cls, id, forModification);
    }

    public int getLoadDocumentCount() {
        return loadDocumentCount;
    }

    public void setLoadDocumentCount(int loadDocumentCount) {
        this.loadDocumentCount = loadDocumentCount;
    }

    public int getLoadAssetCount() {
        return loadAssetCount;
    }

    public void setLoadAssetCount(int loadAssetCount) {
        this.loadAssetCount = loadAssetCount;
    }

    public int getFindAssetCount() {
        return findAssetCount;
    }

    public void setFindAssetCount(int findAssetCount) {
        this.findAssetCount = findAssetCount;
    }

    public int getFindDocumentCount() {
        return findDocumentCount;
    }

    public void setFindDocumentCount(int findDocumentCount) {
        this.findDocumentCount = findDocumentCount;
    }

    public int getGetAllDocumentsCount() {
        return getAllDocumentsCount;
    }

    public void setGetAllDocumentsCount(int getAllDocumentsCount) {
        this.getAllDocumentsCount = getAllDocumentsCount;
    }
}
