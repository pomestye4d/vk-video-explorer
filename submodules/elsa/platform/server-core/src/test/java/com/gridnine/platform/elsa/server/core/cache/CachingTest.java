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

package com.gridnine.platform.elsa.server.core.cache;

import com.gridnine.platform.elsa.common.core.test.model.domain.TestDomainDocument;
import com.gridnine.platform.elsa.common.core.test.model.domain.TestDomainDocumentProjection;
import com.gridnine.platform.elsa.common.core.test.model.domain.TestDomainDocumentProjectionFields;
import com.gridnine.platform.elsa.core.auth.AuthContext;
import com.gridnine.platform.elsa.core.storage.Storage;
import com.gridnine.platform.elsa.server.core.common.ServerCoreTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {CacheConfiguration.class})
public class CachingTest extends ServerCoreTestBase {

    @Autowired
    private InvocationCountAdvice invocationCountAdvice;

    @Autowired
    private Storage storage;

    @Test
    public void testCache() {
        AuthContext.setCurrentUser("system");
        var doc = new TestDomainDocument();
        doc.setStringProperty("test");
        storage.saveDocument(doc);
        doc = storage.loadDocument(TestDomainDocument.class, doc.getId(), false);
        Assertions.assertEquals(1, invocationCountAdvice.getLoadDocumentCount());
        doc = storage.loadDocument(TestDomainDocument.class, doc.getId(), false);
        Assertions.assertEquals(1, invocationCountAdvice.getLoadDocumentCount());
        Assertions.assertTrue(doc.isSealed());
        doc = storage.loadDocument(TestDomainDocument.class, doc.getId(), true);
        Assertions.assertEquals(2, invocationCountAdvice.getLoadDocumentCount());
        doc.setStringProperty("test2");
        storage.saveDocument(doc);
        doc = storage.loadDocument(TestDomainDocument.class, doc.getId(), false);
        Assertions.assertTrue(doc.isSealed());
        Assertions.assertEquals(3, invocationCountAdvice.getLoadDocumentCount());
        var ref = storage.findUniqueDocumentReference(TestDomainDocumentProjection.class,
                TestDomainDocumentProjectionFields.stringProperty, "test2");
        Assertions.assertNotNull(ref);
        Assertions.assertEquals(1, invocationCountAdvice.getFindDocumentCount());
        ref = storage.findUniqueDocumentReference(TestDomainDocumentProjection.class,
                TestDomainDocumentProjectionFields.stringProperty, "test2");
        Assertions.assertNotNull(ref);
        Assertions.assertEquals(1, invocationCountAdvice.getFindDocumentCount());
        doc = storage.loadDocument(TestDomainDocument.class, doc.getId(), true);
        doc.setStringProperty("test");
        storage.saveDocument(doc);
        ref = storage.findUniqueDocumentReference(TestDomainDocumentProjection.class,
                TestDomainDocumentProjectionFields.stringProperty, "test");
        Assertions.assertNotNull(ref);
        Assertions.assertEquals(2, invocationCountAdvice.getFindDocumentCount());
        storage.deleteDocument(doc);
        ref = storage.findUniqueDocumentReference(TestDomainDocumentProjection.class,
                TestDomainDocumentProjectionFields.stringProperty, "test");
        Assertions.assertNull(ref);
    }

    @Test
    public void testGetAllCache() {
        invocationCountAdvice.setGetAllDocumentsCount(0);
        invocationCountAdvice.setLoadDocumentCount(0);
        AuthContext.setCurrentUser("system");
        {
            var doc = new TestDomainDocument();
            doc.setStringProperty("1");
            doc.setGetAllProperty("test");
            storage.saveDocument(doc);
            storage.loadDocument(TestDomainDocument.class, doc.getId(), false);
            Assertions.assertEquals(1, invocationCountAdvice.getLoadDocumentCount());
        }
        {
            var doc = new TestDomainDocument();
            doc.setStringProperty("2");
            doc.setGetAllProperty("test");
            storage.saveDocument(doc);
            storage.loadDocument(TestDomainDocument.class, doc.getId(), false);
            Assertions.assertEquals(2, invocationCountAdvice.getLoadDocumentCount());
        }

        {
            var docRefs = storage.getAllDocumentReferences(TestDomainDocumentProjection.class,
                    TestDomainDocumentProjectionFields.getAllProperty, "test");
            Assertions.assertEquals(2, docRefs.size());
            Assertions.assertEquals(1, invocationCountAdvice.getGetAllDocumentsCount());
            docRefs = storage.getAllDocumentReferences(TestDomainDocumentProjection.class,
                    TestDomainDocumentProjectionFields.getAllProperty, "test");
            Assertions.assertEquals(2, docRefs.size());
            Assertions.assertEquals(1, invocationCountAdvice.getGetAllDocumentsCount());
            var docs = storage.getAllDocuments(TestDomainDocumentProjection.class,
                    TestDomainDocumentProjectionFields.getAllProperty, "test", false);
            Assertions.assertEquals(2, docs.size());
            Assertions.assertEquals(2, invocationCountAdvice.getLoadDocumentCount());
            invocationCountAdvice.setGetAllDocumentsCount(0);
        }

        {
            var doc = storage.getAllDocuments(TestDomainDocumentProjection.class,
                            TestDomainDocumentProjectionFields.getAllProperty, "test", true).stream()
                    .filter(it -> "2".equals(it.getStringProperty())).findFirst().orElseThrow();
            Assertions.assertEquals(0, invocationCountAdvice.getGetAllDocumentsCount());
            doc.setStringProperty("3");
            storage.saveDocument(doc);
            var docRefs = storage.getAllDocumentReferences(TestDomainDocumentProjection.class,
                    TestDomainDocumentProjectionFields.getAllProperty, "test");
            Assertions.assertEquals(2, docRefs.size());
            Assertions.assertEquals(0, invocationCountAdvice.getGetAllDocumentsCount());
        }
        {
            var docs = storage.getAllDocuments(TestDomainDocumentProjection.class,
                    TestDomainDocumentProjectionFields.getAllProperty, "test", true);
            var doc = docs.stream()
                    .filter(it -> "1".equals(it.getStringProperty())).findFirst().orElse(null);
            storage.deleteDocument(doc);
            var docRefs = storage.getAllDocumentReferences(TestDomainDocumentProjection.class,
                    TestDomainDocumentProjectionFields.getAllProperty, "test");
            Assertions.assertEquals(1, docRefs.size());
            Assertions.assertEquals(1, invocationCountAdvice.getGetAllDocumentsCount());
        }
    }
}
