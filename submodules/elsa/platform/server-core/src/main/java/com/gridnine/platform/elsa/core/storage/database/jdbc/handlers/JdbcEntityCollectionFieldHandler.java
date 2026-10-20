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

import com.gridnine.platform.elsa.common.core.model.common.BaseIntrospectableObject;
import com.gridnine.platform.elsa.common.core.model.common.ClassMapper;
import com.gridnine.platform.elsa.common.core.model.common.EnumMapper;
import com.gridnine.platform.elsa.common.core.model.common.Xeption;
import com.gridnine.platform.elsa.common.core.model.domain.CaptionProvider;
import com.gridnine.platform.elsa.common.core.reflection.ReflectionFactory;
import com.gridnine.platform.elsa.common.core.search.SimpleCriterion;
import com.gridnine.platform.elsa.common.core.serialization.JsonMarshaller;
import com.gridnine.platform.elsa.common.core.serialization.JsonUnmarshaller;
import com.gridnine.platform.elsa.common.core.serialization.SerializationParameters;
import com.gridnine.platform.elsa.common.core.utils.Pair;
import com.gridnine.platform.elsa.common.meta.domain.DomainMetaRegistry;
import com.gridnine.platform.elsa.core.storage.database.jdbc.adapter.JdbcDialect;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcFieldType;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcUtils;
import org.springframework.beans.factory.ListableBeanFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JdbcEntityCollectionFieldHandler extends BaseJdbcSingleFieldHandler {
    private volatile JsonMarshaller jsonMarshaller;

    private volatile JsonUnmarshaller jsonUnmarshaller;

    private final SerializationParameters serializationParameters;

    private volatile DomainMetaRegistry domainMetaRegistry;

    private final Class<?> entityClass;

    private final  ListableBeanFactory beanFactory;

    public JdbcEntityCollectionFieldHandler(String fieldName, ListableBeanFactory beanFactory,  Class<?> ettClass) {
        super(fieldName, false, JdbcFieldType.JSON);
        this.serializationParameters = new SerializationParameters();
        serializationParameters.setPrettyPrint(false);
        serializationParameters.setEntityReferenceCaptionSerializationStrategy(SerializationParameters.EntityReferenceCaptionSerializationStrategy.ONLY_NOT_CACHED);
        serializationParameters.setEnumSerializationStrategy(SerializationParameters.EnumSerializationStrategy.ID);
        serializationParameters.setEntityReferenceTypeSerializationStrategy(SerializationParameters.EntityReferenceTypeSerializationStrategy.ABSTRACT_CLASS_ID);
        serializationParameters.setClassSerializationStrategy(SerializationParameters.ClassSerializationStrategy.ID);
        this.beanFactory = beanFactory;
        this.entityClass = ettClass;
    }

    private JsonMarshaller jsonMarshaller() {
        if(jsonMarshaller == null){
            jsonMarshaller = beanFactory.getBean(JsonMarshaller.class);
        }
        return jsonMarshaller;
    }
    private JsonUnmarshaller jsonUnmarshaller() {
        if(jsonUnmarshaller == null){
            jsonUnmarshaller = beanFactory.getBean(JsonUnmarshaller.class);
        }
        return jsonUnmarshaller;
    }

    private DomainMetaRegistry getDomainMetaRegistry() {
        if(domainMetaRegistry == null){
            domainMetaRegistry = beanFactory.getBean(DomainMetaRegistry.class);
        }
        return domainMetaRegistry;
    }

    @Override
    public Object getModelValue(ResultSet rs, EnumMapper enumMapper, ClassMapper classMapper, CaptionProvider captionProvider, ReflectionFactory factory, JdbcDialect dialect, SimpleCriterion activeCriterion) throws Exception {
        if(JdbcUtils.isNull(rs, fieldName)){
            return new ArrayList<>();
        }
        String json = dialect.readJson(rs, fieldName);
        return  jsonUnmarshaller().unmarshalCollection(this.entityClass, new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), serializationParameters);
    }

    @Override
    public Map<String, Pair<Object, JdbcFieldType>> getSqlValues(Object value, EnumMapper enumMapper, ClassMapper classMapper, ReflectionFactory factory, JdbcDialect dialect) throws Exception {
        var lst = (List<BaseIntrospectableObject>) value;
        if(lst.isEmpty()){
            return Collections.singletonMap(fieldName, Pair.of(null, JdbcFieldType.JSON));
        }
        var baos = new ByteArrayOutputStream();
        jsonMarshaller().marshalCollection(lst, baos, getDomainMetaRegistry().getEntities().get(this.entityClass.getName()).isAbstract(), serializationParameters);
        return Collections.singletonMap(fieldName, dialect.writeJson(baos.toString(StandardCharsets.UTF_8)));
    }

    @Override
    public Pair<Object, JdbcFieldType> getSqlQueryValue(Object value, EnumMapper enumMapper, ClassMapper classMapper, ReflectionFactory factory) throws Exception {
        throw Xeption.forDeveloper("unsupported");
    }
}
