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
import com.gridnine.platform.elsa.common.core.model.domain.CaptionProvider;
import com.gridnine.platform.elsa.common.core.reflection.ReflectionFactory;
import com.gridnine.platform.elsa.common.core.search.SimpleCriterion;
import com.gridnine.platform.elsa.common.core.utils.Pair;
import com.gridnine.platform.elsa.core.storage.database.jdbc.adapter.JdbcDialect;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcFieldType;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcIndexDescription;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcIndexType;

import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JdbcEnumCollectionFieldHandler implements JdbcFieldHandler {

    private final String fieldName;
    private final boolean indexed;
    private final Class<Enum<?>> enumClass;

    public JdbcEnumCollectionFieldHandler(String fieldName, Class<Enum<?>> enumClass, boolean indexed) {
        this.fieldName = fieldName;
        this.indexed = indexed;
        this.enumClass = enumClass;
    }

    @Override
    public Map<String, JdbcFieldType> getColumns() {
        return Collections.singletonMap(fieldName, JdbcFieldType.INT_ARRAY);
    }

    @Override
    public Map<String, JdbcIndexDescription> getIndexes(String tableName) {
        return indexed ? Collections.singletonMap("%s_%s".formatted(tableName, fieldName), new JdbcIndexDescription(fieldName, JdbcIndexType.BTREE)) :
                Collections.emptyMap();
    }

    @Override
    public Object getModelValue(ResultSet rs, EnumMapper enumMapper, ClassMapper classMapper, CaptionProvider captionProvider, ReflectionFactory factory, JdbcDialect dialiect, SimpleCriterion activeCriterion) throws Exception {
        var jdbcValue = rs.getArray(fieldName);
        if (jdbcValue == null) {
            return Collections.emptyList();
        }
        return Arrays.stream((Object[]) jdbcValue.getArray()).map(it -> (Integer) it).map(it -> factory.safeGetEnum(enumClass, enumMapper.getName(it, enumClass))).toList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Pair<Object, JdbcFieldType>> getSqlValues(Object value, EnumMapper enumMapper, ClassMapper classMapper, ReflectionFactory factory, JdbcDialect dialect) throws Exception {
        return Collections.singletonMap(fieldName, Pair.of(((List<Enum<?>>) value).stream().map(enumMapper::getId).toList(), JdbcFieldType.INT_ARRAY));
    }

    @Override
    public Pair<Object, JdbcFieldType> getSqlQueryValue(Object value, EnumMapper enumMapper, ClassMapper classMapper, ReflectionFactory factory) throws Exception {
        return Pair.of(enumMapper.getId((Enum<?>) value), JdbcFieldType.INT);
    }

}
