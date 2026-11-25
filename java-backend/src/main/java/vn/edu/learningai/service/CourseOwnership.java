package vn.edu.learningai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.learningai.entity.Course;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.repository.CourseRepository;
import vn.edu.learningai.security.AuthenticatedUser;

import java.util.Optional;

/**
 * Quyền trên một khóa học.
 *
 * ADMIN chỉ được xem và gỡ xuất bản; các thao tác ghi (tạo, sửa, xóa, lưu trữ,
 * thumbnail, bài học) chỉ thuộc giảng viên sở hữu khóa.
 */
@Component
@RequiredArgsConstructor
public class CourseOwnership {
    private final CourseRepository courseRepository;

    /** Giảng viên sở hữu khóa hoặc ADMIN — dùng cho API đọc. */
    public Course requireViewable(Long courseId, AuthenticatedUser actor) {
        Course course = find(courseId);
        if (!actor.isAdmin() && !isOwner(course, actor)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Not the course owner");
        }
        return course;
    }

    /** Chỉ giảng viên sở hữu khóa — dùng cho mọi thao tác ghi. */
    public Course requireTeacherOwner(Long courseId, AuthenticatedUser actor) {
        Course course = find(courseId);
        if (!isOwner(course, actor)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Not the course owner");
        }
        return course;
    }

    /** Chỉ giảng viên sở hữu khóa — dùng cho thao tác ghi cần lock PESSIMISTIC_WRITE. */
    public Course requireTeacherOwnerForUpdate(Long courseId, AuthenticatedUser actor) {
        Course course = findForUpdate(courseId);
        if (!isOwner(course, actor)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Not the course owner");
        }
        return course;
    }

    /** Giảng viên sở hữu khóa hoặc ADMIN — chỉ dùng cho thao tác gỡ xuất bản. */
    public Course requireCanUnpublish(Long courseId, AuthenticatedUser actor) {
        return requireViewable(courseId, actor);
    }

    /** Giảng viên sở hữu khóa hoặc ADMIN — dùng cho thao tác gỡ xuất bản có lock. */
    public Course requireCanUnpublishForUpdate(Long courseId, AuthenticatedUser actor) {
        Course course = findForUpdate(courseId);
        if (!actor.isAdmin() && !isOwner(course, actor)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Not the course owner");
        }
        return course;
    }

    public Course requireOwned(Long courseId, AuthenticatedUser actor) {
        return requireOwned(courseRepository.findById(courseId), actor);
    }

    public Course requireOwnedForUpdate(Long courseId, AuthenticatedUser actor) {
        return requireOwned(courseRepository.findByIdForUpdate(courseId), actor);
    }

    private Course requireOwned(Optional<Course> found, AuthenticatedUser actor) {
        Course course = found
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Course not found"));
        if (!actor.isAdmin() && !course.getTeacher().getId().equals(actor.userId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Not the course owner");
        }
        return course;
    }

    private Course find(Long courseId) {
        return courseRepository.findById(courseId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Course not found"));
    }

    private Course findForUpdate(Long courseId) {
        return courseRepository.findByIdForUpdate(courseId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Course not found"));
    }

    private boolean isOwner(Course course, AuthenticatedUser actor) {
        return course.getTeacher().getId().equals(actor.userId());
    }
}
