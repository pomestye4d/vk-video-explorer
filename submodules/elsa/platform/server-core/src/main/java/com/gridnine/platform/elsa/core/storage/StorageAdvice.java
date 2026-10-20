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

import com.gridnine.platform.elsa.common.core.model.common.*;
import com.gridnine.platform.elsa.common.core.model.domain.*;
import com.gridnine.platform.elsa.common.core.search.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

public interface StorageAdvice extends HasPriority {
    default <A extends BaseAsset> void onSave(A asset, RunnableWithExceptionAndArgument<A> callback) throws Exception {
        callback.run(asset);
    }

    default <A extends BaseAsset> List<A> onSearchAssets(Class<A> cls, SearchQuery query, boolean forModification, CallableWithExceptionAnd3Arguments<List<A>, Class<A>, SearchQuery, Boolean> callback) throws Exception {
        return callback.call(cls, query, forModification);
    }

    default List<VersionInfo> onGetVersionsMetadata(Class<?> cls, Callable<List<VersionInfo>> callback) throws Exception {
        return callback.call();
    }

    default <A extends BaseAsset> void onDeleteAsset(A asset, RunnableWithExceptionAndArgument<A> callback) throws Exception {
        callback.run(asset);
    }

    default <A extends BaseAsset> A onLoadAssetVersion(Class<A> cls, String id, int version, CallableWithExceptionAnd3Arguments<A, Class<A>, String, Integer> callback) throws Exception {
        return callback.call(cls, id, version);
    }

    default <A extends BaseAsset> A onLoadAsset(Class<A> cls, String id, boolean forModification, CallableWithExceptionAnd3Arguments<A, Class<A>, String, Boolean> callback) throws Exception {
        return callback.call(cls, id, forModification);
    }

    default <D extends BaseDocument> D onLoadDocument(Class<D> cls, String id, boolean forModification, CallableWithExceptionAnd3Arguments<D, Class<D>, String, Boolean> callback) throws Exception {
        return callback.call(cls, id, forModification);
    }

    default <D extends BaseDocument> D onLoadDocumentVersion(Class<D> cls, String objectId, int versionNumber, CallableWithExceptionAnd3Arguments<D, Class<D>, String, Integer> callback) throws Exception {
        return callback.call(cls, objectId, versionNumber);
    }

    default <T, D extends BaseDocument, I extends BaseSearchableProjection<D>, E extends FieldNameSupport & EqualitySupport & ArgumentType<T>> EntityReference<D>
    onFindUniqueDocumentReference(Class<I> projClass, E property, T propertyValue, CallableWithExceptionAnd3Arguments<EntityReference<D>,
            Class<I>, E, T> callback) throws Exception {
        return callback.call(projClass, property, propertyValue);
    }

    default <T, D extends BaseDocument, I extends BaseSearchableProjection<D>, E extends FieldNameSupport & EqualitySupport & ArgumentType<T>> Set<EntityReference<D>>
    onGetAllDocumentReferences(Class<I> projClass, E property, T propertyValue, CallableWithExceptionAnd3Arguments<Set<EntityReference<D>>,
            Class<I>, E, T> callback) throws Exception {
        return callback.call(projClass, property, propertyValue);
    }

    default <A extends BaseAsset> List<List<Object>> onSearchAssets(Class<A> cls, AggregationQuery query, CallableWithExceptionAnd2Arguments<List<List<Object>>, Class<A>, AggregationQuery> callback) throws Exception {
        return callback.call(cls, query);
    }

    default <D extends BaseDocument, I extends BaseSearchableProjection<D>> List<I> onSearchDocuments(Class<I> cls, SearchQuery query,
                                                                                                      CallableWithExceptionAnd2Arguments<List<I>, Class<I>, SearchQuery> callback) throws Exception {
        return callback.call(cls, query);
    }

    default <D extends BaseDocument, I extends BaseSearchableProjection<D>> List<List<Object>> onSearchDocuments(Class<I> cls, AggregationQuery query,
                                                                                                                 CallableWithExceptionAnd2Arguments<List<List<Object>>, Class<I>, AggregationQuery> callback) throws Exception {
        return callback.call(cls, query);
    }

    default <D extends BaseDocument> void onSaveDocument(D doc, boolean createNewVersion, String comment, SaveDocumentParameters params,
                                                         RunnableWithExceptionAnd4Arguments<D, Boolean, String, SaveDocumentParameters> callback) throws Exception {
        callback.run(doc, createNewVersion, comment, params);
    }

    default <D extends BaseDocument> void onDeleteDocument(D document, DeleteDocumentParameters params,
                                                           RunnableWithExceptionAnd2Arguments<D, DeleteDocumentParameters> callback) throws Exception {
        callback.run(document, params);
    }

    default <D extends BaseIdentity> void onUpdateCaptions(D entity, UpdateCaptionsParameters params, RunnableWithExceptionAnd2Arguments<D, UpdateCaptionsParameters> callback) throws Exception {
        callback.run(entity, params);
    }

    default <T, A extends BaseAsset, E extends FieldNameSupport & EqualitySupport & ArgumentType<T>> A onFindUniqueAsset(Class<A> cls, E property, T propertyValue, boolean forModification,
                                                                                                                         CallableWithExceptionAnd4Arguments<A,
                                                                                                                                 Class<A>, E, T, Boolean> callbackObject) throws Exception {
        return callbackObject.call(cls, property, propertyValue, forModification);
    }

    default <T, A extends BaseAsset, E extends FieldNameSupport & EqualitySupport & ArgumentType<T>> Set<A> onGetAllAssets(Class<A> cls, E property, T propertyValue, boolean forModification, CallableWithExceptionAnd4Arguments<Set<A>,
            Class<A>, E, T, Boolean> callbackObject) throws Exception {
        return callbackObject.call(cls, property, propertyValue, forModification);
    }

    default <VA extends BaseVirtualAsset> List<VA> onSearchVirtualAssets(Class<VA> cls, SearchQuery query, CallableWithExceptionAnd2Arguments<List<VA>, Class<VA>, SearchQuery> callback) throws Exception{
      return callback.call(cls, query);
    }

    default <VA extends BaseVirtualAsset> List<List<Object>> onSearchVirtualAssets(Class<VA> cls, AggregationQuery query, CallableWithExceptionAnd2Arguments<List<List<Object>>, Class<VA>, AggregationQuery> callback) throws Exception{
        return callback.call(cls, query);
    }
}
