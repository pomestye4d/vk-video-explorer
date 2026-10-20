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

import com.gridnine.platform.elsa.common.core.model.common.BaseIdentity;
import com.gridnine.platform.elsa.common.core.model.common.CallableWithExceptionAndArgument;
import com.gridnine.platform.elsa.common.core.model.domain.*;
import com.gridnine.platform.elsa.common.core.search.*;
import com.gridnine.platform.elsa.core.storage.transaction.ElsaTransactionContext;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public interface Storage {
    <D extends BaseDocument> D loadDocument(EntityReference<D> ref, boolean forModification);

    <D extends BaseDocument> D loadDocument(Class<D> cls, String id, boolean forModification);

    <A extends BaseAsset> void saveAsset(A asset, boolean createNewVersion, String comment);

    <A extends BaseAsset> void deleteAsset(A asset);

    <A extends BaseAsset> void deleteAsset(A asset, DeleteAssetParameters params);

    List<VersionInfo> getVersionsMetadata(Class<?> cls, String id);

    <A extends BaseAsset> void saveAsset(A asset, String comment);

    <A extends BaseAsset> List<A> searchAssets(Class<A> cls, SearchQuery query, boolean forModification);

    <VA extends BaseVirtualAsset> List<VA> searchVirtualAssets(Class<VA> cls, SearchQuery query);

    <VA extends BaseVirtualAsset> List<List<Object>> searchVirtualAssets(Class<VA> cls, AggregationQuery query);

    <A extends BaseAsset> List<A> searchAssets(Class<A> cls, SearchQuery query);

    <A extends BaseAsset> A loadAssetVersion(Class<A> cls, String id, int version);

    <A extends BaseAsset> A loadAsset(Class<A> cls, String id, boolean forModification);

    default <A extends BaseAsset> A loadAsset(EntityReference<A> ref, boolean forModification){
        return ref == null? null: loadAsset(ref.getType(), ref.getId(), forModification);
    }

    <D extends BaseDocument> D loadDocumentVersion(Class<D> cls, String id, int version);

    <T, D extends BaseDocument, I extends BaseSearchableProjection<D>, E extends FieldNameSupport & EqualitySupport & ArgumentType<T>>
    EntityReference<D> findUniqueDocumentReference(Class<I> projClass, E property, T propertyValue);

    <T, D extends BaseDocument, I extends BaseSearchableProjection<D>, E extends FieldNameSupport & EqualitySupport & ArgumentType<T>>
    D findUniqueDocument(Class<I> projClass, E property, T propertyValue, boolean forModification);

    <T, D extends BaseDocument, I extends BaseSearchableProjection<D>, E extends FieldNameSupport & EqualitySupport & ArgumentType<T>>
    D findUniqueDocument(Class<I> projClass, E property, T propertyValue);

    <T, D extends BaseDocument, I extends BaseSearchableProjection<D>, E extends FieldNameSupport & EqualitySupport & ArgumentType<T>>
    Set<EntityReference<D>> getAllDocumentReferences(Class<I> projClass, E property, T propertyValue);

    <T, D extends BaseDocument, I extends BaseSearchableProjection<D>, E extends FieldNameSupport & EqualitySupport & ArgumentType<T>>
    Set<D> getAllDocuments(Class<I> projClass, E property, T propertyValue, boolean forModification);

    <A extends BaseAsset> List<List<Object>> searchAssets(Class<A> cls, AggregationQuery query);

    <D extends BaseDocument, I extends BaseSearchableProjection<D>> List<I> searchDocuments(Class<I> cls, SearchQuery query);

    <D extends BaseDocument, I extends BaseSearchableProjection<D>> List<List<Object>> searchDocuments(Class<I> cls, AggregationQuery query);

    <D extends BaseDocument> void updateProjections(D doc, Class<?>... projectionClasses);

    <D extends BaseDocument> void saveDocument(D doc);

    <D extends BaseDocument> void saveDocument(D doc, String comment);

    <D extends BaseDocument> void saveDocument(D doc, boolean createNewVersion, String comment);

    <D extends BaseDocument> void saveDocument(D doc, boolean createNewVersion, String comment, SaveDocumentParameters params);

    <A extends BaseAsset> void saveAsset(A asset, boolean createNewVersion, String comment, SaveAssetParameters params);

    <D extends BaseDocument> void deleteDocument(D doc);

    <D extends BaseDocument> void deleteDocument(D doc, DeleteDocumentParameters params);

    <D extends BaseIdentity> void updateCaptions(D entity);

    <D extends BaseIdentity> void updateCaptions(D entity, UpdateCaptionsParameters params);

    <D extends BaseIdentity> List<EntityReference<D>> searchCaptions(Class<D> cls, String pattern, int limit);

    <T, A extends BaseAsset, E extends FieldNameSupport & EqualitySupport & ArgumentType<T>> A findUniqueAsset(Class<A> cls, E property,
                                                                                                               T propertyValue,
                                                                                                               boolean forModification);

    <A extends BaseAsset> void updateAggregatedData(A asset);

    <T, A extends BaseAsset, E extends FieldNameSupport & EqualitySupport & ArgumentType<T>> Set<A> getAllAssets(Class<A> cls, E property, T propertyValue, boolean forModification);

    <I extends BaseIdentity> String getCaption(Class<I> type, String id, Locale currentLocale);

    <RP> RP performNativeOperation(CallableWithExceptionAndArgument<RP, ElsaTransactionContext> operation);
}