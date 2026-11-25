package vn.edu.learningai.service.lesson;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.learningai.common.enums.LessonStatus;
import vn.edu.learningai.dto.response.LessonDocumentResponse;
import vn.edu.learningai.dto.response.LessonResponse;
import vn.edu.learningai.entity.Course;
import vn.edu.learningai.entity.Lesson;
import vn.edu.learningai.entity.LessonDocument;
import vn.edu.learningai.repository.CourseRepository;
import vn.edu.learningai.repository.LessonCountRow;
import vn.edu.learningai.repository.LessonDocumentRepository;
import vn.edu.learningai.repository.LessonRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Lắp ráp LessonResponse / LessonDocumentResponse theo đúng phạm vi hiển thị:
 * - Học viên: ẩn transcript, chỉ đếm quiz/flashcard PUBLISHED, không lộ trạng thái xóa nội bộ.
 * - Quản lý (Teacher sở hữu / Admin): hiển thị transcript, kiểm tra đủ điều kiện khóa/bài lưu trữ.
 * - Truy vấn danh sách dùng GROUP BY theo lô (O(1) query count) thay vì N+1.
 */
@Component
@RequiredArgsConstructor
public class LessonResponseAssembler {
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final LessonDocumentRepository documentRepository;

    public LessonResponse toResponse(Lesson lesson, boolean managerView, boolean canManageCourse) {
        Course course = managerView ? courseRepository.findById(lesson.getCourseId()).orElse(null) : null;
        long publishedCount = managerView && course != null && Boolean.TRUE.equals(course.getIsPublished())
            ? lessonRepository.countByCourseIdAndStatus(lesson.getCourseId(), LessonStatus.PUBLISHED)
            : 0L;
        long documentCount = documentRepository.countByLessonId(lesson.getId());
        if (!managerView) {
            return buildStudentResponse(
                lesson,
                documentCount,
                lessonRepository.countPublishedQuizzes(lesson.getId()),
                lessonRepository.countPublishedFlashcards(lesson.getId())
            );
        }
        return buildManagerResponse(
            lesson,
            canManageCourse,
            course,
            publishedCount,
            documentCount,
            lessonRepository.countQuizzes(lesson.getId()),
            lessonRepository.countFlashcards(lesson.getId()),
            lessonRepository.countQuizAttempts(lesson.getId())
        );
    }

    public List<LessonResponse> toResponses(
        Long courseId,
        List<Lesson> lessons,
        boolean managerView,
        boolean canManageCourse
    ) {
        if (lessons.isEmpty()) {
            return List.of();
        }
        List<Long> lessonIds = lessons.stream().map(Lesson::getId).toList();
        Map<Long, Long> documentCounts = toCountMap(documentRepository.countByLessonIds(lessonIds));

        if (!managerView) {
            Map<Long, Long> publishedQuizCounts = toCountMap(lessonRepository.countPublishedQuizzesByLessonIds(lessonIds));
            Map<Long, Long> publishedFlashcardCounts =
                toCountMap(lessonRepository.countPublishedFlashcardsByLessonIds(lessonIds));
            return lessons.stream()
                .map(lesson -> buildStudentResponse(
                    lesson,
                    documentCounts.getOrDefault(lesson.getId(), 0L),
                    publishedQuizCounts.getOrDefault(lesson.getId(), 0L),
                    publishedFlashcardCounts.getOrDefault(lesson.getId(), 0L)
                ))
                .toList();
        }

        Course course = courseRepository.findById(courseId).orElse(null);
        long publishedCount = course != null && Boolean.TRUE.equals(course.getIsPublished())
            ? lessons.stream().filter(lesson -> lesson.getStatus() == LessonStatus.PUBLISHED).count()
            : 0L;
        Map<Long, Long> quizCounts = toCountMap(lessonRepository.countQuizzesByLessonIds(lessonIds));
        Map<Long, Long> flashcardCounts = toCountMap(lessonRepository.countFlashcardsByLessonIds(lessonIds));
        Map<Long, Long> attemptCounts = toCountMap(lessonRepository.countQuizAttemptsByLessonIds(lessonIds));

        return lessons.stream()
            .map(lesson -> buildManagerResponse(
                lesson,
                canManageCourse,
                course,
                publishedCount,
                documentCounts.getOrDefault(lesson.getId(), 0L),
                quizCounts.getOrDefault(lesson.getId(), 0L),
                flashcardCounts.getOrDefault(lesson.getId(), 0L),
                attemptCounts.getOrDefault(lesson.getId(), 0L)
            ))
            .toList();
    }

    public LessonDocumentResponse toDocumentResponse(LessonDocument document) {
        return new LessonDocumentResponse(
            document.getId(),
            document.getLessonId(),
            document.getOriginalName(),
            document.getMediaType(),
            document.getSizeBytes(),
            document.getExtractedText() != null,
            document.getCreatedAt()
        );
    }

    private LessonResponse buildStudentResponse(
        Lesson lesson,
        long documentCount,
        long publishedQuizCount,
        long publishedFlashcardCount
    ) {
        return new LessonResponse(
            lesson.getId(),
            lesson.getCourseId(),
            lesson.getTitle(),
            lesson.getStatus(),
            lesson.getVideoUrl(),
            null,
            lesson.getOrderIndex(),
            documentCount,
            publishedQuizCount,
            publishedFlashcardCount,
            false,
            null,
            lesson.getCreatedAt(),
            lesson.getUpdatedAt()
        );
    }

    private LessonResponse buildManagerResponse(
        Lesson lesson,
        boolean canManageCourse,
        Course course,
        long publishedLessonCount,
        long documentCount,
        long quizCount,
        long flashcardCount,
        long attemptCount
    ) {
        boolean courseArchived = course != null && course.getArchivedAt() != null;
        boolean lessonArchived = lesson.getStatus() == LessonStatus.ARCHIVED;
        boolean hasHistory = attemptCount > 0;
        boolean lastPublished = lesson.getStatus() == LessonStatus.PUBLISHED
            && course != null
            && Boolean.TRUE.equals(course.getIsPublished())
            && publishedLessonCount <= 1;

        boolean blocked = !canManageCourse || courseArchived || lessonArchived
            || hasHistory || quizCount > 0 || flashcardCount > 0 || lastPublished;
        String reason = !canManageCourse ? "Chỉ giảng viên sở hữu khóa mới có thể xóa bài học."
            : courseArchived ? "Khóa học đã lưu trữ, chỉ có thể xem."
            : lessonArchived ? "Bài học đã lưu trữ, chỉ có thể xem."
            : hasHistory ? "Bài học đã có lịch sử làm quiz."
            : quizCount > 0 || flashcardCount > 0 ? "Xóa quiz và flashcard trước khi xóa bài học."
            : lastPublished ? "Khóa đang xuất bản cần ít nhất một bài học hiển thị."
            : null;

        return new LessonResponse(
            lesson.getId(),
            lesson.getCourseId(),
            lesson.getTitle(),
            lesson.getStatus(),
            lesson.getVideoUrl(),
            lesson.getTranscriptText(),
            lesson.getOrderIndex(),
            documentCount,
            quizCount,
            flashcardCount,
            !blocked,
            reason,
            lesson.getCreatedAt(),
            lesson.getUpdatedAt()
        );
    }

    private static Map<Long, Long> toCountMap(List<LessonCountRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }
        return rows.stream().collect(Collectors.toMap(
            LessonCountRow::getLessonId,
            row -> row.getTotal() == null ? 0L : row.getTotal(),
            Long::sum
        ));
    }
}
