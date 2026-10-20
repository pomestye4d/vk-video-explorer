/*****************************************************************
 * Gridnine AB http://www.gridnine.com
 * Project: LDocs
 *****************************************************************/

package com.gridnine.platform.elsa.core.storage.standard;

import com.gridnine.platform.elsa.common.core.l10n.SupportedLocalesProvider;
import com.gridnine.platform.elsa.common.core.lock.LockManager;
import com.gridnine.platform.elsa.common.core.model.common.ClassMapper;
import com.gridnine.platform.elsa.common.core.model.common.EnumMapper;
import com.gridnine.platform.elsa.common.core.model.domain.CaptionProvider;
import com.gridnine.platform.elsa.common.meta.domain.DomainMetaRegistry;
import com.gridnine.platform.elsa.core.storage.Storage;
import com.gridnine.platform.elsa.core.storage.StorageFactory;
import com.gridnine.platform.elsa.core.storage.database.DatabaseFactory;
import com.gridnine.platform.elsa.core.storage.database.jdbc.JdbcDatabaseCustomizer;
import com.gridnine.platform.elsa.core.storage.transaction.ElsaTransactionManager;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanFactory;

import java.util.List;
import java.util.Map;

public class StandardStorageFactory implements StorageFactory {

    @Autowired
    private AbstractBeanFactory abstractBeanFactory;

    @Autowired
    private ListableBeanFactory factory;

    @Autowired
    private LockManager lockManager;

    @Autowired
    private DomainMetaRegistry metaRegistry;

    @Autowired
    private SupportedLocalesProvider localesProvider;

    @Autowired
    private List<DatabaseFactory> databaseFactories;

    @Autowired
    private DomainMetaRegistry domainMetaRegistry;

    private StandardStorage storage;

    @Autowired(required = false)
    private JdbcDatabaseCustomizer jdbcDatabaseCustomizer;

    @Override
    public String getId() {
        return "STANDARD";
    }

    @Override
    public void init(String prefix,Map<String,Object> customParameters) throws Exception {
          customParameters.put(JdbcDatabaseCustomizer.KEY, jdbcDatabaseCustomizer);
          storage = new StandardStorage(factory, lockManager, domainMetaRegistry, localesProvider, databaseFactories, abstractBeanFactory, prefix, customParameters);
    }

    @Override
    public StandardStorage getStorage() {
        return storage;
    }

    @Override
    public ClassMapper getClassMapper() {
        return storage.getClassMapper();
    }

    @Override
    public EnumMapper getEnumMapper() {
        return storage.getEnumMapper();
    }

    @Override
    public CaptionProvider getCaptionProvider() {
        return storage.getCaptionProvider();
    }

    @Override
    public ElsaTransactionManager getTransactionManager() {
        return storage.getTransactionManager();
    }
}
