package vn.edu.learningai.service.lesson;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LessonDocumentInspectorTest {
    private final LessonDocumentInspector inspector = new LessonDocumentInspector();

    @Test
    void acceptsPlainTextAndCodeSnippetsInTxtFiles() {
        var htmlTxt = new MockMultipartFile(
            "file", "lecture-html.txt", "application/octet-stream",
            "<html><body><h1>Java OOP</h1></body></html>".getBytes(StandardCharsets.UTF_8)
        );
        var inspected = inspector.inspect(htmlTxt);

        assertThat(inspected.originalName()).isEqualTo("lecture-html.txt");
        assertThat(inspected.mediaType()).isEqualTo("text/plain");
        assertThat(inspected.contentHash()).hasSize(64);
    }

    @Test
    void rejectsFakePdfAndBinaryDisguisedAsTxt() {
        var fakePdf = new MockMultipartFile(
            "file", "fake.pdf", "application/pdf",
            "Not really a PDF".getBytes(StandardCharsets.UTF_8)
        );
        assertThatThrownBy(() -> inspector.inspect(fakePdf))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.UNSUPPORTED_FILE_TYPE);

        var binaryAsTxt = new MockMultipartFile(
            "file", "malware.txt", "text/plain",
            new byte[] {0x7F, 'E', 'L', 'F', 0x00, 0x02, 0x01}
        );
        assertThatThrownBy(() -> inspector.inspect(binaryAsTxt))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    void sanitizesCrossPlatformPathsAndSpecialCharactersWithoutThrowing() {
        assertThat(inspector.sanitizeOriginalName("C:\\fakepath\\bai:1?*.txt")).isEqualTo("bai:1?*.txt");
        assertThat(inspector.sanitizeOriginalName("../../etc/passwd.txt")).isEqualTo("passwd.txt");
        assertThat(inspector.sanitizeOriginalName("   ")).isEqualTo("document");
        String veryLong = "a".repeat(300) + ".pdf";
        String truncated = inspector.sanitizeOriginalName(veryLong);
        assertThat(truncated).hasSize(255).endsWith(".pdf");
    }

    @Test
    void rejectsEmptyAndOversizedFiles() {
        var empty = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);
        assertThatThrownBy(() -> inspector.inspect(empty))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.VALIDATION_ERROR);

        byte[] oversized = new byte[(int) LessonDocumentInspector.MAX_DOCUMENT_BYTES + 1];
        var tooLarge = new MockMultipartFile("file", "big.txt", "text/plain", oversized);
        assertThatThrownBy(() -> inspector.inspect(tooLarge))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.FILE_TOO_LARGE);
    }
}
