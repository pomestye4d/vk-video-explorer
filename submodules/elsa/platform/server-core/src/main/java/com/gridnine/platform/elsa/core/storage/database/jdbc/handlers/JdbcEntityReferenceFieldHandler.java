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

import com.gridnine.platform.elsa.common.core.model.common.BaseIdentity;
import com.gridnine.platform.elsa.common.core.model.common.ClassMapper;
import com.gridnine.platform.elsa.common.core.model.common.EnumMapper;
import com.gridnine.platform.elsa.common.core.model.domain.CaptionProvider;
import com.gridnine.platform.elsa.common.core.model.domain.EntityReference;
import com.gridnine.platform.elsa.common.core.reflection.ReflectionFactory;
import com.gridnine.platform.elsa.common.core.search.SimpleCriterion;
import com.gridnine.platform.elsa.common.core.utils.Pair;
import com.gridnine.platform.elsa.core.storage.database.jdbc.adapter.JdbcDialect;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcFieldType;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcIndexDescription;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcIndexType;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class JdbcEntityReferenceFieldHandler implements JdbcFieldHandler {


    private final String fieldName;

    private final boolean indexed;

    private final boolean isAbstract;

    private final boolean storeCaptions;

    private final Class<?> cls;

    private final String typeFieldName;

    private final String captionFieldName;

    public JdbcEntityReferenceFieldHandler(String fieldName, boolean indexed, Class<?> cls, boolean isAbstract, boolean storeCaptions) {
        this.fieldName = fieldName;
        this.indexed = indexed;
        this.isAbstract = isAbstract;
        this.storeCaptions = storeCaptions;
        this.typeFieldName = "%stype".formatted(fieldName);
        this.captionFieldName = "%scaption".formatted(fieldName);
        this.cls = cls;
    }

    @Override
    public Map<String, JdbcFieldType> getColumns() {
        final var result = new LinkedHashMap<String, JdbcFieldType>();
        result.put(fieldName, JdbcFieldType.UUID);
        if (isAbstract) {
            result.put(typeFieldName, JdbcFieldType.INT);
        }
        if (storeCaptions) {
            result.put(captionFieldName, JdbcFieldType.STRING);
        }
        return result;
    }

    @Override
    public Map<String, JdbcIndexDescription> getIndexes(String tableName) {
        if (!indexed) {
            return Collections.emptyMap();
        }
        return Collections.singletonMap("%s_%s".formatted(tableName, fieldName), new JdbcIndexDescription(fieldName, JdbcIndexType.BTREE));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object getModelValue(ResultSet rs, EnumMapper enumMapper, ClassMapper classMapper, CaptionProvider captionProvider, ReflectionFactory factory, JdbcDialect dialect, SimpleCriterion activeCriterion) throws SQLException {
        if (JdbcUtils.isNull(rs, fieldName)) {
            return null;
        }
        var id = rs.getObject(fieldName, UUID.class).toString();
        var cls = isAbstract ? factory.getClass(classMapper.getName(rs.getInt(typeFieldName))) : this.cls;
        var caption = storeCaptions ? rs.getString(captionFieldName) : captionProvider.getCaption(new EntityReference<>(id, (Class) cls, null));
        return new EntityReference<>(id, (Class<BaseIdentity>) cls, caption);
    }

    @Override
    public Map<String, Pair<Object, JdbcFieldType>> getSqlValues(Object value, EnumMapper enumMapper, ClassMapper classMapper, ReflectionFactory factory, JdbcDialect dialect) {
        var result = new LinkedHashMap<String, Pair<Object, JdbcFieldType>>();
        var ref = (EntityReference<?>) value;
        result.put(fieldName, Pair.of(ref == null ? null : ref.getId(), JdbcFieldType.UUID));
        if (isAbstract) {
            result.put(typeFieldName, Pair.of(ref == null ? null : classMapper.getId(ref.getType().getName()), JdbcFieldType.INT));
        }
        if (storeCaptions) {
            result.put(captionFieldName, Pair.of(ref == null ? null : ref.getCaption(), JdbcFieldType.STRING));
        }

        return result;
    }

    @Override
    public Pair<Object, JdbcFieldType> getSqlQueryValue(Object value, EnumMapper enumMapper, ClassMapper classMapper, ReflectionFactory factory) throws Exception {
        return Pair.of(value == null ? null : ((EntityReference<?>) value).getId(), JdbcFieldType.UUID);
    }

}
