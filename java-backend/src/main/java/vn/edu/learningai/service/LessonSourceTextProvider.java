package vn.edu.learningai.service;

import vn.edu.learningai.common.enums.SourceType;

public interface LessonSourceTextProvider {
    String getSourceText(Long lessonId, SourceType type, Long documentId);
}
