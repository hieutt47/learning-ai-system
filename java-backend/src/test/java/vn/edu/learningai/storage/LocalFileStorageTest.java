package vn.edu.learningai.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageTest {
    @TempDir
    Path root;

    private LocalFileStorage storage;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties();
        properties.setLocation(root.resolve("files"));
        properties.setPublicBaseUrl("/api/v1/files/");
        storage = new LocalFileStorage(properties);
    }

    @Test
    void storesUnderGeneratedKeyAndIgnoresClientNames() {
        StoredFile stored = storage.store("bytes".getBytes(), "png");

        assertThat(stored.storageKey()).matches("[0-9a-f-]{36}\\.png");
        assertThat(stored.url()).isEqualTo("/api/v1/files/" + stored.storageKey());
        assertThat(root.resolve("files").resolve(stored.storageKey())).exists();
    }

    @Test
    void eachUploadGetsItsOwnKey() {
        StoredFile first = storage.store("a".getBytes(), "png");
        StoredFile second = storage.store("a".getBytes(), "png");

        assertThat(first.storageKey()).isNotEqualTo(second.storageKey());
    }

    @Test
    void existsReportsOnlyKnownFiles() {
        StoredFile stored = storage.store("a".getBytes(), "png");

        assertThat(storage.exists(stored.storageKey())).isTrue();
        assertThat(storage.exists("00000000-0000-0000-0000-000000000000.png")).isFalse();
    }

    @Test
    void traversalKeysAreNeverResolved() {
        assertThat(storage.exists("../outside.png")).isFalse();
        assertThat(storage.exists("/etc/passwd")).isFalse();
        assertThat(storage.exists("a/b.png")).isFalse();

        assertThatThrownBy(() -> storage.load("../outside.png"))
            .isInstanceOf(StorageException.class)
            .hasMessageContaining("Invalid storage key");
    }

    @Test
    void deleteIsIdempotentAndIgnoresUnknownKeys() {
        StoredFile stored = storage.store("a".getBytes(), "png");

        storage.delete(stored.storageKey());
        storage.delete(stored.storageKey());
        storage.delete("nonsense/../x");

        assertThat(storage.exists(stored.storageKey())).isFalse();
    }

    @Test
    void loadReadsTheStoredBytes() throws Exception {
        StoredFile stored = storage.store("payload".getBytes(), "jpg");

        Path path = root.resolve("files").resolve(stored.storageKey());
        assertThat(Files.readString(path)).isEqualTo("payload");
        assertThat(storage.load(stored.storageKey()).getContentAsByteArray()).isEqualTo("payload".getBytes());
    }

    @Test
    void loadFailsForMissingFile() {
        assertThatThrownBy(() -> storage.load("00000000-0000-0000-0000-000000000000.png"))
            .isInstanceOf(StorageException.class)
            .hasMessageContaining("not found");
    }
}
