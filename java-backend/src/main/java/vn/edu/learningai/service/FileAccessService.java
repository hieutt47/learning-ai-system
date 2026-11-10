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

@Service
@RequiredArgsConstructor
public class FileAccessService {
    private final CourseRepository courseRepository;
    private final FileStorage fileStorage;
    public StoredFileContent getThumbnail(String storageKey) {
        if (!courseRepository.existsByThumbnailStorageKey(storageKey)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "File not found");
        }
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
