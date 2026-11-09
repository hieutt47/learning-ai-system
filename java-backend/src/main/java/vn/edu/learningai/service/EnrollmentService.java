package vn.edu.learningai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.learningai.common.enums.UserRole;
import vn.edu.learningai.dto.response.CourseEnrollmentResponse;
import vn.edu.learningai.dto.response.CoursePublicResponse;
import vn.edu.learningai.dto.response.EnrollmentResponse;
import vn.edu.learningai.dto.response.MemberResponse;
import vn.edu.learningai.dto.response.PageResponse;
import vn.edu.learningai.entity.Course;
import vn.edu.learningai.entity.CourseMember;
import vn.edu.learningai.entity.CourseMemberId;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.repository.CourseMemberRepository;
import vn.edu.learningai.repository.CourseRepository;
import vn.edu.learningai.repository.EnrollmentQueryRepository;
import vn.edu.learningai.repository.MarketplaceQueryRepository;
import vn.edu.learningai.repository.LessonRepository;
import vn.edu.learningai.repository.MemberRow;
import vn.edu.learningai.security.AuthenticatedUser;

import java.math.BigDecimal;
import java.util.List;

/**
 * ENROLL-01: a student joins a free, published course; both sides can then list
 * the resulting memberships.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentService {
    private static final String CURRENCY = "VND";

    private final CourseRepository courseRepository;
    private final CourseMemberRepository courseMemberRepository;
    private final EnrollmentWriter enrollmentWriter;
    private final EnrollmentQueryRepository enrollmentQueryRepository;
    private final MarketplaceQueryRepository marketplaceQueryRepository;
    private final CourseOwnership courseOwnership;
    private final LessonRepository lessonRepository;

    /**
     * Enrolls the caller, or returns the enrollment they already have.
     *
     * <p>Idempotent by contract: a retry answers 200 with the original
     * membership instead of 409, so a flaky network cannot block a student.
     * The race between two concurrent first requests is resolved by the
     * composite primary key: the loser's insert violates it, and the catch
     * below re-reads the winner's row rather than surfacing a 500.</p>
     *
     * @return the membership plus whether it was created by this call, which
     *         the controller turns into 201 or 200
     */
    // Not annotated: the read runs in its own transaction and the insert in
    // EnrollmentWriter's, so a duplicate key cannot poison this call's commit.
    public EnrollmentResult enroll(Long courseId, AuthenticatedUser actor) {
        if (actor.role() != UserRole.STUDENT) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Only students can enroll in courses");
        }

        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Course not found"));

        // A draft or archived course is 404, not 409: the caller is not allowed
        // to know it exists. A published but priced course is 409, because the
        // student can see it and needs a reason why enrolling failed.
        if (!Boolean.TRUE.equals(course.getIsPublished()) || course.getArchivedAt() != null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Course not found");
        }
        if (course.getPrice() != null && course.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            throw new ApiException(ErrorCode.COURSE_NOT_FREE, "Only free courses can be enrolled");
        }

        CourseMemberId id = new CourseMemberId(courseId, actor.userId());
        var existing = courseMemberRepository.findById(id);
        if (existing.isPresent()) {
            return new EnrollmentResult(toResponse(existing.get()), false);
        }

        try {
            enrollmentWriter.create(id);
            // Read back rather than echo the in-memory entity: MySQL keeps
            // microseconds, the entity carries nanoseconds, and two responses
            // for one row must agree on enrolled_at.
            return new EnrollmentResult(toResponse(courseMemberRepository.findById(id).orElseThrow()), true);
        } catch (DataIntegrityViolationException e) {
            // A concurrent request for the same pair won. Its row is the truth.
            log.debug("Concurrent enrollment for course {} student {}", courseId, actor.userId());
            CourseMember winner = courseMemberRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_STATE,
                    "Enrollment conflicted and could not be read back"));
            return new EnrollmentResult(toResponse(winner), false);
        }
    }

    /**
     * The student's courses, including any that were unpublished or archived
     * after they joined (T33).
     */
    @Transactional(readOnly = true)
    public PageResponse<CourseEnrollmentResponse> listMine(AuthenticatedUser actor, int page, int pageSize) {
        requireValidPagination(page, pageSize);

        Page<EnrollmentQueryRepository.EnrolledCourse> pageResult =
            enrollmentQueryRepository.findEnrolledCourses(actor.userId(), PageRequest.of(page - 1, pageSize));

        List<CourseEnrollmentResponse> items = pageResult.getContent().stream()
            .map(row -> new CourseEnrollmentResponse(toPublicResponse(row.course()), row.enrolledAt()))
            .toList();

        return new PageResponse<>(items, page, pageSize, pageResult.getTotalElements());
    }

    /**
     * Members of a course, for its owner only. Names and join dates, no email.
     */
    @Transactional(readOnly = true)
    public PageResponse<MemberResponse> listMembers(Long courseId, AuthenticatedUser actor, int page, int pageSize) {
        requireValidPagination(page, pageSize);
        Course course = courseOwnership.requireOwned(courseId, actor);

        Page<MemberRow> pageResult =
            enrollmentQueryRepository.findMembers(course.getId(), PageRequest.of(page - 1, pageSize));

        List<MemberResponse> items = pageResult.getContent().stream()
            .map(row -> new MemberResponse(course.getId(), row.studentId(), row.fullName(), row.enrolledAt()))
            .toList();

        return new PageResponse<>(items, page, pageSize, pageResult.getTotalElements());
    }

    private CoursePublicResponse toPublicResponse(Course course) {
        return CoursePublicResponse.builder()
            .id(course.getId())
            .title(course.getTitle())
            .description(course.getDescription())
            .category(new CoursePublicResponse.Category(
                course.getCategory().getId(), course.getCategory().getName()))
            .teacher(new CoursePublicResponse.Teacher(
                course.getTeacher().getId(), course.getTeacher().getFullName()))
            .thumbnailUrl(course.getThumbnailUrl())
            .price(course.getPrice())
            .currency(CURRENCY)
            .totalStudents(marketplaceQueryRepository.countEnrollments(course.getId()))
            .lessonCount((int) lessonRepository.countByCourseId(course.getId()))
            // Only "my courses" needs this: it explains why a course the student
            // joined is no longer in the marketplace.
            .archivedAt(course.getArchivedAt())
            .build();
    }

    private EnrollmentResponse toResponse(CourseMember member) {
        return new EnrollmentResponse(
            member.getId().getCourseId(), member.getId().getStudentId(), member.getEnrolledAt());
    }

    private void requireValidPagination(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                "page must be >= 1 and page_size must be between 1 and 100");
        }
    }

    /** The membership plus whether this call created it. */
    public record EnrollmentResult(EnrollmentResponse response, boolean created) {
    }
}
