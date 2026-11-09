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

    public EnrollmentResult enroll(Long courseId, AuthenticatedUser actor) {
        if (actor.role() != UserRole.STUDENT) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Only students can enroll in courses");
        }
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Course not found"));
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
            return new EnrollmentResult(toResponse(courseMemberRepository.findById(id).orElseThrow()), true);
        } catch (DataIntegrityViolationException e) {

            log.debug("Concurrent enrollment for course {} student {}", courseId, actor.userId());
            CourseMember winner = courseMemberRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_STATE,
                    "Enrollment conflicted and could not be read back"));
            return new EnrollmentResult(toResponse(winner), false);
        }
    }
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
    public record EnrollmentResult(EnrollmentResponse response, boolean created) {
    }
}
