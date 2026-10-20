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

package com.gridnine.platform.elsa.server.core.storage;

import com.gridnine.platform.elsa.common.core.model.domain.EntityReference;
import com.gridnine.platform.elsa.common.core.model.domain.TextSearchValue;
import com.gridnine.platform.elsa.common.core.search.*;
import com.gridnine.platform.elsa.common.core.test.model.domain.*;
import com.gridnine.platform.elsa.core.auth.AuthContext;
import com.gridnine.platform.elsa.core.storage.Storage;
import com.gridnine.platform.elsa.core.storage.database.jdbc.JdbcDatabase;
import com.gridnine.platform.elsa.core.storage.transaction.ElsaTransactionManager;
import com.gridnine.platform.elsa.server.core.common.ServerCoreTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.annotation.DirtiesContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class StorageTest extends ServerCoreTestBase {

    @Autowired
    private Storage storage;

    @Autowired
    private ElsaTransactionManager transactionManager;

    @Test
    public void testDocumentCRUD() {
        AuthContext.setCurrentUser("system");
        var doc = new TestDomainDocument();
        doc.setStringProperty("test");
        doc.setEntityReference(new EntityReference<>(doc));
        doc.getStringCollection().addAll(Arrays.asList("test1", "test2"));
        doc.getEnumCollection().addAll(Arrays.asList(TestEnum.ITEM1, TestEnum.ITEM2));
        doc.getEntityRefCollection().addAll(Arrays.asList(new EntityReference<>(doc), new EntityReference<>(doc)));
        doc.setEnumProperty(TestEnum.ITEM1);
        doc.setInstantProperty(Instant.now());
        storage.saveDocument(doc, "version1");
        doc = storage.loadDocument(TestDomainDocument.class, doc.getId(), true);
        Assertions.assertEquals("test", doc.getStringProperty());
        doc.setStringProperty("test2");
        storage.saveDocument(doc, true, "version2");
        doc = storage.loadDocument(TestDomainDocument.class, doc.getId(), true);
        Assertions.assertEquals("test2", doc.getStringProperty());
        doc = storage.loadDocumentVersion(TestDomainDocument.class, doc.getId(), 0);
        Assertions.assertEquals("test", doc.getStringProperty());
        var metadataItems = storage.getVersionsMetadata(TestDomainDocument.class, doc.getId());
        Assertions.assertEquals(2, metadataItems.size());
        var query = new SearchQueryBuilder().where(SearchCriterion.eq(TestDomainDocumentProjectionFields.stringProperty,
                "test2")).build();
        var indexes = storage.searchDocuments(TestDomainDocumentProjection.class, query);
        Assertions.assertEquals(1, indexes.size());
        query = new SearchQueryBuilder().freeText("test2").build();
        indexes = storage.searchDocuments(TestDomainDocumentProjection.class, query);
        Assertions.assertEquals(1, indexes.size());
        indexes = storage.searchDocuments(TestDomainDocumentProjection.class, new SearchQuery());
        Assertions.assertEquals(1, indexes.size());
        var idx = indexes.get(0);
        Assertions.assertEquals(2, idx.getStringCollection().size());
        Assertions.assertEquals(2, idx.getEnumCollection().size());
        Assertions.assertEquals(2, idx.getEntityRefCollection().size());
        query = new SearchQueryBuilder().preferredFields(TestDomainDocumentProjectionFields.entityReference)
                .where(SearchCriterion.eq(TestDomainDocumentProjectionFields.entityReference,
                        doc.getEntityReference())).build();
        indexes = storage.searchDocuments(TestDomainDocumentProjection.class, query);
        Assertions.assertEquals(1, indexes.size());
        var aggregationQuery = new AggregationQueryBuilder().count().where(
                SearchCriterion.eq(TestDomainDocumentProjectionFields.stringProperty, "test2")
        ).build();
        var count = (long) storage.searchDocuments(TestDomainDocumentProjection.class, aggregationQuery).get(0).get(0);
        Assertions.assertEquals(1, count);
        var refs = storage.searchCaptions(TestDomainDocument.class, "test", 10);
        Assertions.assertEquals(1, refs.size());
    }

    @Test
    public void testAssetCRUD() {
        AuthContext.setCurrentUser("system");
        var asset = new TestDomainAsset();
        asset.setStringProperty("test");
        asset.setDateProperty(LocalDateTime.now());
        {
            var logItem = new TestLogItem();
            logItem.setWhat("edited 1");
            logItem.setWho("ivan");
            asset.setLogItem(logItem);
        }
        {
            var logItem = new TestLogItem();
            logItem.setWhat("edited 2");
            logItem.setWho("petr");
            asset.getLog().add(logItem);
        }
        {
            var logItem = new TestLogItem();
            logItem.setWhat("edited 3");
            logItem.setWho("dima");
            asset.getLog().add(logItem);
        }
        storage.saveAsset(asset, "init");
        asset = storage.loadAsset(TestDomainAsset.class, asset.getId(), true);
        {
            var logItem = asset.getLogItem();
            Assertions.assertEquals("edited 1", logItem.getWhat());
            Assertions.assertEquals("ivan", logItem.getWho());
        }
        {
            var logItem = asset.getLog().getFirst();
            Assertions.assertEquals("edited 2", logItem.getWhat());
            Assertions.assertEquals("petr", logItem.getWho());
        }
        {
            var logItem = asset.getLog().getLast();
            Assertions.assertEquals("edited 3", logItem.getWhat());
            Assertions.assertEquals("dima", logItem.getWho());
        }
        var metadataItems = storage.getVersionsMetadata(TestDomainAsset.class, asset.getId());
        asset.setStringProperty("test2");
        storage.saveAsset(asset, true, "version2");
        asset = storage.loadAsset(TestDomainAsset.class, asset.getId(), true);
        asset = storage.loadAssetVersion(TestDomainAsset.class, asset.getId(), 0);
        Assertions.assertEquals("test", asset.getStringProperty());
        var query = new SearchQueryBuilder().
                where(SearchCriterion.eq(TestDomainAssetFields.stringProperty, "test2")).build();
        var assets = storage.searchAssets(TestDomainAsset.class, query);
        Assertions.assertEquals(1, assets.size());
        query = new SearchQueryBuilder().freeText("test2").build();
        assets = storage.searchAssets(TestDomainAsset.class, query);
        Assertions.assertEquals(1, assets.size());
        assets = storage.searchAssets(TestDomainAsset.class, new SearchQuery());
        Assertions.assertEquals(1, assets.size());
        metadataItems = storage.getVersionsMetadata(TestDomainAsset.class, asset.getId());
        Assertions.assertEquals(2, metadataItems.size());
        var aggregationQuery = new AggregationQueryBuilder().count().where(
                SearchCriterion.eq(TestDomainAssetFields.stringProperty, "test2")
        ).build();
        var count = (long) storage.searchAssets(TestDomainAsset.class, aggregationQuery).get(0).get(0);
        Assertions.assertEquals(1, count);
    }

    @Test
    public void testTextSearchField() {
        AuthContext.setCurrentUser("system");
        var asset = new TestDomainAsset();
        asset.setStringProperty("test");
        asset.setDateProperty(LocalDateTime.now());
        var text = "the quick brown fox jumps over the lazy dog";
        asset.setSearchableText(new TextSearchValue(text));
        storage.saveAsset(asset, "init");
        asset = storage.loadAsset(TestDomainAsset.class, asset.getId(), true);
        Assertions.assertEquals(text, asset.getSearchableText().getValue());
        Assertions.assertEquals(0, asset.getSearchableText().getMatchedFragmentPositions().length);

        // note: this repo has no Postgres-backed test module, so only the HSQLDB (client-side, LIKE-based)
        // matched-position path is exercised here. The Postgres native to_tsvector/plainto_tsquery path, including
        // the per-locale (elsa.storage.tsLocales) OR-across-columns search, needs separate manual/integration verification.
        var query = new SearchQueryBuilder().where(SearchCriterion.fts(TestDomainAssetFields.searchableText, "quick fox")).build();
        var assets = storage.searchAssets(TestDomainAsset.class, query);
        Assertions.assertEquals(1, assets.size());
        var found = assets.getFirst();
        var foundText = found.getSearchableText().getValue();
        var positions = found.getSearchableText().getMatchedFragmentPositions();
        Assertions.assertTrue(positions.length > 0);
        var matchedChars = new StringBuilder();
        for (var pos : positions) {
            matchedChars.append(foundText.charAt(pos));
        }
        Assertions.assertTrue(matchedChars.toString().contains("quick"));
        Assertions.assertTrue(matchedChars.toString().contains("fox"));

        query = new SearchQueryBuilder().where(SearchCriterion.fts(TestDomainAssetFields.searchableText, "elephant")).build();
        assets = storage.searchAssets(TestDomainAsset.class, query);
        Assertions.assertEquals(0, assets.size());
    }

    @Test
    public void testTransaction() {
        try {
            transactionManager.withTransaction((tx) -> {
                AuthContext.setCurrentUser("system");
                var asset = new TestDomainAsset();
                asset.setStringProperty("test");
                asset.setDateProperty(LocalDateTime.now());
                storage.saveAsset(asset, "init");
                var asset2 = new TestDomainAsset();
                asset2.setStringProperty("test");
                asset2.setDateProperty(LocalDateTime.now());
                storage.saveAsset(asset2, "init");
                throw new Exception("test");
            });
        } catch (Throwable e) {
            //noops
        }
        Assertions.assertEquals(0, storage.searchAssets(TestDomainAsset.class, new SearchQuery()).size());
        transactionManager.withTransaction((tx) -> {
            AuthContext.setCurrentUser("system");
            var asset = new TestDomainAsset();
            asset.setStringProperty("test");
            asset.setDateProperty(LocalDateTime.now());
            storage.saveAsset(asset, "init");
            var asset2 = new TestDomainAsset();
            asset2.setStringProperty("test");
            asset2.setDateProperty(LocalDateTime.now());
            storage.saveAsset(asset2, "init");
        });
        var assets = storage.searchAssets(TestDomainAsset.class, new SearchQuery());
        Assertions.assertEquals(2, assets.size());
        assets.forEach((it) -> storage.deleteAsset(it));

    }

    @Test
    public void testVirtualAssets() {
        var joinedAsset = new TestDomainJoinedAsset();
        joinedAsset.setJoinedProperty("joined value");
        storage.saveAsset(joinedAsset, "init");
        var baseAsset = new TestDomainAsset();
        baseAsset.setStringProperty("test");
        baseAsset.setDateProperty(LocalDateTime.now());
        baseAsset.setLocalJoinedField(UUID.fromString(joinedAsset.getId()));
        storage.saveAsset(baseAsset, "init");
        var vas = storage.searchVirtualAssets(TestDomainVirtualAsset.class,
                new SearchQueryBuilder().where(SearchCriterion.eq(TestDomainVirtualAssetFields.joinedProperty, "joined value"))
                        .orderBy(TestDomainVirtualAssetFields.stringProperty, SortOrder.ASC).build());
        Assertions.assertEquals(1, vas.size());
        var item = vas.get(0);
        baseAsset = storage.loadAsset(baseAsset.toReference(), false);
        Assertions.assertEquals(baseAsset.getStringProperty(), item.getStringProperty());
        Assertions.assertEquals(baseAsset.getDateProperty(), item.getDateProperty());
        Assertions.assertEquals(baseAsset.getLocalJoinedField(), item.getLocalJoinedField());
        Assertions.assertEquals(joinedAsset.getJoinedProperty(), item.getJoinedProperty());

    }

    @Test
    public void testVirtualAssetsAggregationQuery() {
        var joinedAsset = new TestDomainJoinedAsset();
        joinedAsset.setJoinedProperty("joined value");
        storage.saveAsset(joinedAsset, "init");
        var baseAsset = new TestDomainAsset();
        baseAsset.setStringProperty("test");
        baseAsset.setDateProperty(LocalDateTime.now());
        baseAsset.setLocalJoinedField(UUID.fromString(joinedAsset.getId()));
        storage.saveAsset(baseAsset, "init");
        var vas = storage.searchVirtualAssets(TestDomainVirtualAsset.class,
                new AggregationQueryBuilder().where(SearchCriterion.eq(TestDomainVirtualAssetFields.joinedProperty, "joined value")).count().build());
        Assertions.assertEquals(1, vas.size());
        Assertions.assertEquals(1, ((Number) vas.get(0).get(0)).intValue());
    }

    @Test
    public void testNativeOperation(){
        var asset = new TestDomainAsset();
        asset.setStringProperty("test");
        asset.setDateProperty(LocalDateTime.now());
        storage.saveAsset(asset, "init");
        var res = storage.performNativeOperation(ctx ->{
            var template = ctx.getAttribute(JdbcDatabase.JDBC_TEMPLATE_PARAM);
            var result = template.query("select * from testdomainasset where stringproperty = ?", ps -> {
                ps.setString(1, "test");
            }, (rs, rowNum) -> rs.getString("stringproperty"));
            return result.get(0);
        });
        Assertions.assertEquals("test", res);
    }
}
