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

package com.gridnine.platform.elsa.server.atomikos.test;

import com.gridnine.platform.elsa.common.core.search.SearchQuery;
import com.gridnine.platform.elsa.common.core.test.model.domain.TestDomainDocument;
import com.gridnine.platform.elsa.common.core.test.model.domain.TestDomainDocumentProjection;
import com.gridnine.platform.elsa.core.auth.AuthContext;
import com.gridnine.platform.elsa.core.storage.Storage;
import com.gridnine.platform.elsa.core.storage.transaction.ElsaTransactionManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class AtomikosStorageTest extends AtomikosTestBase {

    @Autowired
    private Storage storage;

    @Autowired
    private ElsaTransactionManager tm;

    @Test
    public void testTransaction() {
        AuthContext.setCurrentUser("system");
        try {
            tm.withTransaction((tc) -> {
                var doc = new TestDomainDocument();
                doc.setStringProperty("test");
                storage.saveDocument(doc, "version1");
                throw new Exception("test");
            });
        } catch (Throwable e) {
            Assertions.assertEquals(0, storage.searchDocuments(TestDomainDocumentProjection.class, new SearchQuery()).size());
        }
        tm.withTransaction((tc) -> {
            var doc = new TestDomainDocument();
            doc.setStringProperty("test");
            storage.saveDocument(doc, "version1");
        });
        Assertions.assertEquals(1, storage.searchDocuments(TestDomainDocumentProjection.class, new SearchQuery()).size());
    }

}
