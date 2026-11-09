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

/**
 * Validates an uploaded thumbnail before anything is persisted.
 *
 * <p>The servlet-level multipart limit in {@code application.yaml} is a second
 * line of defence: it only covers the raw request and rejects oversized
 * bodies before they reach a controller, with a 413 that carries our
 * {@code FILE_TOO_LARGE} code. This class enforces the same limit on the bytes
 * it actually stores, because the declared multipart size is client supplied.</p>
 */
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
        // Declared size is only a hint; the byte array below is authoritative.
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

    /**
     * Removes a file only once the surrounding transaction has committed.
     *
     * <p>Deleting inline would lose the previous thumbnail if the commit fails
     * afterwards (the row still points at it); running after commit costs at
     * most an orphaned file when the process dies in between, which is the
     * harmless direction.</p>
     */
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
