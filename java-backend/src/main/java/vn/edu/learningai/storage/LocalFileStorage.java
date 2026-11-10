package vn.edu.learningai.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
public class LocalFileStorage implements FileStorage {
    private static final Pattern KEY_PATTERN = Pattern.compile("[A-Za-z0-9-]{1,64}\\.[a-z0-9]{1,8}");
    private final Path root;
    private final String publicBaseUrl;
    public LocalFileStorage(StorageProperties properties) {
        this.root = canonicalRoot(properties.getLocation());
        this.publicBaseUrl = stripTrailingSlash(properties.getPublicBaseUrl());
    }
    @Override
    public StoredFile store(byte[] content, String extension) {
        String key = UUID.randomUUID() + "." + extension;
        Path target = resolve(key);
        try {
            Files.createDirectories(root);
            Files.write(target, content, java.nio.file.StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new StorageException("Could not write stored file", e);
        }
        return new StoredFile(key, publicBaseUrl + "/" + key);
    }
    @Override
    public void delete(String storageKey) {
        if (!isValidKey(storageKey)) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException e) {
            log.warn("Could not delete stored file {}", storageKey, e);
        }
    }
    @Override
    public boolean exists(String storageKey) {
        return isValidKey(storageKey) && Files.isRegularFile(resolve(storageKey));
    }
    @Override
    public Resource load(String storageKey) {
        if (!isValidKey(storageKey)) {
            throw new StorageException("Invalid storage key");
        }
        Path path = resolve(storageKey);
        if (!Files.isRegularFile(path)) {
            throw new StorageException("Stored file not found");
        }
        return new FileSystemResource(path);
    }
    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new StorageException("Invalid storage key");
        }
        return path;
    }
    private boolean isValidKey(String storageKey) {
        return StringUtils.hasText(storageKey) && KEY_PATTERN.matcher(storageKey).matches();
    }
    private static Path canonicalRoot(Path location) {
        Path absolute = location.toAbsolutePath().normalize();
        try {
            Files.createDirectories(absolute);
            return absolute.toRealPath();
        } catch (IOException e) {
            throw new StorageException("Storage directory is not usable: " + absolute, e);
        }
    }
    private static String stripTrailingSlash(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
