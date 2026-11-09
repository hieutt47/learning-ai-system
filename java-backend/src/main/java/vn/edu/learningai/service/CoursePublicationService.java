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

/**
 * COURSE-01 publication and archive.
 *
 * <p>Publishing is the only transition with preconditions, and they are all
 * checked in one place ({@link #requirePublishable}) so a future caller cannot
 * publish through a path that skips a rule.</p>
 */
@Service
@RequiredArgsConstructor
public class CoursePublicationService {
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final CourseOwnerAssembler courseOwnerAssembler;
    private final CourseOwnership courseOwnership;

    /**
     * Publishes or unpublishes a course.
     *
     * <p>Unpublishing has no preconditions: it is always safe to withdraw a
     * course. Archived courses cannot be republished — archive is final in the
     * MVP, and flipping {@code is_published} on an archived row would put it
     * back in the marketplace while {@code archived_at} still says otherwise.</p>
     */
    @Transactional
    public CourseOwnerResponse setPublished(Long courseId, boolean publish, AuthenticatedUser actor) {
        Course course = courseOwnership.requireOwned(courseId, actor);

        if (publish) {
            if (course.getArchivedAt() != null) {
                throw new ApiException(ErrorCode.INVALID_STATE, "Archived courses cannot be published");
            }
            if (Boolean.TRUE.equals(course.getIsPublished())) {
                // Already published: answer with the same row, do not re-check
                // preconditions that may have changed since (e.g. a lesson was
                // deleted). Publication is a state, not an event.
                return courseOwnerAssembler.toResponse(course);
            }
            requirePublishable(course);
            course.setIsPublished(true);
        } else {
            course.setIsPublished(false);
        }

        return courseOwnerAssembler.toResponse(courseRepository.save(course));
    }

    /**
     * Archives a course: removed from the marketplace, enrollments preserved.
     *
     * <p>Archiving also unpublishes, because the two flags are read together to
     * decide visibility and leaving them inconsistent invites bugs. The
     * membership rows are untouched, so enrolled students keep access (T33).</p>
     */
    @Transactional
    public CourseOwnerResponse archive(Long courseId, AuthenticatedUser actor) {
        Course course = courseOwnership.requireOwned(courseId, actor);

        if (course.getArchivedAt() != null) {
            // Idempotent: archiving an archived course answers with the original
            // timestamp instead of moving it.
            return courseOwnerAssembler.toResponse(course);
        }

        // Truncated to microseconds: archived_at is DATETIME(6), so a
        // nanosecond value would change on the first read back and the archive
        // response would disagree with every later one.
        course.setArchivedAt(Instant.now().truncatedTo(ChronoUnit.MICROS));
        course.setIsPublished(false);
        return courseOwnerAssembler.toResponse(courseRepository.save(course));
    }

    /**
     * Every precondition from docs/02-architecture/business-rules.md:
     * title, description, category, thumbnail, at least one lesson and price 0.
     *
     * <p>The field checks are belt and braces — the write path already refuses
     * blank titles and a null category — but they are repeated here because a
     * publish decision must not rely on every earlier write having validated
     * correctly.</p>
     */
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
            // Drafts may be priced; the MVP simply cannot sell, so a priced course
            // must stay unpublished rather than enter a marketplace that cannot
            // enroll anyone in it.
            throw new ApiException(ErrorCode.COURSE_NOT_FREE,
                "Only free courses can be published in this version");
        }
        if (lessonRepository.countByCourseId(course.getId()) < 1) {
            // The count is the rule from LESSON-01: a lesson is valid by existing.
            throw notPublishable("at least one lesson is required");
        }
    }

    private ApiException notPublishable(String reason) {
        return new ApiException(ErrorCode.INVALID_STATE, "Course cannot be published: " + reason);
    }
}
