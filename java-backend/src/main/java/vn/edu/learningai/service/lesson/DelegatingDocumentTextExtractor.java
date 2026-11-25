package vn.edu.learningai.service.lesson;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.service.DocumentTextExtractor;

import java.util.List;
import java.util.Locale;

/**
 * Bộ điều phối Strategy trích xuất văn bản (Strategy Pattern):
 * tự chọn chiến lược phù hợp theo mediaType (PlainText cho TXT, Tika cho PDF/DOCX).
 */
@Primary
@Service
@RequiredArgsConstructor
public class DelegatingDocumentTextExtractor implements DocumentTextExtractor {
    private final List<DocumentTextExtractorStrategy> strategies;

    @Override
    public String extract(MultipartFile file) {
        return extract(file, inferMediaType(file));
    }

    @Override
    public String extract(MultipartFile file, String mediaType) {
        return strategies.stream()
            .filter(strategy -> strategy.supports(mediaType))
            .findFirst()
            .orElseThrow(() -> new ApiException(ErrorCode.UNSUPPORTED_FILE_TYPE, "Unsupported document format"))
            .extract(file);
    }

    private String inferMediaType(MultipartFile file) {
        String name = file == null || file.getOriginalFilename() == null
            ? ""
            : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (name.endsWith(".txt")) {
            return "text/plain";
        }
        if (name.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (name.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        return file == null ? null : file.getContentType();
    }
}
