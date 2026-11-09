package vn.edu.learningai.storage;

import org.springframework.core.io.Resource;

public interface FileStorage {
    StoredFile store(byte[] content, String extension);

    void delete(String storageKey);
    boolean exists(String storageKey);

    Resource load(String storageKey);
}
