package vn.edu.learningai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.edu.learningai.dto.response.CoursePublicResponse;
import vn.edu.learningai.dto.response.PageResponse;
import vn.edu.learningai.entity.Course;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.repository.CategoryRepository;
import vn.edu.learningai.repository.CourseRepository;
import vn.edu.learningai.repository.LessonOutlineRow;
import vn.edu.learningai.repository.LessonRepository;
import vn.edu.learningai.repository.MarketplaceQueryRepository;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MarketplaceService {
    private static final String CURRENCY = "VND";
    private static final int MAX_QUERY_LENGTH = 200;
    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final LessonRepository lessonRepository;
    private final MarketplaceQueryRepository marketplaceQueryRepository;
    @Transactional(readOnly = true)
    public PageResponse<CoursePublicResponse> search(
        String query,
        Long categoryId,
        String sort,
        int page,
        int pageSize
    ) {
        requireValidPagination(page, pageSize);
        String normalizedQuery = normalizeQuery(query);
        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Category not found");
        }

        Pageable pageable = PageRequest.of(page - 1, pageSize, resolveSort(sort));
        Page<Course> courses =
            marketplaceQueryRepository.searchPublished(normalizedQuery, categoryId, pageable);
        List<Long> courseIds = courses.getContent().stream().map(Course::getId).toList();
        Map<Long, Long> enrollmentCounts = marketplaceQueryRepository.countEnrollments(courseIds);
        Map<Long, Long> lessonCounts = lessonRepository.countByCourseIds(courseIds).stream()
            .collect(java.util.stream.Collectors.toMap(
                vn.edu.learningai.repository.CourseCountRow::courseId,
                vn.edu.learningai.repository.CourseCountRow::total));
        List<CoursePublicResponse> items = courses.getContent().stream()
            .map(course -> {
                CoursePublicResponse response = toPublicResponse(
                    course, enrollmentCounts.getOrDefault(course.getId(), 0L), null);
                response.setLessonCount(lessonCounts.getOrDefault(course.getId(), 0L).intValue());
                return response;
            })
            .toList();
        return new PageResponse<>(items, page, pageSize, courses.getTotalElements());
    }
    @Transactional(readOnly = true)
    public CoursePublicResponse getPublicCourse(Long id) {
        Course course = courseRepository.findWithCategoryAndTeacherById(id)
            .filter(c -> Boolean.TRUE.equals(c.getIsPublished()) && c.getArchivedAt() == null)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Course not found"));

        List<LessonOutlineRow> outline = marketplaceQueryRepository.findOutline(id);
        return toPublicResponse(course, marketplaceQueryRepository.countEnrollments(id), outline);
    }
    private CoursePublicResponse toPublicResponse(Course course, long totalStudents, List<LessonOutlineRow> outline) {
        CoursePublicResponse response = CoursePublicResponse.builder()
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
            .totalStudents(totalStudents)
            .build();
        if (outline != null) {
            response.setOutline(outline.stream()
                .map(row -> new CoursePublicResponse.OutlineItem(row.id(), row.title(), row.orderIndex()))
                .toList());
        }
        return response;
    }
    private String normalizeQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }
        String trimmed = query.trim();
        if (trimmed.length() > MAX_QUERY_LENGTH) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "q must be at most 200 characters");
        }
        return trimmed;
    }
    private Sort resolveSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
        }

        String[] parts = sort.split(",", 2);
        String property = parts[0].trim();
        boolean ascending = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim());
        return Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, property);
    }
    private void requireValidPagination(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                "page must be >= 1 and page_size must be between 1 and 100");
        }
    }
}
