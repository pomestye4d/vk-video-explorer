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

package com.gridnine.platform.elsa.core.storage.database.jdbc;

import com.gridnine.platform.elsa.common.core.l10n.SupportedLocalesProvider;
import com.gridnine.platform.elsa.common.core.model.common.ClassMapper;
import com.gridnine.platform.elsa.common.core.model.common.EnumMapper;
import com.gridnine.platform.elsa.common.core.model.common.Xeption;
import com.gridnine.platform.elsa.common.core.model.domain.CaptionProvider;
import com.gridnine.platform.elsa.common.core.reflection.ReflectionFactory;
import com.gridnine.platform.elsa.common.core.utils.Lazy;
import com.gridnine.platform.elsa.common.meta.custom.CustomMetaRegistry;
import com.gridnine.platform.elsa.common.meta.domain.DomainMetaRegistry;
import com.gridnine.platform.elsa.core.cache.CacheManager;
import com.gridnine.platform.elsa.core.cache.CacheMetadataProvider;
import com.gridnine.platform.elsa.core.storage.database.Database;
import com.gridnine.platform.elsa.core.storage.database.DatabaseFactory;
import com.gridnine.platform.elsa.core.storage.database.jdbc.adapter.JdbcDataSourceProvider;
import com.gridnine.platform.elsa.core.storage.database.jdbc.adapter.JdbcDialect;
import com.gridnine.platform.elsa.core.storage.database.jdbc.model.JdbcDatabaseMetadataProvider;
import com.gridnine.platform.elsa.core.storage.database.jdbc.structureUpdater.JdbcStructureManualUpdateHandler;
import com.gridnine.platform.elsa.core.storage.database.jdbc.structureUpdater.JdbcStructureUpdater;
import com.gridnine.platform.elsa.core.storage.standard.JdbcCaptionProviderImpl;
import com.gridnine.platform.elsa.core.storage.transaction.DefaultElsaTransactionManager;
import com.gridnine.platform.elsa.core.storage.transaction.ElsaTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SimpleJdbcDatabaseFactory implements DatabaseFactory {

    @Autowired
    private AbstractBeanFactory abstractBeanFactory;

    private Database database;

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @Autowired
    private List<JdbcDataSourceProvider> providers;

    @Autowired
    private JdbcDatabaseMetadataProvider jdbcDatabaseMetadataProvider;

    @Autowired
    private DomainMetaRegistry domainMetaRegistry;

    @Autowired
    private CustomMetaRegistry customMetaRegistry;

    @Autowired
    private ReflectionFactory reflectionFactory;

    @Autowired
    private SupportedLocalesProvider supportedLocalesProvider;

    @Autowired
    private Environment env;

    @Autowired
    private CacheMetadataProvider cacheMetadataProvider;

    @Autowired
    private CacheManager cacheManager;

    @Autowired(required = false)
    private List<JdbcStructureManualUpdateHandler> manualUpdateHandlers;

    private ClassMapper classMapper;

    private EnumMapper enumMapper;

    private ElsaTransactionManager transactionManager;

    private DataSource ds;

    private CaptionProvider captionProvider;

    @Override
    public String getId() {
        return "SIMPLE";
    }

    @Override
    public void init(String configurationPrefix, Map<String,Object> customParameters) throws Exception {
        String adapterId = abstractBeanFactory.resolveEmbeddedValue("${%s.adapter:#{null}}".formatted(configurationPrefix));
        String url = abstractBeanFactory.resolveEmbeddedValue("${%s.url:#{null}}".formatted(configurationPrefix));
        int poolSize = Integer.parseInt(abstractBeanFactory.resolveEmbeddedValue("${%s.poolSize:5}".formatted(configurationPrefix)));
        String login = abstractBeanFactory.resolveEmbeddedValue("${%s.login:#{null}}".formatted(configurationPrefix));
        String password = abstractBeanFactory.resolveEmbeddedValue("${%s.password:#{null}}".formatted(configurationPrefix));
        var adapter = providers.stream().filter(it -> it.getId().equals(adapterId)).findFirst().orElse(null);
        if (adapter == null) {
            throw Xeption.forDeveloper("unsupported adapter id: %s".formatted(adapterId));
        }
        var props = new HashMap<String, Object>();
        props.put("login", login);
        props.put("password", password);
        props.put("poolSize", poolSize);
        props.put("url", url);
        ds = adapter.createDataSource(props);
        JdbcDialect dialect = adapter.createDialect(ds);
        var autoCommitTemplate = new JdbcTemplate(ds);
        var noAutoCommitDataSource = new DataSource() {
            @Override
            public <T> T unwrap(Class<T> iface) throws SQLException {
                return ds.unwrap(iface);
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) throws SQLException {
                return ds.isWrapperFor(iface);
            }

            @Override
            public Connection getConnection() throws SQLException {
                var conn = ds.getConnection();
                conn.setAutoCommit(false);
                return conn;
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                var conn = ds.getConnection(username, password);
                conn.setAutoCommit(false);
                return conn;
            }

            @Override
            public PrintWriter getLogWriter() throws SQLException {
                return ds.getLogWriter();
            }

            @Override
            public void setLogWriter(PrintWriter out) throws SQLException {
                ds.setLogWriter(out);
            }

            @Override
            public void setLoginTimeout(int seconds) throws SQLException {
                ds.setLoginTimeout(seconds);
            }

            @Override
            public int getLoginTimeout() throws SQLException {
                return ds.getLoginTimeout();
            }

            @Override
            public Logger getParentLogger() throws SQLFeatureNotSupportedException {
                return ds.getParentLogger();
            }
        };
        var storageTemplate = new JdbcTemplate(noAutoCommitDataSource);
        JdbcStructureUpdater.updateStructure(autoCommitTemplate, dialect, jdbcDatabaseMetadataProvider, manualUpdateHandlers, customParameters, (JdbcDatabaseCustomizer) customParameters.get(JdbcDatabaseCustomizer.KEY));
        classMapper = new JdbcClassMapperImpl(domainMetaRegistry, customMetaRegistry, autoCommitTemplate, dialect);
        enumMapper = new JdbcEnumMapperImpl(domainMetaRegistry, autoCommitTemplate, supportedLocalesProvider, dialect);
        captionProvider = new JdbcCaptionProviderImpl(env, cacheMetadataProvider, new Lazy<>(() ->database), cacheManager, supportedLocalesProvider);
        database = new JdbcDatabase(storageTemplate, jdbcDatabaseMetadataProvider, enumMapper, classMapper,captionProvider,
                dialect, domainMetaRegistry, reflectionFactory,customParameters);
        transactionManager = new DefaultElsaTransactionManager(new JdbcTransactionManager(noAutoCommitDataSource));
    }

    @Override
    public Database getPrimaryDatabase() {
        return database;
    }

    @Override
    public ClassMapper getClassMapper() {
        return classMapper;
    }

    @Override
    public EnumMapper getEnumMapper() {
        return enumMapper;
    }

    @Override
    public ElsaTransactionManager getTransactionManager() {
        return transactionManager;
    }

    @Override
    public CaptionProvider getCaptionProvider() {
        return captionProvider;
    }
}

