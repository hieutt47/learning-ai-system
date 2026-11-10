package vn.edu.learningai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.storage.FileStorage;
import vn.edu.learningai.storage.ImageType;
import vn.edu.learningai.storage.StoredFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class ThumbnailService {
    private final FileStorage fileStorage;
    @Value("${app.upload.max-thumbnail-bytes:5242880}")
    private long maxBytes;
    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Thumbnail file is required");
        }

        if (file.getSize() > maxBytes) {
            throw tooLarge();
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Thumbnail could not be read");
        }
        if (content.length > maxBytes) {
            throw tooLarge();
        }
        ImageType type = ImageType.detect(content)
            .orElseThrow(() -> new ApiException(ErrorCode.UNSUPPORTED_FILE_TYPE,
                "Thumbnail must be a JPEG, PNG or WebP image"));
        return fileStorage.store(content, type.extension());
    }

    public void deleteAfterCommit(String storageKey) {
        if (storageKey == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            fileStorage.delete(storageKey);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                fileStorage.delete(storageKey);
            }
        });
    }
    private ApiException tooLarge() {
        return new ApiException(ErrorCode.FILE_TOO_LARGE, "Thumbnail must be at most 5 MiB");
    }
}
