package vn.edu.learningai.service.lesson;

import org.junit.jupiter.api.Test;
import vn.edu.learningai.common.enums.LessonStatus;
import vn.edu.learningai.common.enums.UserRole;
import vn.edu.learningai.dto.request.LessonReorderRequest;
import vn.edu.learningai.dto.request.LessonWriteRequest;
import vn.edu.learningai.entity.Course;
import vn.edu.learningai.entity.Lesson;
import vn.edu.learningai.entity.User;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.repository.CourseRepository;
import vn.edu.learningai.repository.LessonCountRow;
import vn.edu.learningai.repository.LessonDocumentRepository;
import vn.edu.learningai.repository.LessonRepository;
import vn.edu.learningai.repository.UserRepository;
import vn.edu.learningai.service.DocumentCleanupService;
import vn.edu.learningai.service.DocumentStorage;
import vn.edu.learningai.service.DocumentTextExtractor;
import vn.edu.learningai.service.LessonService;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test thuần Java xác minh các nghiệp vụ trọng yếu và tối ưu truy vấn của Role 3:
 * - Xóa bài học dọn ai_generations mồ côi và compact order 2 pha không đụng UNIQUE.
 * - Không cho đổi thứ tự bài đã ARCHIVED.
 * - Chuẩn hóa YouTube URL và sinh orderIndex theo max + 1.
 * - Phân biệt ENROLLMENT_REQUIRED vs FORBIDDEN cho học viên đã ghi danh vào khóa unpublish/archive.
 * - Student response chỉ đếm PUBLISHED quiz/flashcard và không lộ delete_blocked_reason.
 * - listForCourse dùng truy vấn GROUP BY theo lô (O(1)) thay vì N+1.
 */
class LessonBusinessRulesTest {

    @Test
    void deleteLessonCleansOrphanedAiGenerationsAndCompactsOrdersSafelyInTwoPhases() {
        var harness = new Harness();
        harness.course = Course.builder().id(20L).isPublished(false).build();
        harness.putLesson(Lesson.builder().id(1L).courseId(20L).title("Bai 1").videoUrl("").orderIndex(1).status(LessonStatus.PUBLISHED).build());
        harness.putLesson(Lesson.builder().id(3L).courseId(20L).title("Bai 3").videoUrl("").orderIndex(2).status(LessonStatus.PUBLISHED).build());
        harness.putLesson(Lesson.builder().id(2L).courseId(20L).title("Bai 2").videoUrl("").orderIndex(3).status(LessonStatus.PUBLISHED).build());

        harness.service.delete(1L, 7L);

        assertThat(harness.deletedAiGenerationsFor).containsExactly(1L);
        assertThat(harness.lessons).doesNotContainKey(1L);
        assertThat(harness.lessons.get(3L).getOrderIndex()).isEqualTo(1);
        assertThat(harness.lessons.get(2L).getOrderIndex()).isEqualTo(2);
        assertThat(harness.flushSnapshots).hasSize(3);
        assertThat(harness.flushSnapshots.get(1)).allMatch(order -> order > 1000);
        assertThat(harness.flushSnapshots.get(2)).containsExactly(1, 2);
    }

    @Test
    void createLessonUsesMaxOrderIndexPlusOneAndNormalizesYouTubeUrl() {
        var harness = new Harness();
        harness.course = Course.builder().id(20L).isPublished(false).build();
        harness.putLesson(Lesson.builder().id(1L).courseId(20L).title("Bai 1").videoUrl("").orderIndex(5).status(LessonStatus.PUBLISHED).build());

        var created = harness.service.create(
            20L,
            new LessonWriteRequest("Bai moi", "https://youtu.be/eIrMbAQSU34?si=xyz", "  noi dung  "),
            7L
        );

        assertThat(created.orderIndex()).isEqualTo(6);
        assertThat(created.videoUrl()).isEqualTo("https://www.youtube.com/watch?v=eIrMbAQSU34");
        assertThat(created.transcriptText()).isEqualTo("noi dung");
    }

    @Test
    void reorderRejectsMovingArchivedLesson() {
        var harness = new Harness();
        harness.course = Course.builder().id(20L).isPublished(true).build();
        harness.putLesson(Lesson.builder().id(1L).courseId(20L).title("Cu").videoUrl("").orderIndex(1).status(LessonStatus.ARCHIVED).build());
        harness.putLesson(Lesson.builder().id(2L).courseId(20L).title("Moi").videoUrl("").orderIndex(2).status(LessonStatus.PUBLISHED).build());

        assertThatThrownBy(() -> harness.service.reorder(20L, new LessonReorderRequest(List.of(2L, 1L)), 7L))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_STATE);
    }

    @Test
    void studentViewHidesDraftCountsAndDeleteReasonAndDistinguishesUnpublishedCourse() {
        var harness = new Harness();
        harness.course = Course.builder().id(20L).isPublished(true).build();
        harness.putLesson(Lesson.builder().id(10L).courseId(20L).title("Bai 1").videoUrl("")
            .transcriptText("Bi mat").orderIndex(1).status(LessonStatus.PUBLISHED).build());
        harness.totalQuizzes = 4;
        harness.publishedQuizzes = 1;
        harness.totalFlashcards = 5;
        harness.publishedFlashcards = 2;
        harness.quizAttempts = 3;

        harness.activeEnrolled = true;
        harness.rawEnrolled = true;
        var studentResponse = harness.service.get(10L, 99L);
        assertThat(studentResponse.transcriptText()).isNull();
        assertThat(studentResponse.quizCount()).isEqualTo(1);
        assertThat(studentResponse.flashcardCount()).isEqualTo(2);
        assertThat(studentResponse.canDelete()).isFalse();
        assertThat(studentResponse.deleteBlockedReason()).isNull();

        harness.activeEnrolled = false;
        assertThatThrownBy(() -> harness.service.get(10L, 99L))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.FORBIDDEN);

        harness.rawEnrolled = false;
        assertThatThrownBy(() -> harness.service.get(10L, 99L))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENROLLMENT_REQUIRED);
    }

    @Test
    void archivedCourseAndReadOnlyAdminMarkCanDeleteFalse() {
        var harness = new Harness();
        harness.course = Course.builder().id(20L).isPublished(false).archivedAt(Instant.now()).build();
        harness.putLesson(Lesson.builder().id(10L).courseId(20L).title("Bai 1").videoUrl("").orderIndex(1).status(LessonStatus.PUBLISHED).build());

        var teacherView = harness.service.get(10L, 7L);
        assertThat(teacherView.canDelete()).isFalse();
        assertThat(teacherView.deleteBlockedReason()).contains("Khóa học đã lưu trữ");

        harness.course = Course.builder().id(20L).isPublished(false).build();
        var adminView = harness.service.get(10L, 1L);
        assertThat(adminView.canDelete()).isFalse();
        assertThat(adminView.deleteBlockedReason()).contains("Chỉ giảng viên sở hữu khóa");
    }

    @Test
    void listForCourseUsesBatchGroupByQueriesInsteadOfPerLessonNPlusOne() {
        var harness = new Harness();
        harness.course = Course.builder().id(20L).isPublished(true).build();
        harness.putLesson(Lesson.builder().id(10L).courseId(20L).title("Bai 1").videoUrl("").orderIndex(1).status(LessonStatus.PUBLISHED).build());
        harness.putLesson(Lesson.builder().id(11L).courseId(20L).title("Bai 2").videoUrl("").orderIndex(2).status(LessonStatus.PUBLISHED).build());
        harness.putLesson(Lesson.builder().id(12L).courseId(20L).title("Bai 3").videoUrl("").orderIndex(3).status(LessonStatus.DRAFT).build());
        harness.batchDocCounts.put(10L, 2L);
        harness.batchQuizCounts.put(10L, 3L);
        harness.batchPublishedQuizCounts.put(10L, 1L);
        harness.batchAttemptCounts.put(10L, 5L);

        var managerList = harness.service.listForCourse(20L, 7L);
        assertThat(managerList).hasSize(3);
        assertThat(managerList.get(0).documentCount()).isEqualTo(2);
        assertThat(managerList.get(0).quizCount()).isEqualTo(3);
        assertThat(managerList.get(0).canDelete()).isFalse();
        assertThat(managerList.get(2).canDelete()).isTrue();
        assertThat(harness.perLessonCountCalls).isZero();
        assertThat(harness.batchCountCalls).isEqualTo(4);

        harness.batchCountCalls = 0;
        harness.activeEnrolled = true;
        var studentList = harness.service.listForCourse(20L, 99L);
        assertThat(studentList).hasSize(2);
        assertThat(studentList.get(0).quizCount()).isEqualTo(1);
        assertThat(studentList.get(0).canDelete()).isFalse();
        assertThat(harness.perLessonCountCalls).isZero();
        assertThat(harness.batchCountCalls).isEqualTo(3);
    }

    private static LessonCountRow countRow(Long lessonId, Long total) {
        return new LessonCountRow() {
            @Override public Long getLessonId() { return lessonId; }
            @Override public Long getTotal() { return total; }
        };
    }

    private static final class Harness {
        final Map<Long, Lesson> lessons = new HashMap<>();
        final List<Long> deletedAiGenerationsFor = new ArrayList<>();
        final Map<Long, Long> batchDocCounts = new HashMap<>();
        final Map<Long, Long> batchQuizCounts = new HashMap<>();
        final Map<Long, Long> batchPublishedQuizCounts = new HashMap<>();
        final Map<Long, Long> batchFlashcardCounts = new HashMap<>();
        final Map<Long, Long> batchPublishedFlashcardCounts = new HashMap<>();
        final Map<Long, Long> batchAttemptCounts = new HashMap<>();
        Course course = Course.builder().id(20L).isPublished(false).build();
        long totalQuizzes;
        long publishedQuizzes;
        long totalFlashcards;
        long publishedFlashcards;
        long quizAttempts;
        boolean activeEnrolled;
        boolean rawEnrolled;
        int perLessonCountCalls;
        int batchCountCalls;
        final List<List<Integer>> flushSnapshots = new ArrayList<>();
        long nextId = 100L;

        final LessonRepository lessonRepository;
        final CourseRepository courseRepository;
        final LessonDocumentRepository documentRepository;
        final UserRepository userRepository;
        final LessonService service;

        Harness() {
            this.lessonRepository = proxy(LessonRepository.class, (method, args) -> switch (method.getName()) {
                case "findById", "findByIdForUpdate" -> Optional.ofNullable(lessons.get(args[0]));
                case "findCourseIdById" -> Optional.ofNullable(lessons.get(args[0])).map(Lesson::getCourseId);
                case "findStatusById" -> Optional.ofNullable(lessons.get(args[0])).map(Lesson::getStatus);
                case "findByCourseIdOrderByOrderIndexAsc", "findByCourseIdForUpdate" ->
                    lessons.values().stream()
                        .filter(l -> l.getCourseId().equals(args[0]))
                        .sorted(Comparator.comparingInt(Lesson::getOrderIndex))
                        .toList();
                case "findByCourseIdAndStatusOrderByOrderIndexAsc" ->
                    lessons.values().stream()
                        .filter(l -> l.getCourseId().equals(args[0]) && l.getStatus() == args[1])
                        .sorted(Comparator.comparingInt(Lesson::getOrderIndex))
                        .toList();
                case "countByCourseIdAndStatus" -> lessons.values().stream()
                    .filter(l -> l.getCourseId().equals(args[0]) && l.getStatus() == args[1]).count();
                case "countCourse" -> 1L;
                case "countCourseOwner" -> ((Long) args[1]).longValue() == 7L ? 1L : 0L;
                case "countCourseMember" -> activeEnrolled ? 1L : 0L;
                case "countRawCourseMember" -> rawEnrolled ? 1L : 0L;
                case "countQuizAttempts" -> { perLessonCountCalls++; yield quizAttempts; }
                case "countQuizzes" -> { perLessonCountCalls++; yield totalQuizzes; }
                case "countPublishedQuizzes" -> { perLessonCountCalls++; yield publishedQuizzes; }
                case "countFlashcards" -> { perLessonCountCalls++; yield totalFlashcards; }
                case "countPublishedFlashcards" -> { perLessonCountCalls++; yield publishedFlashcards; }
                case "countQuizAttemptsByLessonIds" -> { batchCountCalls++; yield rows(batchAttemptCounts); }
                case "countQuizzesByLessonIds" -> { batchCountCalls++; yield rows(batchQuizCounts); }
                case "countPublishedQuizzesByLessonIds" -> { batchCountCalls++; yield rows(batchPublishedQuizCounts); }
                case "countFlashcardsByLessonIds" -> { batchCountCalls++; yield rows(batchFlashcardCounts); }
                case "countPublishedFlashcardsByLessonIds" -> { batchCountCalls++; yield rows(batchPublishedFlashcardCounts); }
                case "deleteAiGenerations" -> {
                    deletedAiGenerationsFor.add((Long) args[0]);
                    yield null;
                }
                case "save" -> {
                    Lesson input = (Lesson) args[0];
                    Lesson saved = input.getId() != null ? input : Lesson.builder()
                        .id(nextId++).courseId(input.getCourseId()).title(input.getTitle())
                        .videoUrl(input.getVideoUrl()).transcriptText(input.getTranscriptText())
                        .orderIndex(input.getOrderIndex()).status(input.getStatus()).build();
                    lessons.put(saved.getId(), saved);
                    yield saved;
                }
                case "delete" -> {
                    lessons.remove(((Lesson) args[0]).getId());
                    yield null;
                }
                case "flush" -> {
                    flushSnapshots.add(lessons.values().stream()
                        .map(Lesson::getOrderIndex)
                        .sorted()
                        .toList());
                    yield null;
                }
                default -> throw new UnsupportedOperationException("LessonRepository." + method.getName());
            });

            this.courseRepository = proxy(CourseRepository.class, (method, args) -> switch (method.getName()) {
                case "findById", "findByIdForUpdate" ->
                    course != null && course.getId().equals(args[0]) ? Optional.of(course) : Optional.empty();
                default -> throw new UnsupportedOperationException("CourseRepository." + method.getName());
            });

            this.documentRepository = proxy(LessonDocumentRepository.class, (method, args) -> switch (method.getName()) {
                case "findByLessonIdOrderByCreatedAtAsc" -> List.of();
                case "countByLessonId" -> { perLessonCountCalls++; yield 0L; }
                case "countByLessonIds" -> { batchCountCalls++; yield rows(batchDocCounts); }
                case "deleteAll", "flush" -> null;
                default -> throw new UnsupportedOperationException("LessonDocumentRepository." + method.getName());
            });

            this.userRepository = proxy(UserRepository.class, (method, args) -> switch (method.getName()) {
                case "findById" -> {
                    long id = (Long) args[0];
                    UserRole role = id == 1L ? UserRole.ADMIN : id == 7L ? UserRole.TEACHER : UserRole.STUDENT;
                    yield Optional.of(User.builder().id(id).role(role).isActive(true).build());
                }
                default -> throw new UnsupportedOperationException("UserRepository." + method.getName());
            });

            DocumentStorage storage = proxy(DocumentStorage.class, (m, a) -> null);
            DocumentTextExtractor extractor = file -> "extracted";
            DocumentCleanupService cleanup = new DocumentCleanupService(null, storage, null);

            this.service = new LessonService(
                lessonRepository,
                courseRepository,
                documentRepository,
                userRepository,
                storage,
                extractor,
                cleanup,
                new YouTubeUrlPolicy(),
                new LessonDocumentInspector(),
                new LessonResponseAssembler(lessonRepository, courseRepository, documentRepository)
            );
        }

        void putLesson(Lesson lesson) {
            lessons.put(lesson.getId(), lesson);
        }

        private static List<LessonCountRow> rows(Map<Long, Long> map) {
            return map.entrySet().stream().map(e -> countRow(e.getKey(), e.getValue())).toList();
        }

        @SuppressWarnings("unchecked")
        private static <T> T proxy(Class<T> type, Handler handler) {
            AtomicBoolean active = new AtomicBoolean(true);
            return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[] {type},
                (p, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> type.getSimpleName() + "Stub";
                            case "hashCode" -> System.identityHashCode(p);
                            case "equals" -> p == args[0];
                            default -> active.get();
                        };
                    }
                    return handler.invoke(method, args);
                }
            );
        }

        @FunctionalInterface
        private interface Handler {
            Object invoke(java.lang.reflect.Method method, Object[] args) throws Throwable;
        }
    }
}
