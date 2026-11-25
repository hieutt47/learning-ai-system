package vn.edu.learningai.service.lesson;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DelegatingDocumentTextExtractorTest {

    @Test
    void plainTextStrategyReadsUtf8StripsBomAndReturnsNullForBlank() {
        var extractor = new PlainTextDocumentExtractor();
        assertThat(extractor.supports("text/plain")).isTrue();
        assertThat(extractor.supports("application/pdf")).isFalse();

        var withBom = new MockMultipartFile(
            "file", "notes.txt", "text/plain",
            "\uFEFF  Lập trình hướng đối tượng Java  \n".getBytes(StandardCharsets.UTF_8)
        );
        assertThat(extractor.extract(withBom)).isEqualTo("Lập trình hướng đối tượng Java");

        var blank = new MockMultipartFile(
            "file", "blank.txt", "text/plain",
            "   \r\n\t  ".getBytes(StandardCharsets.UTF_8)
        );
        assertThat(extractor.extract(blank)).isNull();
    }

    @Test
    void delegatingExtractorDispatchesByMediaTypeWithoutCallingOtherStrategies() {
        AtomicInteger binaryCalls = new AtomicInteger();
        DocumentTextExtractorStrategy binarySpy = new DocumentTextExtractorStrategy() {
            @Override
            public boolean supports(String mediaType) {
                return "application/pdf".equals(mediaType);
            }

            @Override
            public String extract(MultipartFile file) {
                binaryCalls.incrementAndGet();
                return "PDF text";
            }
        };

        var delegating = new DelegatingDocumentTextExtractor(List.of(new PlainTextDocumentExtractor(), binarySpy));

        var txt = new MockMultipartFile("file", "a.txt", "text/plain", "Xin chào".getBytes(StandardCharsets.UTF_8));
        assertThat(delegating.extract(txt, "text/plain")).isEqualTo("Xin chào");
        assertThat(binaryCalls.get()).isZero();

        var pdf = new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[] {1, 2, 3});
        assertThat(delegating.extract(pdf, "application/pdf")).isEqualTo("PDF text");
        assertThat(binaryCalls.get()).isEqualTo(1);

        assertThatThrownBy(() -> delegating.extract(txt, "application/octet-stream"))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.UNSUPPORTED_FILE_TYPE);
    }
}
