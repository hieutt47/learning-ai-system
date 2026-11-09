package vn.edu.learningai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalDocumentStorage implements DocumentStorage {
    private final Path storageRoot;

    public LocalDocumentStorage(@Value("${app.storage.lesson-documents:./data/lesson-documents}") String storageRoot) {
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file) {
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "document" : file.getOriginalFilename());
        String extension = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : "";
        String storageKey = UUID.randomUUID() + extension.toLowerCase();
        try {
            Files.createDirectories(storageRoot);
            Files.copy(file.getInputStream(), resolve(storageKey), StandardCopyOption.REPLACE_EXISTING);
            return storageKey;
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
