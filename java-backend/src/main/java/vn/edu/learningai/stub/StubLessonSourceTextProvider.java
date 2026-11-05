package vn.edu.learningai.stub;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import vn.edu.learningai.common.enums.SourceType;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.service.LessonSourceTextProvider;

import java.util.List;

/**
 * TODO(stub): thay bằng cài đặt LessonSourceTextProvider của phần bài học (người 3).
 * Tạm đọc thẳng lessons.transcript_text / lesson_documents.extracted_text theo đúng quy ước đã thống nhất.
 */
@Component
@RequiredArgsConstructor
public class StubLessonSourceTextProvider implements LessonSourceTextProvider {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String getSourceText(Long lessonId, SourceType type, Long documentId) {
        List<String> rows = switch (type) {
            case TRANSCRIPT -> jdbcTemplate.queryForList(
                "SELECT transcript_text FROM lessons WHERE id = ?", String.class, lessonId);
            case DOCUMENT -> jdbcTemplate.queryForList(
                "SELECT extracted_text FROM lesson_documents WHERE id = ? AND lesson_id = ?",
                String.class, documentId, lessonId);
        };
        if (rows.isEmpty()) {
            throw new ApiException(ErrorCode.NOT_FOUND,
                type == SourceType.DOCUMENT ? "Document not found in this lesson" : "Lesson not found");
        }
        String text = rows.get(0);
        if (text == null || text.isBlank()) {
            throw new ApiException(ErrorCode.SOURCE_TEXT_EMPTY);
        }
        return text;
    }
}
