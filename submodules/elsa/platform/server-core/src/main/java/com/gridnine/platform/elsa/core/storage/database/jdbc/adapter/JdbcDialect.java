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

package com.gridnine.platform.elsa.core.storage.database.jdbc.adapter;

import com.gridnine.platform.elsa.common.core.utils.Pair;
import com.gridnine.platform.elsa.core.storage.database.DatabaseBinaryData;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcFieldType;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcIndexDescription;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcIndexType;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcSequenceDescription;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

public interface JdbcDialect {
    Set<String> getTableNames();

    Map<String, JdbcFieldType> getColumnTypes(String tableName);

    Map<String, JdbcIndexDescription> getIndexes(String tableName);

    String getSqlType(JdbcFieldType value);

    String createDropIndexQuery(String tableName, String index);

    String getCreateIndexSql(String tableName, String key, JdbcIndexDescription value);

    Set<String> getSequencesNames();

    String getDeleteSequenceSql(String sequenceName);

    String getCreateSequenceSql(JdbcSequenceDescription sequence);

    String getSequenceNextValueSql(String sequenceName);

    void setBlob(PreparedStatement ps, int idx, DatabaseBinaryData value) throws Exception;

    DatabaseBinaryData readBlob(ResultSet ps, String fieldName) throws Exception;

    String getCardinalitySql(String property);

    void deleteBlob(Connection cnn, Long id) throws SQLException;

    String getIlikeFunctionName();

    String createIndexExtensionsSql(JdbcIndexType type);

    boolean hasArraysIntersectionOperationSupport();

    boolean hasArraysAnyOperationSupport();

    Pair<Object, JdbcFieldType> writeJson(String string) throws Exception;

    String readJson(ResultSet rs, String fieldName) throws Exception;

    boolean hasFullTextSearchSupport();
}
