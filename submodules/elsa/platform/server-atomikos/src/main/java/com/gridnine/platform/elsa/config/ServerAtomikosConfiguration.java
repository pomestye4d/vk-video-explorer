/*****************************************************************
 * Gridnine AB http://www.gridnine.com
 * Project: LDocs
 *****************************************************************/

package com.gridnine.platform.elsa.config;

import com.gridnine.platform.elsa.server.atomikos.SimpleAtomikosJdbcDatabaseFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServerAtomikosConfiguration {
    @Bean
    public SimpleAtomikosJdbcDatabaseFactory simpleAtomikosJdbcDatabaseFactory(){
        return new SimpleAtomikosJdbcDatabaseFactory();
    }
}
