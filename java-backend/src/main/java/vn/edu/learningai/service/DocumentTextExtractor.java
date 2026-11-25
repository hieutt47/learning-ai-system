package vn.edu.learningai.service;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentTextExtractor {
    String extract(MultipartFile file);

    default String extract(MultipartFile file, String mediaType) {
        return extract(file);
    }
}
