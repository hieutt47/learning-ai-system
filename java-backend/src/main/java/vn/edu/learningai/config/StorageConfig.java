package vn.edu.learningai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.edu.learningai.storage.FileStorage;
import vn.edu.learningai.storage.LocalFileStorage;
import vn.edu.learningai.storage.StorageProperties;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean
    public FileStorage fileStorage(StorageProperties properties) {
        return new LocalFileStorage(properties);
    }
}
