package vn.edu.learningai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.learningai.dto.request.CoursePatchRequest;
import vn.edu.learningai.dto.request.CourseWriteRequest;
import vn.edu.learningai.dto.response.CourseOwnerResponse;
import vn.edu.learningai.dto.response.PageResponse;
import vn.edu.learningai.entity.Category;
import vn.edu.learningai.entity.Course;
import vn.edu.learningai.entity.User;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.mapper.CourseMapper;
import vn.edu.learningai.repository.CategoryRepository;
import vn.edu.learningai.repository.CourseMemberRepository;
import vn.edu.learningai.repository.CourseRepository;
import vn.edu.learningai.repository.UserRepository;
import vn.edu.learningai.security.AuthenticatedUser;
import vn.edu.learningai.storage.StoredFile;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final CourseMemberRepository courseMemberRepository;
    private final UserRepository userRepository;
    private final CourseMapper courseMapper;
    private final CourseOwnerAssembler courseOwnerAssembler;
    private final ThumbnailService thumbnailService;
    private final CourseOwnership courseOwnership;

    @Transactional
    public CourseOwnerResponse create(CourseWriteRequest request, AuthenticatedUser actor) {
        requireTeacherOrAdmin(actor);

        User teacher = userRepository.findById(actor.userId())
            .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED, "User not found"));
        Category category = requireCategory(request.getCategoryId());

        Course course = Course.builder()
            .teacher(teacher)
            .category(category)
            .title(request.getTitle())
            .description(request.getDescription())
            .price(request.getPrice())
            .isPublished(false)
            .build();

        return courseOwnerAssembler.toResponse(courseRepository.save(course));
    }

    @Transactional
    public CourseOwnerResponse update(Long id, CoursePatchRequest request, AuthenticatedUser actor) {
        Course course = getOwnedCourse(id, actor);

        if (request.getTitle() != null) {
            course.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            course.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            course.setPrice(request.getPrice());
        }
        if (request.getCategoryId() != null) {
            course.setCategory(requireCategory(request.getCategoryId()));
        }

        return courseOwnerAssembler.toResponse(courseRepository.save(course));
    }

    @Transactional(readOnly = true)
    public PageResponse<CourseOwnerResponse> listForTeaching(AuthenticatedUser actor, int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "page must be >= 1 and page_size must be between 1 and 100");
        }

        Pageable pageable = PageRequest.of(page - 1, pageSize, defaultSort());
        Page<Course> courses = actor.isAdmin()
            ? courseRepository.findAll(pageable)
            : courseRepository.findByTeacherId(actor.userId(), pageable);

        return new PageResponse<>(
            courseOwnerAssembler.toResponses(courses.getContent()), page, pageSize, courses.getTotalElements());
    }

    @Transactional(readOnly = true)
    public CourseOwnerResponse getOwned(Long id, AuthenticatedUser actor) {
        return courseOwnerAssembler.toResponse(getOwnedCourse(id, actor));
    }

    /**
     * Replaces the course thumbnail. The new file is written before the row is
     * updated and the old file is removed only after the transaction commits, so
     * a rollback leaves the previous thumbnail intact instead of dangling.
     */
    @Transactional
    public CourseOwnerResponse updateThumbnail(Long id, MultipartFile file, AuthenticatedUser actor) {
        Course course = getOwnedCourse(id, actor);
        StoredFile stored = thumbnailService.store(file);

        String previousKey = course.getThumbnailStorageKey();
        course.setThumbnailStorageKey(stored.storageKey());
        course.setThumbnailUrl(stored.url());
        CourseOwnerResponse response =
            courseOwnerAssembler.toResponse(courseRepository.saveAndFlush(course));

        thumbnailService.deleteAfterCommit(previousKey);
        return response;
    }

    @Transactional
    public void delete(Long id, AuthenticatedUser actor) {
        Course course = getOwnedCourse(id, actor);

        if (Boolean.TRUE.equals(course.getIsPublished()) || course.getArchivedAt() != null) {
            throw new ApiException(ErrorCode.INVALID_STATE, "Only draft courses can be deleted");
        }
        if (courseMemberRepository.existsById_CourseId(id)) {
            throw new ApiException(ErrorCode.INVALID_STATE, "Course already has enrolled students");
        }
        // TODO(Nguoi 3): also reject when the course has lessons once LESSON-01 exists.

        courseRepository.delete(course);
        // The row is gone, so nothing can ever resolve this file again.
        thumbnailService.deleteAfterCommit(course.getThumbnailStorageKey());
    }

    private Course getOwnedCourse(Long id, AuthenticatedUser actor) {
        return courseOwnership.requireOwned(id, actor);
    }

    private Category requireCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Category not found"));
    }

    private void requireTeacherOrAdmin(AuthenticatedUser actor) {
        if (!actor.isTeacherOrAdmin()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Only teachers can manage courses");
        }
    }

    private Sort defaultSort() {
        return Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
    }
}
