package vn.edu.learningai.storage;

/** The file could not be written or read; callers translate this into a 500. */
public class StorageException extends RuntimeException {
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public StorageException(String message) {
        super(message);
    }
}
