package vn.edu.learningai.storage;

import org.springframework.core.io.Resource;

import java.util.Optional;

public record StoredFileContent(Resource resource, Optional<ImageType> imageType) {
}
