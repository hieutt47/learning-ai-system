package vn.edu.learningai.service;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.service.lesson.DocumentTextExtractorStrategy;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * Strategy dùng Apache Tika để trích xuất văn bản từ tài liệu nhị phân có cấu trúc (PDF, DOCX).
 */
@Component
public class TikaDocumentTextExtractor implements DocumentTextExtractorStrategy {
    private static final int MAX_EXTRACTED_CHARS = 500_000;
    private static final Set<String> SUPPORTED_MEDIA_TYPES = Set.of(
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final Tika tika;

    public TikaDocumentTextExtractor() {
        this.tika = new Tika();
        this.tika.setMaxStringLength(MAX_EXTRACTED_CHARS);
    }

    @Override
    public boolean supports(String mediaType) {
        return mediaType != null && SUPPORTED_MEDIA_TYPES.contains(mediaType.toLowerCase(java.util.Locale.ROOT));
    }

    @Override
    public String extract(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            String text = tika.parseToString(input);
            return text == null || text.isBlank() ? null : text.trim();
        } catch (TikaException exception) {
            throw new ApiException(ErrorCode.UNSUPPORTED_FILE_TYPE, "Could not extract text from document");
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.STORAGE_ERROR, "Could not read document stream");
        }
    }
}
