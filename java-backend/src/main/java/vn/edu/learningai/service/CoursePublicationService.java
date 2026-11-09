package vn.edu.learningai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.edu.learningai.dto.response.CourseOwnerResponse;
import vn.edu.learningai.entity.Course;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.repository.CourseRepository;
import vn.edu.learningai.repository.LessonRepository;
import vn.edu.learningai.security.AuthenticatedUser;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class CoursePublicationService {
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final CourseOwnerAssembler courseOwnerAssembler;
    private final CourseOwnership courseOwnership;
    @Transactional
    public CourseOwnerResponse setPublished(Long courseId, boolean publish, AuthenticatedUser actor) {
        Course course = courseOwnership.requireOwned(courseId, actor);
        if (publish) {
            if (course.getArchivedAt() != null) {
                throw new ApiException(ErrorCode.INVALID_STATE, "Archived courses cannot be published");
            }
            if (Boolean.TRUE.equals(course.getIsPublished())) {
                return courseOwnerAssembler.toResponse(course);
            }
            requirePublishable(course);
            course.setIsPublished(true);
        } else {
            course.setIsPublished(false);
        }
        return courseOwnerAssembler.toResponse(courseRepository.save(course));
    }
    @Transactional
    public CourseOwnerResponse archive(Long courseId, AuthenticatedUser actor) {
        Course course = courseOwnership.requireOwned(courseId, actor);
        if (course.getArchivedAt() != null) {
            return courseOwnerAssembler.toResponse(course);
        }

        course.setArchivedAt(Instant.now().truncatedTo(ChronoUnit.MICROS));
        course.setIsPublished(false);
        return courseOwnerAssembler.toResponse(courseRepository.save(course));
    }
    private void requirePublishable(Course course) {
        if (!StringUtils.hasText(course.getTitle())) {
            throw notPublishable("title is required");
        }
        if (!StringUtils.hasText(course.getDescription())) {
            throw notPublishable("description is required");
        }
        if (course.getCategory() == null) {
            throw notPublishable("category is required");
        }
        if (!StringUtils.hasText(course.getThumbnailStorageKey())) {
            throw notPublishable("thumbnail is required");
        }
        if (course.getPrice() != null && course.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            throw new ApiException(ErrorCode.COURSE_NOT_FREE,
                "Only free courses can be published in this version");
        }
        if (lessonRepository.countByCourseId(course.getId()) < 1) {
            throw notPublishable("at least one lesson is required");
        }
    }
    private ApiException notPublishable(String reason) {
        return new ApiException(ErrorCode.INVALID_STATE, "Course cannot be published: " + reason);
    }
}
