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

package com.gridnine.platform.elsa.postgres;

import com.gridnine.platform.elsa.common.core.utils.IoUtils;
import com.gridnine.platform.elsa.common.core.utils.Pair;
import com.gridnine.platform.elsa.core.storage.database.DatabaseBinaryData;
import com.gridnine.platform.elsa.core.storage.database.jdbc.adapter.JdbcDialect;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcFieldType;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcIndexDescription;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcIndexType;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcSequenceDescription;
import org.postgresql.PGConnection;
import org.postgresql.largeobject.LargeObjectManager;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class PostgresDialect implements JdbcDialect {

    private final JdbcTemplate template;

    public PostgresDialect(DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
    }

    @Override
    public Set<String> getTableNames() {
        return new LinkedHashSet<>(template.query("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'public'", (rs, rowNum) ->
                rs.getString(1)));
    }

    @Override
    public Map<String, JdbcFieldType> getColumnTypes(String tableName) {
        var result = new LinkedHashMap<String, JdbcFieldType>();
        template.queryForList(
                "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, UDT_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ?", tableName).forEach(map -> {
            var columnName = (String) map.get("COLUMN_NAME");
            var maxLength = (Integer) map.get("CHARACTER_MAXIMUM_LENGTH");
            var dataTypeStr = (String) map.get("DATA_TYPE");
            var udtName = (String) map.get("UDT_NAME");
            result.put(columnName, getType(dataTypeStr, maxLength, columnName, udtName));
        });
        return result;
    }

    private JdbcFieldType getType(String dataType, Number maxLength, String columnName, String udtName) {
        if ("character varying".equalsIgnoreCase(dataType)) {
            return JdbcFieldType.STRING;
        }
        if ("text".equalsIgnoreCase(dataType)) {
            return JdbcFieldType.TEXT;
        }
        if ("timestamp without time zone".equalsIgnoreCase(dataType)) {
            return JdbcFieldType.DATE_TIME;
        }
        if ("timestamp with time zone".equalsIgnoreCase(dataType)) {
            return JdbcFieldType.INSTANT;
        }
        if ("date".equalsIgnoreCase(dataType)) {
            return JdbcFieldType.DATE;
        }
        if ("oid".equalsIgnoreCase(dataType)) {
            return JdbcFieldType.BLOB;
        }
        if ("uuid".equalsIgnoreCase(dataType)) {
            return "id".equals(columnName)? JdbcFieldType.UUID_ID: JdbcFieldType.UUID;
        }
        if ("bigint".equalsIgnoreCase(dataType)) {
            return JdbcFieldType.LONG;
        }
        if ("integer".equalsIgnoreCase(dataType)) {
            return "id".equalsIgnoreCase(columnName) ? JdbcFieldType.INT_ID : JdbcFieldType.INT;
        }
        if ("numeric".equalsIgnoreCase(dataType)) {
            return JdbcFieldType.BIG_DECIMAL;
        }
        if ("boolean".equalsIgnoreCase(dataType)) {
            return JdbcFieldType.BOOLEAN;
        }
        if ("ARRAY".equalsIgnoreCase(dataType)) {
            if("_int4".equals(udtName)){
                return JdbcFieldType.INT_ARRAY;
            } else if("_uuid".equals(udtName)){
                return JdbcFieldType.UUID_ARRAY;
            } else {
                return JdbcFieldType.STRING_ARRAY;
            }
        }
        if ("jsonb".equalsIgnoreCase(dataType)) {
            return JdbcFieldType.JSON;
        }
        if ("tsvector".equalsIgnoreCase(dataType)) {
            return JdbcFieldType.TEXT_SEARCH;
        }
        throw new IllegalStateException("unsupported type: %s".formatted(dataType));
    }

    @Override
    public Map<String, JdbcIndexDescription> getIndexes(String tableName) {
        var result = new LinkedHashMap<String, JdbcIndexDescription>();
        template.queryForList("SELECT indexname, indexdef FROM pg_indexes WHERE schemaname = 'public' AND tablename = ?", tableName)
                .forEach(map -> {
                    var indexName = ((String) map.get("indexname"));
                    if (!indexName.endsWith("_pkey")) {
                        var indexdef = ((String) map.get("indexdef"));
                        var idx = indexName.lastIndexOf("_");
                        var columnName = indexName.startsWith(tableName + "_") ? indexName.substring(tableName.length() + 1) : indexName.substring(idx + 1);
                        var indexType = indexdef.contains("USING btree") ? JdbcIndexType.BTREE
                                : indexdef.contains("gin_trgm_ops") ? JdbcIndexType.GIN : JdbcIndexType.GIN_TSVECTOR;
                        result.put(indexName, new JdbcIndexDescription(columnName, indexType));
                    }
                });
        return result;
    }

    @Override
    public String getSqlType(JdbcFieldType value) {
        return switch (value) {
            case INT_ID -> "INT PRIMARY KEY";
            case STRING -> "CHAR VARYING(255)";
            case BOOLEAN -> "BOOLEAN";
            case TEXT -> "TEXT";
            case DATE -> "DATE";
            case DATE_TIME -> "timestamp without time zone";
            case INSTANT -> "timestamptz";
            case LONG -> "BIGINT";
            case INT -> "INT";
            case BIG_DECIMAL -> "numeric(19,2)";
            case JSON -> "JSONB";
            case BLOB -> "OID";
            case STRING_ARRAY -> "CHAR VARYING(255)[]";
            case LONG_ARRAY -> "BIGINT ARRAY";
            case INT_ARRAY -> "INT ARRAY";
            case UUID -> "UUID";
            case UUID_ID -> "UUID PRIMARY KEY";
            case UUID_ARRAY -> "UUID ARRAY";
            case TEXT_SEARCH -> "tsvector";
        };
    }

    @Override
    public String createDropIndexQuery(String tableName, String index) {
        return "DROP INDEX IF EXISTS %s".formatted(index);
    }

    @Override
    public String getCreateIndexSql(String tableName, String indexName, JdbcIndexDescription value) {
        return switch (value.type()) {
            case BTREE -> "CREATE INDEX %s ON %s USING btree(%s)".formatted(indexName, tableName, value.field());
            case GIN -> "CREATE INDEX %s ON %s USING gin(%s gin_trgm_ops)".formatted(indexName, tableName, value.field());
            case GIN_TSVECTOR -> "CREATE INDEX %s ON %s USING gin(%s)".formatted(indexName, tableName, value.field());
        };
    }

    @Override
    public Set<String> getSequencesNames() {
        return new LinkedHashSet<>(template.query("SELECT sequence_name FROM information_schema.sequences",
                (rs, rowNum) -> rs.getString(1)));
    }

    @Override
    public String getDeleteSequenceSql(String sequenceName) {
        return "DROP SEQUENCE %s".formatted(sequenceName);
    }

    @Override
    public String getCreateSequenceSql(JdbcSequenceDescription sequence) {
        return "CREATE SEQUENCE %s".formatted(sequence.sequenceName());
    }

    @Override
    public String getSequenceNextValueSql(String sequenceName) {
        return "select nextval('%s')".formatted(sequenceName);
    }

    @Override
    public void setBlob(PreparedStatement ps, int idx, DatabaseBinaryData value) throws Exception {
        var lobj = ps.getConnection().unwrap(PGConnection.class).getLargeObjectAPI();
        if (value.id() != null) {
            lobj.delete(value.id());
        }
        var oid = lobj.createLO(LargeObjectManager.WRITE | LargeObjectManager.READ);
        try (var obj = lobj.open(oid, LargeObjectManager.WRITE)) {
            obj.write(value.content());
        }
        ps.setLong(idx, oid);
    }

    @Override
    public DatabaseBinaryData readBlob(ResultSet rs, String fieldName) throws Exception {
        var lobj = rs.getStatement().getConnection().unwrap(PGConnection.class).getLargeObjectAPI();
        var oidVal = rs.getLong(fieldName);
        try (var obj = lobj.open(oidVal, LargeObjectManager.READ)) {
            var baos = new ByteArrayOutputStream();
            IoUtils.copy(obj.getInputStream(), baos);
            return new DatabaseBinaryData(oidVal, baos.toByteArray());
        }
    }

    @Override
    public String getCardinalitySql(String property) {
        return "cardinality(%s)".formatted(property);
    }

    @Override
    public void deleteBlob(Connection conn, Long id) throws SQLException {
        var lobj = conn.unwrap(PGConnection.class).getLargeObjectAPI();
        lobj.delete(id);
    }

    @Override
    public String getIlikeFunctionName() {
        return "ilike";
    }

    @Override
    public String createIndexExtensionsSql(JdbcIndexType type) {
        if (type == JdbcIndexType.GIN_TSVECTOR) {
            return "";
        }
        return "CREATE EXTENSION pg_trgm; CREATE EXTENSION btree_gin;";
    }

    @Override
    public boolean hasArraysIntersectionOperationSupport() {
        return true;
    }

    @Override
    public boolean hasArraysAnyOperationSupport() {
        return true;
    }

    @Override
    public Pair<Object, JdbcFieldType> writeJson(String string) throws Exception {
        var jsonObject = new PGobject();
        jsonObject.setType("jsonb");
        jsonObject.setValue(string);
        return Pair.of(jsonObject, JdbcFieldType.JSON);
    }

    @Override
    public String readJson(ResultSet rs, String fieldName) throws Exception {
        return rs.getString(fieldName);
    }

    @Override
    public boolean hasFullTextSearchSupport() {
        return true;
    }
}
