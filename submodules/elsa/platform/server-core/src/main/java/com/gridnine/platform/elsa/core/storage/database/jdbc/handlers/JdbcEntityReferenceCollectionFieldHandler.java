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

import java.sql.ResultSet;
import java.util.*;

public class JdbcEntityReferenceCollectionFieldHandler implements JdbcFieldHandler {

    private final String fieldName;

    private final boolean indexed;

    private final boolean isAbstract;

    private final boolean storeCaptions;

    private final String typeFieldName;

    private final String captionFieldName;

    private final Class<?> cls;

    public JdbcEntityReferenceCollectionFieldHandler(String fieldName, boolean indexed, Class<?> cls, boolean isAbstract, boolean storeCaptions) {
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
        result.put(fieldName, JdbcFieldType.UUID_ARRAY);
        if (isAbstract) {
            result.put(typeFieldName, JdbcFieldType.INT_ARRAY);
        }
        if (storeCaptions) {
            result.put(captionFieldName, JdbcFieldType.STRING_ARRAY);
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
    public Object getModelValue(ResultSet rs, EnumMapper enumMapper, ClassMapper classMapper, CaptionProvider captionProvider, ReflectionFactory factory, JdbcDialect
            dialiect, SimpleCriterion activeCriterion) throws Exception {
        var idJdbcValues = rs.getArray(fieldName);
        if (idJdbcValues == null) {
            return Collections.emptyList();
        }
        var idValues = Arrays.stream((Object[]) idJdbcValues.getArray()).map(it -> ((UUID) it).toString())
                .toList();
        var typeValues = new ArrayList<Class<?>>();
        if (isAbstract) {
            var typeJdbcValues = rs.getArray(typeFieldName);
            Arrays.stream((Object[]) typeJdbcValues.getArray()).map(it -> (Integer) it)
                    .forEach((it) -> typeValues.add(factory.getClass(classMapper.getName(it))));
        } else {
            for (int n = 0; n < idValues.size(); n++) {
                typeValues.add(cls);
            }
        }
        var captionsValues = new ArrayList<String>();
        if (storeCaptions) {
            var captionsJdbcValues = rs.getArray(captionFieldName);
            captionsValues.addAll(Arrays.stream((Object[]) idJdbcValues.getArray()).map(it -> (String) it).toList());
        } else {
            for (int n = 0; n < idValues.size(); n++) {
                captionsValues.add(captionProvider.getCaption(new EntityReference<>(idValues.get(n), (Class) typeValues.get(n), null)));
            }
        }
        var result = new ArrayList<EntityReference<?>>();
        for (int n = 0; n < idValues.size(); n++) {
            result.add(new EntityReference<>(idValues.get(n), (Class<BaseIdentity>) typeValues.get(n), captionsValues.get(n)));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Pair<Object, JdbcFieldType>> getSqlValues(Object value, EnumMapper enumMapper, ClassMapper classMapper, ReflectionFactory factory, JdbcDialect dialect) throws Exception {
        var result = new LinkedHashMap<String, Pair<Object, JdbcFieldType>>();
        var ref = (List<EntityReference<?>>) value;
        result.put(fieldName, Pair.of(ref.stream().map(EntityReference::getId).toList(), JdbcFieldType.UUID_ARRAY));
        if (isAbstract) {
            result.put(typeFieldName, Pair.of(ref.stream().map(it -> classMapper.getId(it.getType().getName())).toList(), JdbcFieldType.INT_ARRAY));
        }
        if (storeCaptions) {
            result.put(typeFieldName, Pair.of(ref.stream().map(EntityReference::getCaption).toList(), JdbcFieldType.STRING_ARRAY));
        }
        return result;
    }

    @Override
    public Pair<Object, JdbcFieldType> getSqlQueryValue(Object value, EnumMapper enumMapper, ClassMapper classMapper, ReflectionFactory factory) throws Exception {
        return Pair.of(value == null ? null : ((EntityReference<?>) value).getId(), JdbcFieldType.UUID);
    }

}
