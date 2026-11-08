package vn.edu.learningai.service;

import vn.edu.learningai.common.enums.SourceType;

/**
 * Nguồn văn bản của bài học cho AI — interface do phần bài học (người 3) cài đặt, đã thống nhất 2026-11-04.
 * Trả toàn bộ text đã lưu; phía AI tự cắt theo giới hạn.
 */
public interface LessonSourceTextProvider {

    /**
     * @param type       TRANSCRIPT → lessons.transcript_text; DOCUMENT → lesson_documents.extracted_text
     * @param documentId bắt buộc khi DOCUMENT, document phải thuộc đúng lesson (không thuộc → NOT_FOUND)
     * @throws vn.edu.learningai.exception.ApiException NOT_FOUND hoặc SOURCE_TEXT_EMPTY (422) khi không có text
     */
    String getSourceText(Long lessonId, SourceType type, Long documentId);
}
