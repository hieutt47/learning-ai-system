package vn.edu.learningai.service;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;

@Service
public class TikaDocumentTextExtractor implements DocumentTextExtractor {
    private final Tika tika = new Tika();

    @Override
    public String extract(MultipartFile file) {
        try {
            String text = tika.parseToString(file.getInputStream());
            return text == null || text.isBlank() ? null : text.trim();
        } catch (Exception exception) {
            throw new ApiException(ErrorCode.UNSUPPORTED_FILE_TYPE, "Could not extract text from document");
        }
    }
}
