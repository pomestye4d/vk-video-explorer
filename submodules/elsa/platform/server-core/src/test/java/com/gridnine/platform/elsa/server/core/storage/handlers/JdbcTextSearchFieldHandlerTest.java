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

package com.gridnine.platform.elsa.server.core.storage.handlers;

import com.gridnine.platform.elsa.common.core.model.domain.TextSearchValue;
import com.gridnine.platform.elsa.common.core.search.SimpleCriterion;
import com.gridnine.platform.elsa.core.storage.database.jdbc.adapter.JdbcDialect;
import com.gridnine.platform.elsa.core.storage.database.jdbc.handlers.JdbcTextSearchFieldHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * These are pure unit tests exercising {@link JdbcTextSearchFieldHandler} directly, via lightweight
 * JDK-proxy test doubles for {@link JdbcDialect}/{@link ResultSet}. This repo has no Postgres-backed
 * integration test module (see StorageTest), so the Postgres-only regex-highlighting path added to
 * this handler cannot be exercised through the existing HSQLDB-backed {@code StorageTest}.
 */
public class JdbcTextSearchFieldHandlerTest {

    private static final char HL_START = (char) 1;

    private static final char HL_STOP = (char) 2;

    @Test
    public void ftsWhereExpressionAddsRegexClauseForValidRegexOnFtsDialect() {
        var handler = new JdbcTextSearchFieldHandler("chunkContent", true, List.of("simple"));
        var criterion = new SimpleCriterion("chunkContent", SimpleCriterion.Operation.FTS, "f\\w+@\\w+");

        var expr = handler.getFtsWhereExpression(criterion, fakeDialect(true));

        Assertions.assertTrue(expr.getLeft().contains("chunkContent ~* ?"), expr.getLeft());
        Assertions.assertEquals(2, expr.getRight().size());
        Assertions.assertEquals("f\\w+@\\w+", expr.getRight().get(1).getLeft());
    }

    @Test
    public void ftsWhereExpressionSkipsRegexClauseForInvalidRegex() {
        var handler = new JdbcTextSearchFieldHandler("chunkContent", true, List.of("simple"));
        var criterion = new SimpleCriterion("chunkContent", SimpleCriterion.Operation.FTS, "a(b");

        var expr = handler.getFtsWhereExpression(criterion, fakeDialect(true));

        Assertions.assertFalse(expr.getLeft().contains("~*"), expr.getLeft());
        Assertions.assertEquals(1, expr.getRight().size());
    }

    @Test
    public void ftsWhereExpressionUnaffectedOnNonFtsDialect() {
        var handler = new JdbcTextSearchFieldHandler("chunkContent", true, List.of("simple"));
        var criterion = new SimpleCriterion("chunkContent", SimpleCriterion.Operation.FTS, "f\\w+@\\w+");

        var expr = handler.getFtsWhereExpression(criterion, fakeDialect(false));

        Assertions.assertFalse(expr.getLeft().contains("~*"), expr.getLeft());
        Assertions.assertTrue(expr.getLeft().contains("like"), expr.getLeft());
    }

    @Test
    public void modelValueMergesRegexPositionsWithHighlightPositions() throws Exception {
        var handler = new JdbcTextSearchFieldHandler("chunkContent", true, List.of("simple"));
        var text = "contact us at foo@example.com for details";
        // ts_headline would highlight the tokenized match "details"; the regex should additionally find the email.
        var highlighted = "contact us at foo@example.com for " + HL_START + "details" + HL_STOP;
        var criterion = new SimpleCriterion("chunkContent", SimpleCriterion.Operation.FTS, "f\\w+@\\w+\\.\\w+");
        var rs = fakeResultSet(Map.of("chunkContent", text, "chunkContent_hl", highlighted));

        var value = (TextSearchValue) handler.getModelValue(rs, null, null, null, null, fakeDialect(true), criterion);

        Assertions.assertEquals(text, value.getValue());
        var matched = new StringBuilder();
        for (var p : value.getMatchedFragmentPositions()) {
            matched.append(text.charAt(p));
        }
        Assertions.assertTrue(matched.toString().contains("details"), matched.toString());
        Assertions.assertTrue(matched.toString().contains("foo@example.com"), matched.toString());
    }

    @Test
    public void modelValueMatchesRegexCaseInsensitivelyForNonAsciiText() throws Exception {
        var handler = new JdbcTextSearchFieldHandler("chunkContent", true, List.of("simple"));
        var text = "Hello world 1 Привет мир 1";
        // ts_headline found no tokenized match for the lowercase substring query "прив"; only the regex path should highlight it.
        var criterion = new SimpleCriterion("chunkContent", SimpleCriterion.Operation.FTS, "прив");
        var rs = fakeResultSet(Map.of("chunkContent", text, "chunkContent_hl", text));

        var value = (TextSearchValue) handler.getModelValue(rs, null, null, null, null, fakeDialect(true), criterion);

        Assertions.assertTrue(value.getMatchedFragmentPositions().length > 0);
        var matched = new StringBuilder();
        for (var p : value.getMatchedFragmentPositions()) {
            matched.append(text.charAt(p));
        }
        Assertions.assertEquals("Прив", matched.toString());
    }

    @Test
    public void modelValueIgnoresInvalidRegexAndKeepsHighlightPositions() throws Exception {
        var handler = new JdbcTextSearchFieldHandler("chunkContent", true, List.of("simple"));
        var text = "the quick brown fox";
        var highlighted = "the " + HL_START + "quick" + HL_STOP + " brown fox";
        var criterion = new SimpleCriterion("chunkContent", SimpleCriterion.Operation.FTS, "a(b");
        var rs = fakeResultSet(Map.of("chunkContent", text, "chunkContent_hl", highlighted));

        var value = (TextSearchValue) handler.getModelValue(rs, null, null, null, null, fakeDialect(true), criterion);

        var matched = new StringBuilder();
        for (var p : value.getMatchedFragmentPositions()) {
            matched.append(text.charAt(p));
        }
        Assertions.assertEquals("quick", matched.toString());
    }

    private static JdbcDialect fakeDialect(boolean ftsSupport) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("hasFullTextSearchSupport".equals(method.getName())) {
                return ftsSupport;
            }
            throw new UnsupportedOperationException(method.getName());
        };
        return (JdbcDialect) Proxy.newProxyInstance(
                JdbcTextSearchFieldHandlerTest.class.getClassLoader(), new Class<?>[]{JdbcDialect.class}, handler);
    }

    private static ResultSet fakeResultSet(Map<String, String> columnValues) {
        var columnNames = List.copyOf(columnValues.keySet());
        InvocationHandler metaHandler = (proxy, method, args) -> switch (method.getName()) {
            case "getColumnCount" -> columnNames.size();
            case "getColumnLabel" -> columnNames.get((int) args[0] - 1);
            default -> throw new UnsupportedOperationException(method.getName());
        };
        var meta = (ResultSetMetaData) Proxy.newProxyInstance(
                JdbcTextSearchFieldHandlerTest.class.getClassLoader(), new Class<?>[]{ResultSetMetaData.class}, metaHandler);

        var normalizedValues = new LinkedHashMap<>(columnValues);
        InvocationHandler rsHandler = (proxy, method, args) -> switch (method.getName()) {
            case "getString", "getObject" -> normalizedValues.get((String) args[0]);
            case "getMetaData" -> meta;
            default -> throw new UnsupportedOperationException(method.getName());
        };
        return (ResultSet) Proxy.newProxyInstance(
                JdbcTextSearchFieldHandlerTest.class.getClassLoader(), new Class<?>[]{ResultSet.class}, rsHandler);
    }
}
