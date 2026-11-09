package vn.edu.learningai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.repository.CourseRepository;
import vn.edu.learningai.storage.FileStorage;
import vn.edu.learningai.storage.ImageType;
import vn.edu.learningai.storage.StorageException;
import vn.edu.learningai.storage.StoredFileContent;

import java.util.Optional;

/**
 * Decides who may read a stored file.
 *
 * <p>Thumbnails are public by contract: {@code thumbnail_url} appears in the
 * guest marketplace response, so reading one requires only that it is
 * referenced by an existing course. A key that is not referenced is reported
 * as 404 even if the file happens to be on disk, so the endpoint cannot be
 * used to enumerate or probe storage.</p>
 */
@Service
@RequiredArgsConstructor
public class FileAccessService {
    private final CourseRepository courseRepository;
    private final FileStorage fileStorage;

    public StoredFileContent getThumbnail(String storageKey) {
        if (!courseRepository.existsByThumbnailStorageKey(storageKey)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "File not found");
        }
        // A referenced row whose bytes are gone is an inconsistency, not a 404:
        // StorageException surfaces as a logged 500 rather than hiding it.
        return new StoredFileContent(fileStorage.load(storageKey), imageTypeOf(storageKey));
    }

    private static Optional<ImageType> imageTypeOf(String storageKey) {
        int dot = storageKey.lastIndexOf('.');
        if (dot < 0) {
            return Optional.empty();
        }
        String extension = storageKey.substring(dot + 1);
        for (ImageType type : ImageType.values()) {
            if (type.extension().equals(extension)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
