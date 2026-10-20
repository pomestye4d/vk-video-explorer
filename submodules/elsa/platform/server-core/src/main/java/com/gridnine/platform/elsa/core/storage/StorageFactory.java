/*****************************************************************
 * Gridnine AB http://www.gridnine.com
 * Project: LDocs
 *****************************************************************/

package com.gridnine.platform.elsa.core.storage;

import com.gridnine.platform.elsa.common.core.model.common.ClassMapper;
import com.gridnine.platform.elsa.common.core.model.common.EnumMapper;
import com.gridnine.platform.elsa.common.core.model.domain.CaptionProvider;
import com.gridnine.platform.elsa.core.storage.transaction.ElsaTransactionManager;

import java.util.Map;

public interface StorageFactory {
    String getId();
    void init(String configPrefix, Map<String,Object> customParameters) throws Exception;
    Storage getStorage();
    ClassMapper getClassMapper();
    EnumMapper getEnumMapper();
    CaptionProvider getCaptionProvider();
    ElsaTransactionManager getTransactionManager();
}
