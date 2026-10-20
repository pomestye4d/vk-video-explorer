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

import com.gridnine.platform.elsa.common.core.test.model.domain.TestDomainDocument;
import com.gridnine.platform.elsa.common.core.test.model.domain.TestDomainDocumentProjection;
import com.gridnine.platform.elsa.core.storage.SearchableProjectionHandler;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TestDomainDocumentProjectionHandler implements SearchableProjectionHandler<TestDomainDocument, TestDomainDocumentProjection> {
    @Override
    public Class<TestDomainDocument> getDocumentClass() {
        return TestDomainDocument.class;
    }

    @Override
    public Class<TestDomainDocumentProjection> getProjectionClass() {
        return TestDomainDocumentProjection.class;
    }

    @Override
    public List<TestDomainDocumentProjection> createProjections(TestDomainDocument doc, Set<String> props) {
        var proj = new TestDomainDocumentProjection();
        proj.setEnumProperty(doc.getEnumProperty());
        proj.setGetAllProperty(doc.getGetAllProperty());
        proj.setEntityReference(doc.getEntityReference());
        proj.setStringProperty(doc.getStringProperty());
        proj.getEntityRefCollection().addAll(doc.getEntityRefCollection());
        proj.getEnumCollection().addAll(doc.getEnumCollection());
        proj.getStringCollection().addAll(doc.getStringCollection());
        proj.setInstantProperty(doc.getInstantProperty());
        return Collections.singletonList(proj);
    }
}
