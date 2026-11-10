package vn.edu.learningai.service;

/**
 * Kiểm tra quyền theo bài học (lesson → course → owner/admin hoặc student đã ghi danh).
 * LessonService cài đặt chính sách này cho API bài học, AI và flashcard.
 */
public interface LessonAccessPolicy {

    /** Owner của khóa học chứa bài học hoặc ADMIN. Lesson không tồn tại → NOT_FOUND, không có quyền → FORBIDDEN. */
    void assertCanManageLesson(Long lessonId, Long userId);

    /**
     * Owner/admin hoặc STUDENT đã ghi danh khóa học. Trả về loại quyền để service lọc nội dung
     * (student chỉ thấy Published). Không ghi danh → ENROLLMENT_REQUIRED.
     */
    LessonViewAccess assertCanViewLesson(Long lessonId, Long userId);
    enum LessonViewAccess {
        MANAGER,
        ENROLLED_STUDENT
    }
}
