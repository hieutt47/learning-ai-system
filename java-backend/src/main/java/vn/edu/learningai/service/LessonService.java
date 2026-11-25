package vn.edu.learningai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.learningai.common.enums.LessonStatus;
import vn.edu.learningai.common.enums.SourceType;
import vn.edu.learningai.common.enums.UserRole;
import vn.edu.learningai.dto.request.LessonReorderRequest;
import vn.edu.learningai.dto.request.LessonWriteRequest;
import vn.edu.learningai.dto.response.LessonDocumentResponse;
import vn.edu.learningai.dto.response.LessonResponse;
import vn.edu.learningai.entity.Course;
import vn.edu.learningai.entity.Lesson;
import vn.edu.learningai.entity.LessonDocument;
import vn.edu.learningai.entity.User;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.repository.CourseRepository;
import vn.edu.learningai.repository.LessonDocumentRepository;
import vn.edu.learningai.repository.LessonRepository;
import vn.edu.learningai.repository.UserRepository;
import vn.edu.learningai.service.lesson.LessonDocumentInspector;
import vn.edu.learningai.service.lesson.LessonResponseAssembler;
import vn.edu.learningai.service.lesson.YouTubeUrlPolicy;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonService implements LessonAccessPolicy, LessonSourceTextProvider {
    private static final int TEMP_ORDER_OFFSET = 1000;

    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final LessonDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentStorage documentStorage;
    private final DocumentTextExtractor textExtractor;
    private final DocumentCleanupService documentCleanup;
    private final YouTubeUrlPolicy youTubeUrlPolicy;
    private final LessonDocumentInspector documentInspector;
    private final LessonResponseAssembler responseAssembler;

    @Transactional(readOnly = true)
    public List<LessonResponse> listForCourse(Long courseId, Long userId) {
        boolean managerView = assertCanViewCourse(courseId, userId) == LessonViewAccess.MANAGER;
        boolean canManage = managerView && lessonRepository.countCourseOwner(courseId, userId) > 0;
        List<Lesson> lessons = managerView
            ? lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId)
            : lessonRepository.findByCourseIdAndStatusOrderByOrderIndexAsc(courseId, LessonStatus.PUBLISHED);
        return responseAssembler.toResponses(courseId, lessons, managerView, canManage);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LessonResponse create(Long courseId, LessonWriteRequest request, Long userId) {
        Course course = lockManageCourse(courseId, userId);
        List<Lesson> lessons = lessonRepository.findByCourseIdForUpdate(courseId);
        int nextOrderIndex = lessons.stream()
            .mapToInt(Lesson::getOrderIndex)
            .max()
            .orElse(0) + 1;
        Lesson lesson = Lesson.builder()
            .courseId(courseId)
            .title(request.title().trim())
            .videoUrl(youTubeUrlPolicy.normalizeOptional(request.videoUrl()))
            .transcriptText(normalizeText(request.transcriptText()))
            .orderIndex(nextOrderIndex)
            .status(Boolean.TRUE.equals(course.getIsPublished()) ? LessonStatus.DRAFT : LessonStatus.PUBLISHED)
            .build();
        return responseAssembler.toResponse(lessonRepository.save(lesson), true, true);
    }

    @Transactional(readOnly = true)
    public LessonResponse get(Long lessonId, Long userId) {
        Lesson lesson = requireLesson(lessonId);
        boolean managerView = assertCanViewLesson(lessonId, userId) == LessonViewAccess.MANAGER;
        boolean canManage = managerView && lessonRepository.countCourseOwner(lesson.getCourseId(), userId) > 0;
        return responseAssembler.toResponse(lesson, managerView, canManage);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LessonResponse update(Long lessonId, LessonWriteRequest request, Long userId) {
        LockedLessonContext context = lockManageLesson(lessonId, userId);
        requireEditable(context.lesson());
        context.lesson().updateContent(
            request.title(),
            youTubeUrlPolicy.normalizeOptional(request.videoUrl()),
            request.transcriptText()
        );
        return responseAssembler.toResponse(context.lesson(), true, true);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LessonResponse changeStatus(Long lessonId, LessonStatus status, Long userId) {
        LockedLessonContext context = lockManageLesson(lessonId, userId);
        Lesson lesson = context.lesson();
        if (lesson.getStatus() == status) {
            return responseAssembler.toResponse(lesson, true, true);
        }
        if (lesson.getStatus() == LessonStatus.DRAFT && status == LessonStatus.PUBLISHED) {
            lesson.changeStatus(status);
        } else if (lesson.getStatus() == LessonStatus.PUBLISHED && status == LessonStatus.ARCHIVED) {
            requireAnotherPublishedLesson(lesson, context.course());
            lesson.changeStatus(status);
        } else {
            throw new ApiException(ErrorCode.INVALID_STATE, "Allowed transitions: DRAFT to PUBLISHED, PUBLISHED to ARCHIVED");
        }
        return responseAssembler.toResponse(lesson, true, true);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<LessonResponse> reorder(Long courseId, LessonReorderRequest request, Long userId) {
        lockManageCourse(courseId, userId);
        List<Lesson> lessons = lessonRepository.findByCourseIdForUpdate(courseId);
        List<Long> requestedIds = request.lessonIds();
        Set<Long> existingIds = lessons.stream().map(Lesson::getId).collect(Collectors.toSet());
        if (requestedIds.size() != lessons.size() || new HashSet<>(requestedIds).size() != requestedIds.size()
            || !existingIds.equals(new HashSet<>(requestedIds))) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "lesson_ids must contain every lesson of the course exactly once");
        }

        Map<Long, Lesson> byId = lessons.stream().collect(Collectors.toMap(Lesson::getId, lesson -> lesson));
        for (int index = 0; index < requestedIds.size(); index++) {
            Lesson target = byId.get(requestedIds.get(index));
            if (target.getStatus() == LessonStatus.ARCHIVED && target.getOrderIndex() != index + 1) {
                throw new ApiException(ErrorCode.INVALID_STATE, "Archived lesson is read-only");
            }
        }

        shiftAndAssignOrders(lessons, requestedIds.stream().map(byId::get).toList());
        return responseAssembler.toResponses(courseId, requestedIds.stream().map(byId::get).toList(), true, true);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void delete(Long lessonId, Long userId) {
        LockedLessonContext context = lockManageLesson(lessonId, userId);
        Lesson lesson = context.lesson();
        requireEditable(lesson);
        requireAnotherPublishedLesson(lesson, context.course());
        if (lessonRepository.countQuizAttempts(lessonId) > 0) {
            throw new ApiException(ErrorCode.LESSON_DELETE_BLOCKED);
        }
        if (lessonRepository.countQuizzes(lessonId) > 0 || lessonRepository.countFlashcards(lessonId) > 0) {
            throw new ApiException(ErrorCode.INVALID_STATE, "Delete draft quiz and flashcard content before deleting the lesson");
        }

        // No quiz/flashcard references ai_generations for this lesson anymore; clean orphaned logs to satisfy FK.
        lessonRepository.deleteAiGenerations(lessonId);
        var documents = documentRepository.findByLessonIdOrderByCreatedAtAsc(lessonId);
        documents.forEach(document -> documentCleanup.afterCommit(document.getStorageKey()));
        documentRepository.deleteAll(documents);
        documentRepository.flush();
        lessonRepository.delete(lesson);
        lessonRepository.flush();
        compactOrders(lesson.getCourseId());
    }

    @Transactional(readOnly = true)
    public List<LessonDocumentResponse> listDocuments(Long lessonId, Long userId) {
        assertCanViewLesson(lessonId, userId);
        return documentRepository.findByLessonIdOrderByCreatedAtAsc(lessonId).stream()
            .map(responseAssembler::toDocumentResponse)
            .toList();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LessonDocumentResponse uploadDocument(Long lessonId, MultipartFile file, Long userId) {
        // Pre-authorize with scalar queries so expensive Tika parsing neither runs for unauthorized
        // callers nor populates Hibernate's L1 cache with pre-lock Lesson/Course state.
        precheckUploadAuthority(lessonId, userId);
        LessonDocumentInspector.InspectedDocument inspected = documentInspector.inspect(file);
        String extractedText = textExtractor.extract(file, inspected.mediaType());

        LockedLessonContext locked = lockManageLesson(lessonId, userId);
        requireEditable(locked.lesson());

        String storageKey = documentStorage.newKey(file);
        documentCleanup.stageUpload(storageKey);
        documentStorage.store(storageKey, file);
        LessonDocument document = LessonDocument.builder()
            .lessonId(lessonId)
            .uploadedBy(userId)
            .originalName(inspected.originalName())
            .storageKey(storageKey)
            .mediaType(inspected.mediaType())
            .sizeBytes(inspected.sizeBytes())
            .extractedText(extractedText)
            .contentHash(inspected.contentHash())
            .build();
        var saved = documentRepository.saveAndFlush(document);
        documentCleanup.adoptUpload(storageKey);
        return responseAssembler.toDocumentResponse(saved);
    }

    @Transactional(readOnly = true)
    public DocumentDownload downloadDocument(Long documentId, Long userId) {
        LessonDocument document = documentRepository.findById(documentId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Document not found"));
        assertCanViewLesson(document.getLessonId(), userId);
        return new DocumentDownload(
            documentStorage.load(document.getStorageKey()),
            document.getOriginalName(),
            document.getMediaType()
        );
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deleteDocument(Long documentId, Long userId) {
        LessonDocument document = documentRepository.findById(documentId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Document not found"));
        LockedLessonContext locked = lockManageLesson(document.getLessonId(), userId);
        requireEditable(locked.lesson());
        documentCleanup.afterCommit(document.getStorageKey());
        documentRepository.delete(document);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertCanManageLesson(Long lessonId, Long userId) {
        requireEditable(requireManage(lessonId, userId));
    }

    @Override
    @Transactional(readOnly = true)
    public LessonViewAccess assertCanViewLesson(Long lessonId, Long userId) {
        Lesson lesson = requireLesson(lessonId);
        UserRole role = currentUserRole(userId);
        if (role == UserRole.ADMIN || lessonRepository.countCourseOwner(lesson.getCourseId(), userId) > 0) {
            return LessonViewAccess.MANAGER;
        }
        if (role == UserRole.STUDENT) {
            if (lessonRepository.countCourseMember(lesson.getCourseId(), userId) > 0) {
                if (lesson.getStatus() != LessonStatus.PUBLISHED) {
                    throw new ApiException(ErrorCode.NOT_FOUND, "Lesson not found");
                }
                return LessonViewAccess.ENROLLED_STUDENT;
            }
            if (lessonRepository.countRawCourseMember(lesson.getCourseId(), userId) > 0) {
                throw new ApiException(ErrorCode.FORBIDDEN, "Course is currently unavailable for learning");
            }
            throw new ApiException(ErrorCode.ENROLLMENT_REQUIRED);
        }
        throw new ApiException(ErrorCode.FORBIDDEN);
    }

    @Override
    @Transactional(readOnly = true)
    public String getSourceText(Long lessonId, SourceType type, Long documentId) {
        Lesson lesson = requireLesson(lessonId);
        String text;
        if (type == SourceType.TRANSCRIPT) {
            text = lesson.getTranscriptText();
        } else if (type == SourceType.DOCUMENT) {
            if (documentId == null) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "document_id is required for DOCUMENT source");
            }
            text = documentRepository.findByIdAndLessonId(documentId, lessonId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Document does not belong to lesson"))
                .getExtractedText();
        } else {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Unsupported source type");
        }
        if (text == null || text.isBlank()) {
            throw new ApiException(ErrorCode.SOURCE_TEXT_EMPTY);
        }
        return text;
    }

    private void precheckUploadAuthority(Long lessonId, Long userId) {
        Long courseId = lessonRepository.findCourseIdById(lessonId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Lesson not found"));
        if (lessonRepository.countCourseOwner(courseId, userId) == 0) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        LessonStatus status = lessonRepository.findStatusById(lessonId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Lesson not found"));
        if (status == LessonStatus.ARCHIVED) {
            throw new ApiException(ErrorCode.INVALID_STATE, "Archived lesson is read-only");
        }
    }

    private LockedLessonContext lockManageLesson(Long lessonId, Long userId) {
        Long courseId = lessonRepository.findCourseIdById(lessonId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Lesson not found"));
        Course course = lockManageCourse(courseId, userId);
        Lesson lockedLesson = lessonRepository.findByIdForUpdate(lessonId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Lesson not found"));
        return new LockedLessonContext(course, lockedLesson);
    }

    private Lesson requireManage(Long lessonId, Long userId) {
        Lesson lesson = requireLesson(lessonId);
        if (lessonRepository.countCourseOwner(lesson.getCourseId(), userId) == 0) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        Course course = courseRepository.findById(lesson.getCourseId())
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Course not found"));
        requireActiveCourse(course);
        return lesson;
    }

    private Course lockManageCourse(Long courseId, Long userId) {
        Course course = courseRepository.findByIdForUpdate(courseId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Course not found"));
        if (lessonRepository.countCourseOwner(courseId, userId) == 0) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        requireActiveCourse(course);
        return course;
    }

    private void requireActiveCourse(Course course) {
        if (course.getArchivedAt() != null) {
            throw new ApiException(ErrorCode.INVALID_STATE, "Archived course is read-only");
        }
    }

    private void requireAnotherPublishedLesson(Lesson lesson, Course course) {
        if (lesson.getStatus() == LessonStatus.PUBLISHED && Boolean.TRUE.equals(course.getIsPublished())
            && lessonRepository.countByCourseIdAndStatus(course.getId(), LessonStatus.PUBLISHED) <= 1) {
            throw new ApiException(ErrorCode.INVALID_STATE,
                "Publish a replacement lesson or unpublish the course first");
        }
    }

    private void requireEditable(Lesson lesson) {
        if (lesson.getStatus() == LessonStatus.ARCHIVED) {
            throw new ApiException(ErrorCode.INVALID_STATE, "Archived lesson is read-only");
        }
    }

    private LessonViewAccess assertCanViewCourse(Long courseId, Long userId) {
        if (lessonRepository.countCourse(courseId) == 0) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Course not found");
        }
        UserRole role = currentUserRole(userId);
        if (role == UserRole.ADMIN || lessonRepository.countCourseOwner(courseId, userId) > 0) {
            return LessonViewAccess.MANAGER;
        }
        if (role == UserRole.STUDENT) {
            if (lessonRepository.countCourseMember(courseId, userId) > 0) {
                return LessonViewAccess.ENROLLED_STUDENT;
            }
            if (lessonRepository.countRawCourseMember(courseId, userId) > 0) {
                throw new ApiException(ErrorCode.FORBIDDEN, "Course is currently unavailable for learning");
            }
            throw new ApiException(ErrorCode.ENROLLMENT_REQUIRED);
        }
        throw new ApiException(ErrorCode.FORBIDDEN);
    }

    private UserRole currentUserRole(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
        return user.getRole();
    }

    private Lesson requireLesson(Long lessonId) {
        return lessonRepository.findById(lessonId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Lesson not found"));
    }

    private String normalizeText(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    private void compactOrders(Long courseId) {
        List<Lesson> lessons = lessonRepository.findByCourseIdForUpdate(courseId);
        if (lessons.isEmpty()) {
            return;
        }
        shiftAndAssignOrders(lessons, lessons);
    }

    private void shiftAndAssignOrders(List<Lesson> allLessons, List<Lesson> finalOrder) {
        int baseOffset = allLessons.stream().mapToInt(Lesson::getOrderIndex).max().orElse(0) + TEMP_ORDER_OFFSET;
        for (int index = 0; index < allLessons.size(); index++) {
            allLessons.get(index).changeOrder(baseOffset + index + 1);
        }
        lessonRepository.flush();
        for (int index = 0; index < finalOrder.size(); index++) {
            finalOrder.get(index).changeOrder(index + 1);
        }
        lessonRepository.flush();
    }

    private record LockedLessonContext(Course course, Lesson lesson) {}

    public record DocumentDownload(Resource resource, String originalName, String mediaType) {}
}
