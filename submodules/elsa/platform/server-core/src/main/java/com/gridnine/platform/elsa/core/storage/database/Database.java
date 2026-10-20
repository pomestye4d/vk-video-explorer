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

package com.gridnine.platform.elsa.core.storage.database;

import com.gridnine.platform.elsa.common.core.model.common.BaseIdentity;
import com.gridnine.platform.elsa.common.core.model.common.CallableWithExceptionAndArgument;
import com.gridnine.platform.elsa.common.core.model.domain.*;
import com.gridnine.platform.elsa.common.core.search.AggregationQuery;
import com.gridnine.platform.elsa.common.core.search.SearchQuery;
import com.gridnine.platform.elsa.core.storage.transaction.ElsaTransactionContext;

import java.util.*;

public interface Database {
    <A extends BaseAsset> DatabaseAssetWrapper<A> loadAssetWrapper(Class<A> aClass, String id) throws Exception;

    <A extends BaseAsset> void saveAsset(DatabaseAssetWrapper<A> aDatabaseAssetWrapper, DatabaseAssetWrapper<A> oldAsset) throws Exception;

    <A extends BaseAsset> void saveAssetVersion(Class<A> aClass, String id, DatabaseBinaryData data, VersionInfo vi) throws Exception;

    <A extends BaseAsset> List<A> searchAssets(Class<A> cls, SearchQuery updateQuery) throws Exception;

    List<VersionInfo> getVersionsMetadata(Class<?> cls, String id) throws Exception;

    <A extends BaseAsset> void deleteAsset(Class<A> aClass, String id) throws Exception;

    DatabaseObjectData loadVersion(Class<?> cls, String id, int number) throws Exception;

    void deleteVersion(Class<?> cls, String id, int number) throws Exception;

    <A extends BaseAsset> A loadAsset(Class<A> cls, String id) throws Exception;

    void updateCaptions(Class<?> aClass, String id, LinkedHashMap<Locale, String> captions, boolean insert) throws Exception;

    void updateCaptions(Class<?> aClass, String id, String name, boolean insert) throws Exception;

    void deleteCaptions(Class<?> aClass, String id) throws Exception;

    <D extends BaseDocument> DatabaseObjectData loadDocumentData(Class<D> cls, String id) throws Exception;

    <D extends BaseDocument, I extends BaseSearchableProjection<D>> List<I> searchDocuments(Class<I> projClass, SearchQuery query) throws Exception;

    <A extends BaseAsset> List<List<Object>> searchAssets(Class<A> cls, AggregationQuery updateQuery) throws Exception;

    <D extends BaseDocument, I extends BaseSearchableProjection<D>> List<List<Object>> searchDocuments(Class<I> cls, AggregationQuery updateQuery) throws Exception;

    void updateProjections(Class<BaseSearchableProjection<BaseDocument>> projectionClass, String id, ArrayList<DatabaseSearchableProjectionWrapper<BaseDocument, BaseSearchableProjection<BaseDocument>>> wrappers, boolean update) throws Exception;

    <D extends BaseDocument> void saveDocument(String id, Class<D> aClass, DatabaseObjectData obj, DatabaseObjectData oldDocument) throws Exception;

    <D extends BaseDocument> void saveDocumentVersion(Class<D> aClass, String id, DatabaseObjectData version, Long oldVersionDataId) throws Exception;

    <D extends BaseDocument> void deleteDocument(Class<D> aClass, String id, Long oid) throws Exception;

    <D extends BaseIdentity> List<EntityReference<D>> searchCaptions(Class<D> cls, String pattern, int limit, Locale locale) throws Exception;

    <D extends BaseDocument, I extends BaseSearchableProjection<D>> void deleteProjections(Class<I> projectionClass, String id) throws Exception;

    <I extends BaseIdentity> String getCaption(Class<I> type, String id, Locale locale) throws Exception;

    <I extends BaseIdentity> String getCaption(Class<I> type, String id) throws Exception;

    <VA extends BaseVirtualAsset> List<VA> searchVirtualAssets(Class<VA> cls, SearchQuery updateQuery) throws Exception;

    <VA extends BaseVirtualAsset> List<List<Object>> searchVirtualAssets(Class<VA> cls, AggregationQuery updateQuery) throws Exception;

    <RP> RP performNativeOperation(CallableWithExceptionAndArgument<RP, ElsaTransactionContext> operation, ElsaTransactionContext ctx) throws Exception;
}
