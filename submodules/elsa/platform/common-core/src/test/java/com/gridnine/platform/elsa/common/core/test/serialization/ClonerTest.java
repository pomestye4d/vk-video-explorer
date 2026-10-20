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

package com.gridnine.platform.elsa.common.core.test.serialization;

import com.gridnine.elsa.common.core.common.TestBase;
import com.gridnine.platform.elsa.common.core.model.domain.EntityReference;
import com.gridnine.platform.elsa.common.core.serialization.Cloner;
import com.gridnine.platform.elsa.common.core.test.model.domain.TestDomainDocument;
import com.gridnine.platform.elsa.common.core.test.model.domain.TestDomainNestedDocumentImpl;
import com.gridnine.platform.elsa.common.core.test.model.domain.TestEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

public class ClonerTest extends TestBase {

    @Autowired
    private Cloner cloner;

    @Test
    public void testCloner() {
        var document = new TestDomainDocument();
        document.setEnumProperty(TestEnum.ITEM1);
        document.setInstantProperty(Instant.now());
        document.setStringProperty("string property");
        var entityRef = new EntityReference<TestDomainDocument>();
        entityRef.setType(TestDomainDocument.class);
        entityRef.setId(UUID.randomUUID().toString());
        entityRef.setCaption("entity reference caption");
        document.setEntityReference(entityRef);
        document.getStringCollection().add("value");
        document.getEnumCollection().add(TestEnum.ITEM2);
        document.getEntityRefCollection().add(entityRef);
        var entity = new TestDomainNestedDocumentImpl();
        entity.setName("nested name");
        entity.setValue("nested value");
        document.setEntityProperty(entity);

        var entity2 = new TestDomainNestedDocumentImpl();
        entity2.setName("nested name 2");
        entity2.setValue("nested value 2");
        document.getEntityCollection().add(entity2);

        var document2 = cloner.clone(document);
        Assertions.assertEquals(document.getStringProperty(), document2.getStringProperty());
        Assertions.assertEquals(document.getEnumProperty(), document2.getEnumProperty());
        Assertions.assertEquals(document.getEntityReference(), document2.getEntityReference());
        Assertions.assertEquals(document.getEntityReference().getCaption(), document2.getEntityReference().getCaption());
        Assertions.assertEquals(document.getEnumCollection(), document2.getEnumCollection());
        Assertions.assertEquals(document.getStringCollection(), document2.getStringCollection());
        Assertions.assertEquals(document.getEntityRefCollection(), document2.getEntityRefCollection());
        var ne1 = (TestDomainNestedDocumentImpl) document.getEntityCollection().get(0);
        var ne2 = (TestDomainNestedDocumentImpl) document2.getEntityCollection().get(0);
        Assertions.assertEquals(ne1.getName(), ne2.getName());
        Assertions.assertEquals(ne1.getValue(), ne2.getValue());
    }

}
