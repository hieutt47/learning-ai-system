package vn.edu.learningai.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {
    /** Directory that backs local file storage; created on first use. */
    private Path location = Path.of("./uploads");

    /**
     * Base URL the stored files are served from. File URLs stored in the
     * database are derived from it, so changing deployment only changes config.
     */
    private String publicBaseUrl = "/api/v1/files";
}
