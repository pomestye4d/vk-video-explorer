package com.gridnine.platform.elsa.core.storage.database.jdbc.structureUpdater;

import com.gridnine.platform.elsa.core.storage.database.jdbc.adapter.JdbcDialect;
import org.springframework.jdbc.core.JdbcTemplate;

public interface JdbcStructureManualUpdateHandler {
    String getId();

    void execute(JdbcTemplate template, JdbcDialect dialect) throws Exception;

    default boolean executeAfterAutoStructureUpdate(){return false;};
}
