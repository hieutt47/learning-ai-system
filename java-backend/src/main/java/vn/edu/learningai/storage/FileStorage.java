package vn.edu.learningai.storage;

import org.springframework.core.io.Resource;

/**
 * Storage adapter for uploaded files (thumbnail now, lesson documents later).
 * Swapping local disk for object storage must not touch services or controllers.
 */
public interface FileStorage {

    /**
     * Writes {@code content} under a server-generated name.
     *
     * @param extension chosen by the server from the detected file type, without the dot
     */
    StoredFile store(byte[] content, String extension);

    /**
     * Best-effort removal of a previously stored file. Unknown keys are ignored
     * so that re-uploading or cleaning up twice cannot fail a request.
     */
    void delete(String storageKey);

    /**
     * @return false for unknown keys and for keys that are not valid storage
     * names, so path traversal attempts read as "not found" instead of an error
     */
    boolean exists(String storageKey);

    /**
     * @throws StorageException if the key is invalid or the file disappeared
     *                          between {@link #exists(String)} and this call
     */
    Resource load(String storageKey);
}
