package vn.edu.learningai.storage;

/**
 * Result of persisting a file.
 *
 * @param storageKey server-generated name, the only thing stored in the database
 *                   ({@code thumbnail_storage_key}); never exposed to clients
 * @param url        client-facing URL ({@code thumbnail_url}), an API route, not a storage path
 */
public record StoredFile(String storageKey, String url) {
}
