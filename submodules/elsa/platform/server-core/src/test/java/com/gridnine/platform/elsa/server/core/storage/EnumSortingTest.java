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

package com.gridnine.platform.elsa.server.core.storage;

import com.gridnine.platform.elsa.common.core.search.SearchQueryBuilder;
import com.gridnine.platform.elsa.common.core.search.SortOrder;
import com.gridnine.platform.elsa.common.core.test.model.domain.TestDomainDocument;
import com.gridnine.platform.elsa.common.core.test.model.domain.TestDomainDocumentProjection;
import com.gridnine.platform.elsa.common.core.test.model.domain.TestDomainDocumentProjectionFields;
import com.gridnine.platform.elsa.common.core.test.model.domain.TestEnum;
import com.gridnine.platform.elsa.common.core.utils.LocaleUtils;
import com.gridnine.platform.elsa.core.auth.AuthContext;
import com.gridnine.platform.elsa.core.storage.Storage;
import com.gridnine.platform.elsa.server.core.common.ServerCoreTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class EnumSortingTest extends ServerCoreTestBase {

    @Autowired
    private Storage storage;

    @Test
    public void testSorting() {
        AuthContext.setCurrentUser("system");
        LocaleUtils.setCurrentLocale(LocaleUtils.ruLocale);
        {
            var doc = new TestDomainDocument();
            doc.setEnumProperty(TestEnum.ITEM1);
            storage.saveDocument(doc, "version1");
        }
        {
            var doc = new TestDomainDocument();
            doc.setEnumProperty(TestEnum.ITEM2);
            storage.saveDocument(doc, "version1");
        }
        var idxs = storage.searchDocuments(TestDomainDocumentProjection.class, new SearchQueryBuilder().orderBy(TestDomainDocumentProjectionFields.enumProperty, SortOrder.ASC).build());
        Assertions.assertEquals(2, idxs.size());
        Assertions.assertEquals(TestEnum.ITEM1, idxs.get(0).getEnumProperty());
        Assertions.assertEquals(TestEnum.ITEM2, idxs.get(1).getEnumProperty());
    }
}
