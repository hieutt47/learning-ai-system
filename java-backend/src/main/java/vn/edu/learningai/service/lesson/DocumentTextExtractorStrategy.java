package vn.edu.learningai.service.lesson;

import org.springframework.web.multipart.MultipartFile;

/**
 * Strategy interface cho đa hình trích xuất văn bản theo loại tệp (OCP):
 * bổ sung định dạng mới chỉ cần thêm Strategy mới mà không sửa LessonService.
 */
public interface DocumentTextExtractorStrategy {
    boolean supports(String mediaType);

    String extract(MultipartFile file);
}
