package vn.edu.learningai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class LocalDocumentStorage implements DocumentStorage {
    private static final Pattern SAFE_EXTENSION = Pattern.compile("^\\.[a-z0-9]{1,10}$");
    private final Path storageRoot;

    public LocalDocumentStorage(@Value("${app.storage.lesson-documents:./data/lesson-documents}") String storageRoot) {
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    @Override
    public String newKey(MultipartFile file) {
        String rawName = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
        int lastSlash = Math.max(rawName.lastIndexOf('/'), rawName.lastIndexOf('\\'));
        String baseName = lastSlash >= 0 ? rawName.substring(lastSlash + 1) : rawName;
        int dot = baseName.lastIndexOf('.');
        String extension = dot > 0 ? baseName.substring(dot).toLowerCase(Locale.ROOT) : "";
        if (!SAFE_EXTENSION.matcher(extension).matches()) {
            extension = "";
        }
        return UUID.randomUUID() + extension;
    }

    @Override
    public void store(String storageKey, MultipartFile file) {
        try {
            Files.createDirectories(storageRoot);
            try (var input = file.getInputStream()) {
                Files.copy(input, resolve(storageKey));
            }
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.STORAGE_ERROR);
        }
    }

    @Override
    public Resource load(String storageKey) {
        try {
            Resource resource = new UrlResource(resolve(storageKey).toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ApiException(ErrorCode.NOT_FOUND, "Document content not found");
            }
            return resource;
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.STORAGE_ERROR);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.STORAGE_ERROR);
        }
    }

    private Path resolve(String storageKey) {
        Path path = storageRoot.resolve(storageKey).normalize();
        if (!path.startsWith(storageRoot)) {
            throw new ApiException(ErrorCode.STORAGE_ERROR);
        }
        return path;
    }
}
