package vn.edu.learningai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.learningai.dto.response.CourseOwnerResponse;
import vn.edu.learningai.entity.Course;
import vn.edu.learningai.mapper.CourseMapper;
import vn.edu.learningai.repository.EnrollmentQueryRepository;
import vn.edu.learningai.repository.LessonRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds {@link CourseOwnerResponse}, including the lesson and student counts.
 *
 * <p>Single and page builds take different paths on purpose: one page of results
 * needs one grouped query for the counts instead of one query per course, which
 * is the difference between three statements and twenty for ten courses.</p>
 */
@Component
@RequiredArgsConstructor
public class CourseOwnerAssembler {
    private final CourseMapper courseMapper;
    private final LessonRepository lessonRepository;
    private final EnrollmentQueryRepository enrollmentQueryRepository;

    public CourseOwnerResponse toResponse(Course course) {
        CourseOwnerResponse response = courseMapper.toCourseOwnerResponse(course);
        response.setLessonCount(lessonRepository.countByCourseId(course.getId()));
        response.setTotalStudents(enrollmentQueryRepository.countEnrolledStudents(course.getId()));
        return response;
    }

    /** One count query per table for the whole page, not per course. */
    public List<CourseOwnerResponse> toResponses(List<Course> courses) {
        if (courses.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> lessons = new HashMap<>();
        Map<Long, Long> students = new HashMap<>();
        lessonRepository.countByCourseIds(courses.stream().map(Course::getId).toList())
            .forEach(row -> lessons.put(row.courseId(), row.total()));
        enrollmentQueryRepository.countEnrolledStudents(
                courses.stream().map(Course::getId).toList())
            .forEach((courseId, total) -> students.put(courseId, total));

        return courses.stream().map(course -> {
            CourseOwnerResponse response = courseMapper.toCourseOwnerResponse(course);
            response.setLessonCount(lessons.getOrDefault(course.getId(), 0L));
            response.setTotalStudents(students.getOrDefault(course.getId(), 0L));
            return response;
        }).toList();
    }
}
