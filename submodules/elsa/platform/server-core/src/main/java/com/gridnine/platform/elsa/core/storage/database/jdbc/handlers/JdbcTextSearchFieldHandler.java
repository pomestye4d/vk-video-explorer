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

package com.gridnine.platform.elsa.core.storage.database.jdbc.handlers;

import com.gridnine.platform.elsa.common.core.model.common.ClassMapper;
import com.gridnine.platform.elsa.common.core.model.common.EnumMapper;
import com.gridnine.platform.elsa.common.core.model.common.Xeption;
import com.gridnine.platform.elsa.common.core.model.domain.CaptionProvider;
import com.gridnine.platform.elsa.common.core.model.domain.TextSearchValue;
import com.gridnine.platform.elsa.common.core.reflection.ReflectionFactory;
import com.gridnine.platform.elsa.common.core.search.SimpleCriterion;
import com.gridnine.platform.elsa.common.core.utils.Pair;
import com.gridnine.platform.elsa.core.storage.database.jdbc.adapter.JdbcDialect;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcFieldType;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcIndexDescription;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcIndexType;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcUtils;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class JdbcTextSearchFieldHandler implements JdbcFieldHandler {

    /** Highlight fragment delimiters used with Postgres ts_headline; chosen as rare control codes unlikely to appear in real text. */
    private static final char HL_START = (char) 1;

    private static final char HL_STOP = (char) 2;

    private final String fieldName;

    private final boolean indexed;

    private final List<String> locales;

    private final String hlFieldName;

    public JdbcTextSearchFieldHandler(String fieldName, boolean indexed, List<String> locales) {
        this.fieldName = fieldName;
        this.indexed = indexed;
        this.locales = locales;
        this.hlFieldName = "%s_hl".formatted(fieldName);
    }

    private String tsFieldName(String locale) {
        return "%sts_value_%s".formatted(fieldName, locale);
    }

    @Override
    public Map<String, JdbcFieldType> getColumns() {
        final var result = new LinkedHashMap<String, JdbcFieldType>();
        result.put(fieldName, JdbcFieldType.TEXT);
        for (var locale : locales) {
            result.put(tsFieldName(locale), JdbcFieldType.TEXT_SEARCH);
        }
        return result;
    }

    @Override
    public Map<String, JdbcIndexDescription> getIndexes(String tableName) {
        if (!indexed) {
            return Map.of();
        }
        var result = new LinkedHashMap<String, JdbcIndexDescription>();
        for (var locale : locales) {
            var tsFieldName = tsFieldName(locale);
            result.put("%s_%s".formatted(tableName, tsFieldName), new JdbcIndexDescription(tsFieldName, JdbcIndexType.GIN_TSVECTOR));
        }
        return result;
    }

    @Override
    public Map<String, Pair<Object, JdbcFieldType>> getSqlValues(Object value, EnumMapper enumMapper, ClassMapper classMapper, ReflectionFactory factory, JdbcDialect dialect) throws Exception {
        var result = new LinkedHashMap<String, Pair<Object, JdbcFieldType>>();
        var tsv = (TextSearchValue) value;
        var text = tsv == null ? null : tsv.getValue();
        result.put(fieldName, Pair.of(text, JdbcFieldType.TEXT));
        for (var locale : locales) {
            result.put(tsFieldName(locale), Pair.of(text, JdbcFieldType.TEXT_SEARCH));
        }
        return result;
    }

    @Override
    public Map<String, String> getSqlValueExpressions(JdbcDialect dialect) {
        if (!dialect.hasFullTextSearchSupport()) {
            return Map.of();
        }
        var result = new LinkedHashMap<String, String>();
        for (var locale : locales) {
            result.put(tsFieldName(locale), "to_tsvector('%s', ?)".formatted(locale));
        }
        return result;
    }

    @Override
    public Pair<Object, JdbcFieldType> getSqlQueryValue(Object value, EnumMapper enumMapper, ClassMapper classMapper, ReflectionFactory factory) throws Exception {
        throw Xeption.forDeveloper("unsupported");
    }

    @Override
    public String getExtraSelectExpression(SimpleCriterion activeCriterion, JdbcDialect dialect) {
        if (!isActiveFtsCriterion(activeCriterion) || !dialect.hasFullTextSearchSupport()) {
            return null;
        }
        return "ts_headline('simple', %s, plainto_tsquery('simple', ?), 'StartSel=%s,StopSel=%s,MaxWords=1000000,MinWords=1,HighlightAll=true') as %s"
                .formatted(fieldName, HL_START, HL_STOP, hlFieldName);
    }

    @Override
    public Pair<String, List<Pair<Object, JdbcFieldType>>> getFtsWhereExpression(SimpleCriterion criterion, JdbcDialect dialect) {
        var values = new ArrayList<Pair<Object, JdbcFieldType>>();
        if (dialect.hasFullTextSearchSupport()) {
            var phrase = (String) criterion.value;
            var clauses = new ArrayList<String>();
            for (var locale : locales) {
                clauses.add("%s @@ plainto_tsquery('%s', ?)".formatted(tsFieldName(locale), locale));
                values.add(Pair.of(phrase, JdbcFieldType.STRING));
            }
            if (isValidRegex(phrase)) {
                clauses.add("%s ~* ?".formatted(fieldName));
                values.add(Pair.of(phrase, JdbcFieldType.STRING));
            }
            return Pair.of("(%s)".formatted(String.join(" or ", clauses)), values);
        }
        var tokens = ((String) criterion.value).toLowerCase().split("\\W+");
        var subQuery = new StringBuilder("(");
        var first = true;
        for (var token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            if (!first) {
                subQuery.append(" and ");
            }
            subQuery.append(fieldName).append(" like ?");
            values.add(Pair.of("%%%s%%".formatted(token), JdbcFieldType.STRING));
            first = false;
        }
        subQuery.append(")");
        return Pair.of(subQuery.toString(), values);
    }

    @Override
    public Object getModelValue(ResultSet rs, EnumMapper enumMapper, ClassMapper classMapper, CaptionProvider captionProvider, ReflectionFactory factory, JdbcDialect dialect, SimpleCriterion activeCriterion) throws Exception {
        if (JdbcUtils.isNull(rs, fieldName)) {
            return null;
        }
        var text = rs.getString(fieldName);
        if (!isActiveFtsCriterion(activeCriterion)) {
            return new TextSearchValue(text, new int[0]);
        }
        var phrase = (String) activeCriterion.value;
        if (dialect.hasFullTextSearchSupport() && hasColumn(rs, hlFieldName)) {
            var highlighted = rs.getString(hlFieldName);
            var positions = new TreeSet<Integer>();
            if (highlighted != null) {
                for (var p : computePositionsFromHighlight(highlighted)) {
                    positions.add(p);
                }
            }
            for (var p : computeRegexPositions(text, phrase)) {
                positions.add(p);
            }
            return new TextSearchValue(text, positions.stream().mapToInt(Integer::intValue).toArray());
        }
        return new TextSearchValue(text, computePositionsClientSide(text, phrase));
    }

    private boolean isActiveFtsCriterion(SimpleCriterion activeCriterion) {
        return activeCriterion != null && activeCriterion.operation == SimpleCriterion.Operation.FTS && fieldName.equals(activeCriterion.property);
    }

    private static boolean hasColumn(ResultSet rs, String columnLabel) throws Exception {
        var meta = rs.getMetaData();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            if (columnLabel.equalsIgnoreCase(meta.getColumnLabel(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidRegex(String phrase) {
        if (phrase == null || phrase.isBlank()) {
            return false;
        }
        try {
            Pattern.compile(phrase);
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    private static int[] computeRegexPositions(String text, String phrase) {
        if (text == null || !isValidRegex(phrase)) {
            return new int[0];
        }
        var positions = new ArrayList<Integer>();
        var matcher = Pattern.compile(phrase, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(text);
        while (matcher.find()) {
            for (var p = matcher.start(); p < matcher.end(); p++) {
                positions.add(p);
            }
        }
        return positions.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int[] computePositionsFromHighlight(String highlighted) {
        var positions = new ArrayList<Integer>();
        var cleanPos = 0;
        var matchStart = 0;
        for (int i = 0; i < highlighted.length(); i++) {
            var c = highlighted.charAt(i);
            if (c == HL_START) {
                matchStart = cleanPos;
                continue;
            }
            if (c == HL_STOP) {
                for (var p = matchStart; p < cleanPos; p++) {
                    positions.add(p);
                }
                continue;
            }
            cleanPos++;
        }
        return positions.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int[] computePositionsClientSide(String text, String phrase) {
        if (text == null || phrase == null || phrase.isBlank()) {
            return new int[0];
        }
        var positions = new ArrayList<Integer>();
        var lowerText = text.toLowerCase();
        for (var token : phrase.toLowerCase().split("\\W+")) {
            if (token.isEmpty()) {
                continue;
            }
            var idx = lowerText.indexOf(token);
            while (idx >= 0) {
                for (var p = idx; p < idx + token.length(); p++) {
                    positions.add(p);
                }
                idx = lowerText.indexOf(token, idx + token.length());
            }
        }
        return positions.stream().mapToInt(Integer::intValue).toArray();
    }
}
