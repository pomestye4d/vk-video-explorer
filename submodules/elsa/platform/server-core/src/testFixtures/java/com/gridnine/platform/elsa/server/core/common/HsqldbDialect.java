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

package com.gridnine.platform.elsa.server.core.common;

import com.gridnine.platform.elsa.common.core.utils.Pair;
import com.gridnine.platform.elsa.core.storage.database.DatabaseBinaryData;
import com.gridnine.platform.elsa.core.storage.database.jdbc.adapter.JdbcDialect;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcFieldType;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcIndexDescription;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcIndexType;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcSequenceDescription;
import org.hsqldb.jdbc.JDBCBlob;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class HsqldbDialect implements JdbcDialect {

    private final JdbcTemplate template;

    public HsqldbDialect(DataSource template) {
        this.template = new JdbcTemplate(template);
    }

    @Override
    public Set<String> getTableNames() {
        return new LinkedHashSet<>(template.query("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC' ORDER BY TABLE_NAME ASC",
                (rs, rowNum) -> rs.getString(1).toLowerCase()));
    }

    @Override
    public Map<String, JdbcFieldType> getColumnTypes(String tableName) {
        var result = new LinkedHashMap<String, JdbcFieldType>();
        template.queryForList("SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME=? ORDER BY ORDINAL_POSITION ASC", tableName.toUpperCase())
                .forEach(map -> {
                    var dataType = ((String) map.get("DATA_TYPE"));
                    var maxLength = ((Number) map.get("CHARACTER_MAXIMUM_LENGTH"));
                    var sqlType = switch (dataType) {
                        case "CHARACTER VARYING" -> maxLength.intValue() > 256 ? JdbcFieldType.TEXT : JdbcFieldType.STRING;
                        case "INTEGER" -> JdbcFieldType.INT;
                        case "TIMESTAMP WITH TIME ZONE" -> JdbcFieldType.INSTANT;
                        case "JSON" -> JdbcFieldType.JSON;
                        default -> throw new UnsupportedOperationException("type %s %s is not supported".formatted(dataType, maxLength));
                    };
                    result.put(((String) map.get("COLUMN_NAME")).toLowerCase(), sqlType);
                });
        return result;
    }

    @Override
    public Map<String, JdbcIndexDescription> getIndexes(String tableName) {
        var result = new LinkedHashMap<String, JdbcIndexDescription>();
        template.queryForList("SELECT INDEX_NAME, COLUMN_NAME, TYPE FROM INFORMATION_SCHEMA.SYSTEM_INDEXINFO WHERE TABLE_NAME=? ORDER BY ORDINAL_POSITION ASC", tableName.toUpperCase())
                .forEach(map -> {
                    var indexName = ((String) map.get("INDEX_NAME")).toLowerCase();
                    var type = ((Number) map.get("TYPE")).intValue();
                    var columnName = ((String) map.get("COLUMN_NAME")).toLowerCase();
                    if (type != 3) {
                        throw new UnsupportedOperationException("index type %s is not supported".formatted(type));
                    }
                    result.put(indexName, new JdbcIndexDescription(columnName, JdbcIndexType.BTREE));
                });
        return result;
    }

    @Override
    public String getSqlType(JdbcFieldType value) {
        return switch (value) {
            case UUID -> "UUID";
            case INT_ID -> "INT PRIMARY KEY";
            case STRING -> "CHAR VARYING(255)";
            case BOOLEAN -> "BOOLEAN";
            case TEXT -> "LONGVARCHAR";
            case DATE -> "DATE";
            case DATE_TIME -> "TIMESTAMP(2)";
            case INSTANT -> "TIMESTAMP WITH TIME ZONE";
            case LONG -> "BIGINT";
            case INT -> "INT";
            case BIG_DECIMAL -> "DECIMAL(19,2)";
            case BLOB -> "BLOB";
            case STRING_ARRAY -> "CHAR VARYING(255) ARRAY";
            case LONG_ARRAY -> "BIGINT ARRAY";
            case JSON -> "LONGVARCHAR";
            case UUID_ARRAY -> "UUID ARRAY";
            case INT_ARRAY -> "INT ARRAY";
            case UUID_ID -> "UUID PRIMARY KEY";
            case TEXT_SEARCH -> "LONGVARCHAR";
        };
    }

    @Override
    public String createDropIndexQuery(String tableName, String index) {
        return "DROP INDEX %s IF EXISTS".formatted(index.toUpperCase());
    }

    @Override
    public String getCreateIndexSql(String tableName, String indexName, JdbcIndexDescription value) {
        return "CREATE INDEX %s ON %s (%s)".formatted(indexName.toUpperCase(), tableName.toUpperCase(), value.field().toUpperCase());
    }

    @Override
    public Set<String> getSequencesNames() {
        return new LinkedHashSet<>(template.query("SELECT SEQUENCE_NAME FROM INFORMATION_SCHEMA.SYSTEM_SEQUENCES WHERE SEQUENCE_SCHEMA != 'SYSTEM_LOBS' ORDER BY SEQUENCE_NAME ASC",
                (rs, rowNum) -> rs.getString(1).toLowerCase()));
    }

    @Override
    public String getDeleteSequenceSql(String sequenceName) {
        return "DROP SEQUENCE %s".formatted(sequenceName.toUpperCase());
    }

    @Override
    public String getCreateSequenceSql(JdbcSequenceDescription sequence) {
        return "CREATE SEQUENCE %s AS %s".formatted(sequence.sequenceName().toUpperCase(), "INT");
    }

    @Override
    public String getSequenceNextValueSql(String sequenceName) {
        return "VALUES NEXT VALUE FOR %s".formatted(sequenceName.toUpperCase());
    }

    @Override
    public void setBlob(PreparedStatement ps, int idx, DatabaseBinaryData value) throws Exception {
        ps.setBlob(idx, new JDBCBlob(value.content()));
    }

    @Override
    public DatabaseBinaryData readBlob(ResultSet ps, String fieldName) throws Exception {
        var bytes = ps.getBytes(fieldName);
        if (bytes == null) {
            return null;
        }
        return new DatabaseBinaryData(null, bytes);
    }

    @Override
    public String getCardinalitySql(String property) {
        return "CARDINALITY(%s)".formatted(property);
    }

    @Override
    public void deleteBlob(Connection conn, Long id) {
        //noops
    }

    @Override
    public String getIlikeFunctionName() {
        return "like";
    }

    @Override
    public String createIndexExtensionsSql(JdbcIndexType type) {
        return "select 1";
    }

    @Override
    public boolean hasArraysIntersectionOperationSupport() {
        return false;
    }

    @Override
    public boolean hasArraysAnyOperationSupport() {
        return false;
    }

    @Override
    public Pair<Object, JdbcFieldType> writeJson(String string) {
        return Pair.of(string, JdbcFieldType.STRING);
    }

    @Override
    public String readJson(ResultSet rs, String fieldName) throws Exception{
        return rs.getString(fieldName);
    }

    @Override
    public boolean hasFullTextSearchSupport() {
        return false;
    }
}
