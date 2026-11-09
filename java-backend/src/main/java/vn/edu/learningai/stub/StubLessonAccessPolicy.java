package vn.edu.learningai.stub;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.service.LessonAccessPolicy;

import java.util.List;
import java.util.Map;

/**
 * TODO(stub): thay bằng LessonService.assertCanManageLesson/assertCanViewLesson của phần bài học (người 3).
 * Tạm truy vấn thẳng lessons/courses/course_members/users để 404/403 hoạt động đúng như thật.
 */
@Component
@RequiredArgsConstructor
public class StubLessonAccessPolicy implements LessonAccessPolicy {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void assertCanManageLesson(Long lessonId, Long userId) {
        if (!isManager(courseOf(lessonId), userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Only the course owner or an admin can manage this lesson");
        }
    }

    @Override
    public LessonViewAccess assertCanViewLesson(Long lessonId, Long userId) {
        Map<String, Object> course = courseOf(lessonId);
        if (isManager(course, userId)) {
            return LessonViewAccess.MANAGER;
        }
        Integer enrolled = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM course_members WHERE course_id = ? AND student_id = ?",
            Integer.class, course.get("course_id"), userId);
        if (enrolled == null || enrolled == 0) {
            throw new ApiException(ErrorCode.ENROLLMENT_REQUIRED);
        }
        return LessonViewAccess.ENROLLED_STUDENT;
    }

    private Map<String, Object> courseOf(Long lessonId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT c.id AS course_id, c.teacher_id FROM lessons l JOIN courses c ON c.id = l.course_id WHERE l.id = ?",
            lessonId);
        if (rows.isEmpty()) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Lesson not found");
        }
        return rows.get(0);
    }

    private boolean isManager(Map<String, Object> course, Long userId) {
        if (userId.equals(((Number) course.get("teacher_id")).longValue())) {
            return true;
        }
        List<String> role = jdbcTemplate.queryForList("SELECT role FROM users WHERE id = ?", String.class, userId);
        return !role.isEmpty() && "ADMIN".equals(role.get(0));
    }
}
