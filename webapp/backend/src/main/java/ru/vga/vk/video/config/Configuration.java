package ru.vga.vk.video.config;

import com.gridnine.platform.elsa.core.storage.standard.StandardStorageFactory;
import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration
public class Configuration {
    @Bean
    public StandardStorageFactory simpleStorageFactory() {
        return new StandardStorageFactory();
    }
}
