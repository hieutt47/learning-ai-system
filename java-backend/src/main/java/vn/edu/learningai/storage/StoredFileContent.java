package vn.edu.learningai.storage;

import org.springframework.core.io.Resource;

import java.util.Optional;

/**
 * A stored file opened for download.
 *
 * @param imageType detected format; empty for files that are not validated images
 */
public record StoredFileContent(Resource resource, Optional<ImageType> imageType) {
}
