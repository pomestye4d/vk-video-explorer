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

import com.gridnine.platform.elsa.core.storage.database.jdbc.adapter.JdbcDataSourceProvider;
import com.gridnine.platform.elsa.core.storage.database.jdbc.adapter.JdbcDialect;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.hsqldb.jdbc.JDBCDriver;
import org.hsqldb.jdbc.pool.JDBCXADataSource;

import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class HsqldbDataSourceProvider implements JdbcDataSourceProvider {
    private static final AtomicInteger idx = new AtomicInteger(0);

    @Override
    public DataSource createDataSource(Map<String, Object> props) throws Exception {
        ComboPooledDataSource ds = new ComboPooledDataSource();
        ds.setDriverClass(JDBCDriver.class.getName());
        ds.setJdbcUrl("jdbc:hsqldb:mem:elsa-%s;shutdown=true".formatted(idx.incrementAndGet()));
//        ds.setJdbcUrl("jdbc:hsqldb:file:/home/avramenko/IdeaProjects/pomestye4d/platform/elsa/temp/db/elsa;shutdown=true");
        ds.setInitialPoolSize(1);
        ds.setAcquireIncrement(5);
        ds.setMinPoolSize(1);
        ds.setMaxPoolSize(5);
        ds.setUser("SA");
        ds.setPassword("");
        ds.setAutoCommitOnClose(false);
        return ds;
    }

    @Override
    public XADataSource createXADataSource(Map<String, Object> properties) throws Exception {
        var ds = new JDBCXADataSource();
        ds.setURL("jdbc:hsqldb:mem:elsa-%s;shutdown=true".formatted(idx.get()));
        ds.setUser("SA");
        ds.setPassword("");
        return ds;
    }

    @Override
    public JdbcDialect createDialect(DataSource ds) {
        return new HsqldbDialect(ds);
    }

    @Override
    public String getId() {
        return "hsqldb";
    }

}
