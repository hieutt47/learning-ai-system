package vn.edu.learningai.service;

public interface LessonAccessPolicy {
    void assertCanManageLesson(Long lessonId, Long userId);

    LessonViewAccess assertCanViewLesson(Long lessonId, Long userId);
    enum LessonViewAccess {
        MANAGER,
        ENROLLED_STUDENT
    }
}
