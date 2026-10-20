/*****************************************************************
 * Gridnine AB http://www.gridnine.com
 * Project: LDocs
 *****************************************************************/

package com.gridnine.platform.elsa.core.storage.database.jdbc;

import java.util.Set;

public interface JdbcDatabaseCustomizer {
    String KEY = "JDBC_DATABASE_CUSTOMIZER";
    Set<String> getIgnoredTableNames();
}
