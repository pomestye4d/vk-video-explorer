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

package com.gridnine.platform.elsa.postgres;

import com.gridnine.platform.elsa.core.storage.database.jdbc.adapter.JdbcDataSourceProvider;
import com.gridnine.platform.elsa.core.storage.database.jdbc.adapter.JdbcDialect;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.postgresql.Driver;
import org.postgresql.xa.PGXADataSource;

import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.util.Map;

public class PostgresqlDataSourceProvider implements JdbcDataSourceProvider {

    @Override
    public ComboPooledDataSource createDataSource(Map<String, Object> props) throws Exception {
        ComboPooledDataSource ds = new ComboPooledDataSource();
        ds.setDriverClass(Driver.class.getName());
        ds.setJdbcUrl((String) props.get("url"));
        ds.setInitialPoolSize(1);
        ds.setAcquireIncrement(5);
        ds.setMinPoolSize(1);
        ds.setMaxPoolSize((Integer) props.get("poolSize"));
        ds.setUser((String) props.get("login"));
        ds.setPassword((String) props.get("password"));
        ds.setAutoCommitOnClose(false);
        return ds;
    }

    @Override
    public XADataSource createXADataSource(Map<String, Object> properties) {
        PGXADataSource ds = new PGXADataSource();
        ds.setURL((String) properties.get("url"));
        ds.setUser((String) properties.get("login"));
        ds.setPassword((String) properties.get("password"));
        return ds;
    }

    @Override
    public JdbcDialect createDialect(DataSource ds) {
        return new PostgresDialect(ds);
    }

    @Override
    public String getId() {
        return "postgres";
    }

}
