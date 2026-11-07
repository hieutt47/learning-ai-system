package vn.edu.learningai.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentStorage {
    String store(MultipartFile file);

    Resource load(String storageKey);

    void delete(String storageKey);
}
