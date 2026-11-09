package vn.edu.learningai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.learningai.service.FileAccessService;
import vn.edu.learningai.storage.ImageType;
import vn.edu.learningai.storage.StoredFileContent;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class FileController {
    private final FileAccessService fileAccessService;
    @GetMapping("/files/{storageKey}")
    public ResponseEntity<Resource> download(@PathVariable String storageKey) {
        StoredFileContent file = fileAccessService.getThumbnail(storageKey);
        MediaType mediaType = file.imageType()
            .map(FileController::mediaTypeOf)
            .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
            .contentType(mediaType)
            .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
            .header("X-Content-Type-Options", "nosniff")
            .body(file.resource());
    }
    private static MediaType mediaTypeOf(ImageType type) {
        return switch (type) {
            case PNG -> MediaType.IMAGE_PNG;
            case JPEG -> MediaType.IMAGE_JPEG;
            case WEBP -> MediaType.parseMediaType("image/webp");
        };
    }
}
