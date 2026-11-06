package vn.edu.learningai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.learningai.common.enums.SourceType;
import vn.edu.learningai.common.enums.UserRole;
import vn.edu.learningai.dto.request.LessonReorderRequest;
import vn.edu.learningai.dto.request.LessonWriteRequest;
import vn.edu.learningai.dto.response.LessonDocumentResponse;
import vn.edu.learningai.dto.response.LessonResponse;
import vn.edu.learningai.entity.Lesson;
import vn.edu.learningai.entity.LessonDocument;
import vn.edu.learningai.entity.User;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.repository.LessonDocumentRepository;
import vn.edu.learningai.repository.LessonRepository;
import vn.edu.learningai.repository.UserRepository;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LessonService implements LessonAccessPolicy, LessonSourceTextProvider {
    private static final long MAX_DOCUMENT_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_MEDIA_TYPES = Set.of(
        "application/pdf",
        "text/plain",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final LessonRepository lessonRepository;
    private final LessonDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentStorage documentStorage;
    private final DocumentTextExtractor textExtractor;

    @Transactional(readOnly = true)
    public List<LessonResponse> listForCourse(Long courseId, Long userId) {
        assertCanViewCourse(courseId, userId);
        return lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public LessonResponse create(Long courseId, LessonWriteRequest request, Long userId) {
        assertCanManageCourse(courseId, userId);
        List<Lesson> lessons = lessonRepository.findByCourseIdForUpdate(courseId);
        Lesson lesson = Lesson.builder()
            .courseId(courseId)
            .title(request.title().trim())
            .videoUrl(request.videoUrl() == null ? "" : request.videoUrl().trim())
            .transcriptText(normalizeText(request.transcriptText()))
            .orderIndex(lessons.size() + 1)
            .build();
        return toResponse(lessonRepository.save(lesson));
    }

    @Transactional(readOnly = true)
    public LessonResponse get(Long lessonId, Long userId) {
        Lesson lesson = requireLesson(lessonId);
        assertCanViewLesson(lessonId, userId);
        return toResponse(lesson);
    }

    @Transactional
    public LessonResponse update(Long lessonId, LessonWriteRequest request, Long userId) {
        Lesson lesson = requireManage(lessonId, userId);
        lesson.updateContent(request.title(), request.videoUrl(), request.transcriptText());
        return toResponse(lesson);
    }

    @Transactional
    public List<LessonResponse> reorder(Long courseId, LessonReorderRequest request, Long userId) {
        assertCanManageCourse(courseId, userId);
        List<Lesson> lessons = lessonRepository.findByCourseIdForUpdate(courseId);
        List<Long> requestedIds = request.lessonIds();
        Set<Long> existingIds = lessons.stream().map(Lesson::getId).collect(java.util.stream.Collectors.toSet());
        if (requestedIds.size() != lessons.size() || new HashSet<>(requestedIds).size() != requestedIds.size()
            || !existingIds.equals(new HashSet<>(requestedIds))) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "lesson_ids must contain every lesson of the course exactly once");
        }
        for (int index = 0; index < lessons.size(); index++) {
            lessons.get(index).changeOrder(index + lessons.size() + 1000);
        }
        lessonRepository.flush();
        var byId = lessons.stream().collect(java.util.stream.Collectors.toMap(Lesson::getId, lesson -> lesson));
        for (int index = 0; index < requestedIds.size(); index++) {
            byId.get(requestedIds.get(index)).changeOrder(index + 1);
        }
        lessonRepository.flush();
        return requestedIds.stream().map(byId::get).map(this::toResponse).toList();
    }

    @Transactional
    public void delete(Long lessonId, Long userId) {
        Lesson lesson = requireManage(lessonId, userId);
        if (lessonRepository.hasQuizAttempts(lessonId)) {
            throw new ApiException(ErrorCode.LESSON_DELETE_BLOCKED);
        }
        if (lessonRepository.countQuizzes(lessonId) > 0 || lessonRepository.countFlashcards(lessonId) > 0) {
            throw new ApiException(ErrorCode.INVALID_STATE, "Delete draft quiz and flashcard content before deleting the lesson");
        }
        documentRepository.findByLessonIdOrderByCreatedAtAsc(lessonId).forEach(document -> documentStorage.delete(document.getStorageKey()));
        documentRepository.deleteAll(documentRepository.findByLessonIdOrderByCreatedAtAsc(lessonId));
        lessonRepository.delete(lesson);
        compactOrders(lesson.getCourseId());
    }

    @Transactional(readOnly = true)
    public List<LessonDocumentResponse> listDocuments(Long lessonId, Long userId) {
        assertCanViewLesson(lessonId, userId);
        return documentRepository.findByLessonIdOrderByCreatedAtAsc(lessonId).stream().map(this::toDocumentResponse).toList();
    }

    @Transactional
    public LessonDocumentResponse uploadDocument(Long lessonId, MultipartFile file, Long userId) {
        requireManage(lessonId, userId);
        validateDocument(file);
        String extractedText = textExtractor.extract(file);
        String storageKey = documentStorage.store(file);
        try {
            LessonDocument document = LessonDocument.builder()
                .lessonId(lessonId)
                .uploadedBy(userId)
                .originalName(safeOriginalName(file))
                .storageKey(storageKey)
                .mediaType(file.getContentType())
                .sizeBytes(file.getSize())
                .extractedText(extractedText)
                .contentHash(sha256(file))
                .build();
            return toDocumentResponse(documentRepository.save(document));
        } catch (RuntimeException exception) {
            documentStorage.delete(storageKey);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public DocumentDownload downloadDocument(Long documentId, Long userId) {
        LessonDocument document = documentRepository.findById(documentId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Document not found"));
        assertCanViewLesson(document.getLessonId(), userId);
        return new DocumentDownload(documentStorage.load(document.getStorageKey()), document.getOriginalName(), document.getMediaType());
    }

    @Transactional
    public void deleteDocument(Long documentId, Long userId) {
        LessonDocument document = documentRepository.findById(documentId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Document not found"));
        requireManage(document.getLessonId(), userId);
        documentStorage.delete(document.getStorageKey());
        documentRepository.delete(document);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertCanManageLesson(Long lessonId, Long userId) {
        requireManage(lessonId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public LessonViewAccess assertCanViewLesson(Long lessonId, Long userId) {
        Lesson lesson = requireLesson(lessonId);
        if (isAdmin(userId) || lessonRepository.isCourseOwner(lesson.getCourseId(), userId)) {
            return LessonViewAccess.MANAGER;
        }
        if (lessonRepository.isCourseMember(lesson.getCourseId(), userId)) {
            return LessonViewAccess.ENROLLED_STUDENT;
        }
        throw new ApiException(ErrorCode.ENROLLMENT_REQUIRED);
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

    private Lesson requireManage(Long lessonId, Long userId) {
        Lesson lesson = requireLesson(lessonId);
        if (!isAdmin(userId) && !lessonRepository.isCourseOwner(lesson.getCourseId(), userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        return lesson;
    }

    private void assertCanManageCourse(Long courseId, Long userId) {
        if (!lessonRepository.courseExists(courseId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Course not found");
        }
        if (!isAdmin(userId) && !lessonRepository.isCourseOwner(courseId, userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
    }

    private void assertCanViewCourse(Long courseId, Long userId) {
        if (!lessonRepository.courseExists(courseId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Course not found");
        }
        if (!isAdmin(userId) && !lessonRepository.isCourseOwner(courseId, userId)
            && !lessonRepository.isCourseMember(courseId, userId)) {
            throw new ApiException(ErrorCode.ENROLLMENT_REQUIRED);
        }
    }

    private boolean isAdmin(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
        return user.getRole() == UserRole.ADMIN;
    }

    private Lesson requireLesson(Long lessonId) {
        return lessonRepository.findById(lessonId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Lesson not found"));
    }

    private LessonResponse toResponse(Lesson lesson) {
        boolean blocked = lessonRepository.hasQuizAttempts(lesson.getId());
        return new LessonResponse(
            lesson.getId(), lesson.getCourseId(), lesson.getTitle(), lesson.getVideoUrl(), lesson.getTranscriptText(),
            lesson.getOrderIndex(), documentRepository.countByLessonId(lesson.getId()),
            lessonRepository.countQuizzes(lesson.getId()), lessonRepository.countFlashcards(lesson.getId()),
            !blocked, blocked ? "Bài học đã có lịch sử làm quiz." : null, lesson.getCreatedAt(), lesson.getUpdatedAt()
        );
    }

    private LessonDocumentResponse toDocumentResponse(LessonDocument document) {
        return new LessonDocumentResponse(document.getId(), document.getLessonId(), document.getOriginalName(),
            document.getMediaType(), document.getSizeBytes(), document.getExtractedText() != null,
            document.getCreatedAt());
    }

    private void validateDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Document is empty");
        }
        if (file.getSize() > MAX_DOCUMENT_BYTES) {
            throw new ApiException(ErrorCode.FILE_TOO_LARGE);
        }
        if (!ALLOWED_MEDIA_TYPES.contains(file.getContentType())) {
            throw new ApiException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
    }

    private String safeOriginalName(MultipartFile file) {
        String name = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
        name = java.nio.file.Path.of(name).getFileName().toString();
        return name.length() <= 255 ? name : name.substring(name.length() - 255);
    }

    private String sha256(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.readAllBytes()));
        } catch (Exception exception) {
            throw new ApiException(ErrorCode.STORAGE_ERROR, "Could not hash document");
        }
    }

    private String normalizeText(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    private void compactOrders(Long courseId) {
        List<Lesson> lessons = lessonRepository.findByCourseIdForUpdate(courseId);
        for (int index = 0; index < lessons.size(); index++) {
            lessons.get(index).changeOrder(index + 1);
        }
    }

    public record DocumentDownload(Resource resource, String originalName, String mediaType) {
    }
}
