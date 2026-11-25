package vn.edu.learningai.service.lesson;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;

/**
 * Kiểm tra định dạng thực tế, chuẩn hóa tên tệp an toàn đa nền tảng và tính SHA-256 theo luồng
 * cho tài liệu bài học (PDF, DOCX, TXT <= 10 MiB).
 */
@Component
public class LessonDocumentInspector {
    public static final long MAX_DOCUMENT_BYTES = 10L * 1024 * 1024;
    private static final int HASH_BUFFER_SIZE = 8192;
    private static final int BINARY_PROBE_BYTES = 8192;
    private static final String PDF_MEDIA_TYPE = "application/pdf";
    private static final String TXT_MEDIA_TYPE = "text/plain";
    private static final String DOCX_MEDIA_TYPE =
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final Set<String> TEXT_COMPATIBLE_APPLICATIONS = Set.of(
        "application/json",
        "application/xml",
        "application/javascript",
        "application/x-sh",
        "application/x-shellscript",
        "application/xhtml+xml"
    );

    private final Tika tika = new Tika();

    public InspectedDocument inspect(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Document is empty");
        }
        if (file.getSize() > MAX_DOCUMENT_BYTES) {
            throw new ApiException(ErrorCode.FILE_TOO_LARGE);
        }

        String originalName = sanitizeOriginalName(file.getOriginalFilename());
        String mediaType = detectAndValidateMediaType(file, originalName);
        String contentHash = sha256(file);
        return new InspectedDocument(originalName, mediaType, file.getSize(), contentHash);
    }

    public String sanitizeOriginalName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return "document";
        }
        int lastSlash = Math.max(rawName.lastIndexOf('/'), rawName.lastIndexOf('\\'));
        String baseName = lastSlash >= 0 ? rawName.substring(lastSlash + 1) : rawName;
        StringBuilder cleaned = new StringBuilder(baseName.length());
        for (int i = 0; i < baseName.length(); i++) {
            char ch = baseName.charAt(i);
            if (ch >= 0x20 && ch != 0x7F) {
                cleaned.append(ch);
            }
        }
        String result = cleaned.toString().trim();
        if (result.isEmpty() || result.equals(".") || result.equals("..")) {
            return "document";
        }
        if (result.length() <= 255) {
            return result;
        }
        int dot = result.lastIndexOf('.');
        if (dot > 0 && result.length() - dot <= 16) {
            String ext = result.substring(dot);
            return result.substring(0, 255 - ext.length()) + ext;
        }
        return result.substring(result.length() - 255);
    }

    private String detectAndValidateMediaType(MultipartFile file, String originalName) {
        String lowerName = originalName.toLowerCase(Locale.ROOT);
        String detected;
        try (InputStream input = file.getInputStream()) {
            detected = tika.detect(input, lowerName);
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.STORAGE_ERROR, "Could not inspect document");
        }

        if (lowerName.endsWith(".pdf")) {
            if (PDF_MEDIA_TYPE.equals(detected)) {
                return PDF_MEDIA_TYPE;
            }
            throw new ApiException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
        if (lowerName.endsWith(".docx")) {
            if (DOCX_MEDIA_TYPE.equals(detected)) {
                return DOCX_MEDIA_TYPE;
            }
            throw new ApiException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
        if (lowerName.endsWith(".txt")) {
            boolean textSubtype = TXT_MEDIA_TYPE.equals(detected)
                || (detected != null && detected.startsWith("text/"))
                || TEXT_COMPATIBLE_APPLICATIONS.contains(detected);
            if (textSubtype && isPlainTextContent(file)) {
                return TXT_MEDIA_TYPE;
            }
            throw new ApiException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
        throw new ApiException(ErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    private boolean isPlainTextContent(MultipartFile file) {
        byte[] buffer = new byte[BINARY_PROBE_BYTES];
        try (InputStream input = file.getInputStream()) {
            int read = input.read(buffer);
            for (int i = 0; i < read; i++) {
                if (buffer[i] == 0x00) {
                    return false;
                }
            }
            return true;
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.STORAGE_ERROR, "Could not inspect document");
        }
    }

    private String sha256(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[HASH_BUFFER_SIZE];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new ApiException(ErrorCode.STORAGE_ERROR, "Could not hash document");
        }
    }

    public record InspectedDocument(
        String originalName,
        String mediaType,
        long sizeBytes,
        String contentHash
    ) {}
}
