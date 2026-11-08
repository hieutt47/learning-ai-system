package vn.edu.learningai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.learningai.entity.Course;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.repository.CourseRepository;
import vn.edu.learningai.security.AuthenticatedUser;

/**
 * Object-level ownership check shared by every route that takes a course id.
 *
 * <p>Extracted so enrollment, membership listing and the existing course CRUD
 * cannot drift: each of them needs the same "404 if missing, 403 if not mine"
 * pair, and duplicating it is how one route ends up leaking another's course.</p>
 */
@Component
@RequiredArgsConstructor
public class CourseOwnership {
    private final CourseRepository courseRepository;

    /**
     * @throws ApiException 404 when the course does not exist, 403 when the
     *                      caller is neither its teacher nor an admin
     */
    public Course requireOwned(Long courseId, AuthenticatedUser actor) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Course not found"));
        if (!actor.isAdmin() && !course.getTeacher().getId().equals(actor.userId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Not the course owner");
        }
        return course;
    }
}
