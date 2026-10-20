/*****************************************************************
 * Gridnine AB http://www.gridnine.com
 * Project: Lunda.ru 2
 *****************************************************************/

package com.gridnine.platform.elsa.server.core.storage;

import com.gridnine.platform.elsa.common.core.test.model.domain.TestProjectSettings;
import com.gridnine.platform.elsa.common.core.test.model.domain.TestSettingsProjection;
import com.gridnine.platform.elsa.common.core.test.model.domain.TestSettingsProjectionFields;
import com.gridnine.platform.elsa.common.core.test.model.domain.TestSettingsType;
import com.gridnine.platform.elsa.core.storage.Storage;
import com.gridnine.platform.elsa.server.core.common.ServerCoreTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class StorageAbstractClassProjectionTest extends ServerCoreTestBase {

    @Autowired
    private Storage storage;

    @Test
    public void testAbstractClassProjection() {
        var projectSettings = new TestProjectSettings();
        projectSettings.setKey(TestSettingsType.TEST_TYPE);
        projectSettings.setSpecific("specific");
        projectSettings.setName("Test project settings");
        storage.saveDocument(projectSettings);
        var doc = (TestProjectSettings) storage.findUniqueDocument(TestSettingsProjection.class, TestSettingsProjectionFields.key, TestSettingsType.TEST_TYPE);
        Assertions.assertEquals(projectSettings.getSpecific(), doc.getSpecific());
        storage.findUniqueDocument(TestSettingsProjection.class, TestSettingsProjectionFields.key, TestSettingsType.TEST_TYPE);
    }
}
