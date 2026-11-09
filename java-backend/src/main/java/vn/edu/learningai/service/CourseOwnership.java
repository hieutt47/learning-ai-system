package vn.edu.learningai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.learningai.entity.Course;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.repository.CourseRepository;
import vn.edu.learningai.security.AuthenticatedUser;

@Component
@RequiredArgsConstructor
public class CourseOwnership {
    private final CourseRepository courseRepository;
    public Course requireOwned(Long courseId, AuthenticatedUser actor) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Course not found"));
        if (!actor.isAdmin() && !course.getTeacher().getId().equals(actor.userId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Not the course owner");
        }
        return course;
    }
}
