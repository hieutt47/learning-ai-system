package vn.edu.learningai.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {
    private Path location = Path.of("./uploads");

    private String publicBaseUrl = "/api/v1/files";
}
