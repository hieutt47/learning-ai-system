package vn.edu.learningai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.storage.FileStorage;
import vn.edu.learningai.storage.StoredFile;
import vn.edu.learningai.support.TestImages;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit-level rules: which bytes are accepted, and with which extension. */
class ThumbnailServiceTest {

    private final List<String> storedExtensions = new ArrayList<>();
    private final List<String> deletedKeys = new ArrayList<>();
    private ThumbnailService service;

    @BeforeEach
    void setUp() {
        FileStorage recordingStorage = new FileStorage() {
            @Override
            public StoredFile store(byte[] content, String extension) {
                storedExtensions.add(extension);
                return new StoredFile("key." + extension, "/api/v1/files/key." + extension);
            }

            @Override
            public void delete(String storageKey) {
                deletedKeys.add(storageKey);
            }

            @Override
            public boolean exists(String storageKey) {
                return false;
            }

            @Override
            public org.springframework.core.io.Resource load(String storageKey) {
                throw new UnsupportedOperationException();
            }
        };
        service = new ThumbnailService(recordingStorage);
        ReflectionTestUtils.setField(service, "maxBytes", 5L * 1024 * 1024);
    }

    @Test
    void storesPngWithServerChosenExtension() {
        StoredFile stored = service.store(file("whatever.txt", TestImages.png()));

        assertThat(storedExtensions).containsExactly("png");
        assertThat(stored.storageKey()).isEqualTo("key.png");
        assertThat(stored.url()).isEqualTo("/api/v1/files/key.png");
    }

    @Test
    void jpegExtensionIsJpgNotJpeg() {
        service.store(file("photo.jpeg", TestImages.jpeg()));

        assertThat(storedExtensions).containsExactly("jpg");
    }

    @Test
    void webpIsAccepted() {
        service.store(file("x.webp", TestImages.webp()));

        assertThat(storedExtensions).containsExactly("webp");
    }

    @Test
    void textPayloadIsUnsupportedRegardlessOfFilename() {
        assertThatThrownBy(() -> service.store(file("logo.png", TestImages.svg())))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.UNSUPPORTED_FILE_TYPE);
        assertThat(storedExtensions).isEmpty();
    }

    @Test
    void emptyFileIsValidationError() {
        assertThatThrownBy(() -> service.store(file("x.png", new byte[0])))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void nullFileIsValidationError() {
        assertThatThrownBy(() -> service.store(null))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void oversizedFileIsFileTooLargeAndNothingIsStored() {
        byte[] tooBig = TestImages.ofSize(5 * 1024 * 1024 + 1);

        assertThatThrownBy(() -> service.store(file("big.png", tooBig)))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.FILE_TOO_LARGE);
        assertThat(storedExtensions).isEmpty();
    }

    @Test
    void exactlyTheLimitIsAccepted() {
        service.store(file("big.png", TestImages.ofSize(5 * 1024 * 1024)));

        assertThat(storedExtensions).containsExactly("png");
    }

    @Test
    void cleanupIgnoresNullKeysSoCoursesWithoutThumbnailsAreSafe() {
        service.deleteAfterCommit(null);

        assertThat(deletedKeys).isEmpty();
    }

    @Test
    void cleanupWithoutTransactionDeletesImmediately() {
        service.deleteAfterCommit("key.png");

        assertThat(deletedKeys).containsExactly("key.png");
    }

    private MockMultipartFile file(String filename, byte[] content) {
        return new MockMultipartFile("file", filename, "application/octet-stream", content);
    }
}
