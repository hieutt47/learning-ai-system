package vn.edu.learningai.service.lesson;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * Strategy đọc trực tiếp văn bản thuần UTF-8 cho tệp TXT (nhanh, bỏ BOM, giới hạn 500.000 ký tự).
 */
@Component
public class PlainTextDocumentExtractor implements DocumentTextExtractorStrategy {
    static final int MAX_EXTRACTED_CHARS = 500_000;
    private static final int BUFFER_SIZE = 4096;

    @Override
    public boolean supports(String mediaType) {
        return "text/plain".equalsIgnoreCase(mediaType);
    }

    @Override
    public String extract(MultipartFile file) {
        StringBuilder result = new StringBuilder();
        char[] buffer = new char[BUFFER_SIZE];
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            int remaining = MAX_EXTRACTED_CHARS;
            int read;
            while (remaining > 0 && (read = reader.read(buffer, 0, Math.min(buffer.length, remaining))) != -1) {
                result.append(buffer, 0, read);
                remaining -= read;
            }
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.STORAGE_ERROR, "Could not read text document");
        }
        String text = result.toString();
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
            text = text.substring(1);
        }
        return text.isBlank() ? null : text.trim();
    }
}
