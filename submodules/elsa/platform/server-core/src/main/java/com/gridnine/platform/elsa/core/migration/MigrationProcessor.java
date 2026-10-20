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

package com.gridnine.platform.elsa.core.migration;

import com.gridnine.platform.elsa.common.core.search.SearchQuery;
import com.gridnine.platform.elsa.core.auth.AuthContext;
import com.gridnine.platform.elsa.core.storage.Storage;
import com.gridnine.platform.elsa.server.core.MigrationRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class MigrationProcessor {

    @Autowired(required = false)
    private List<MigrationHandler> handlers;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${elsa.auth.systemUserName:system}")
    private String systemUserName;
    @Autowired
    private Storage storage;

    @PostConstruct
    public void processMigrations() {
        if ("true".equals(System.getProperty("elsa.testing"))) {
            logger.info("skipping migration in testing mode");
            return;
        }
        logger.info("starting migrations");
        if (handlers == null) {
            logger.info("no migration handlers found");
            return;
        }
        AuthContext.setCurrentUser(systemUserName);
        try {
            for (int i = 0; i < handlers.size(); i++) {
                for (int j = i + 1; j < handlers.size(); j++) {
                    var a = handlers.get(i);
                    var b = handlers.get(j);
                    if (depends(a, b)) {
                        Collections.swap(handlers, i, j);
                        i = -1;
                        break;
                    }
                }
            }
            var executors = Executors.newCachedThreadPool();
            try {
                var processedIds = storage.searchAssets(MigrationRecord.class, new SearchQuery()).stream().map(MigrationRecord::getMigrationId).collect(Collectors.toSet());
                handlers.forEach(handler -> {
                    if (processedIds.contains(handler.getId())) {
                        return;
                    }
                    if (handler.isAsync()) {
                        executors.execute(() -> {
                            try {
                                AuthContext.setCurrentUser(systemUserName);
                                try {
                                    processMigration(handler);
                                } finally {
                                    AuthContext.resetCurrentUser();
                                }
                            } catch (Throwable e) {
                                logger.error("unable to process migration {}", handler.getId(), e);
                            }
                        });
                        return;
                    }
                    try {
                        processMigration(handler);
                    } catch (Throwable e) {
                        logger.error("unable to process migration", e);
                        throw new RuntimeException(e);
                    }
                });
                logger.info("migrations completed");
            } finally {
                executors.shutdown();
            }
        }finally {
            AuthContext.resetCurrentUser();
        }
    }

    private void processMigration(MigrationHandler handler) throws Exception {
        logger.info("processing {} migration {}", handler.isAsync() ? "async" : "sync", handler.getId());
        handler.processMigration();
        addMigrationRecord(handler.getId());
        logger.info("processed migration {}", handler.getId());
    }

    private void addMigrationRecord(String id) {
        var migrationRecord = new MigrationRecord();
        migrationRecord.setMigrationId(id);
        migrationRecord.setDate(Instant.now());
        storage.saveAsset(migrationRecord, false, "migration " + id);
    }

    private boolean depends(MigrationHandler a, MigrationHandler b) {
        return a.getDependencies().stream().anyMatch(cls -> cls.equals(b.getClass()));
    }
}
