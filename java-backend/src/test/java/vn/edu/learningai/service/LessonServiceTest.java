package vn.edu.learningai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.learningai.common.enums.SourceType;
import vn.edu.learningai.common.enums.UserRole;
import vn.edu.learningai.entity.Lesson;
import vn.edu.learningai.entity.LessonDocument;
import vn.edu.learningai.entity.User;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.repository.LessonDocumentRepository;
import vn.edu.learningai.repository.LessonRepository;
import vn.edu.learningai.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {
    @Mock LessonRepository lessonRepository;
    @Mock LessonDocumentRepository documentRepository;
    @Mock UserRepository userRepository;
    @Mock DocumentStorage documentStorage;
    @Mock DocumentTextExtractor textExtractor;

    LessonService service;

    @BeforeEach
    void setUp() {
        service = new LessonService(lessonRepository, documentRepository, userRepository, documentStorage, textExtractor);
    }

    @Test
    void transcriptSourceReturnsStoredText() {
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson("Nội dung transcript")));

        assertEquals("Nội dung transcript", service.getSourceText(10L, SourceType.TRANSCRIPT, null));
    }

    @Test
    void emptyTranscriptUsesStableErrorCode() {
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson("  ")));

        ApiException exception = assertThrows(ApiException.class,
            () -> service.getSourceText(10L, SourceType.TRANSCRIPT, null));

        assertEquals(ErrorCode.SOURCE_TEXT_EMPTY, exception.getErrorCode());
    }

    @Test
    void documentMustBelongToRequestedLesson() {
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson("transcript")));
        when(documentRepository.findByIdAndLessonId(99L, 10L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class,
            () -> service.getSourceText(10L, SourceType.DOCUMENT, 99L));

        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void enrolledStudentCanViewLesson() {
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson("transcript")));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(UserRole.STUDENT)));
        when(lessonRepository.isCourseOwner(20L, 7L)).thenReturn(false);
        when(lessonRepository.isCourseMember(20L, 7L)).thenReturn(true);

        assertEquals(LessonAccessPolicy.LessonViewAccess.ENROLLED_STUDENT,
            service.assertCanViewLesson(10L, 7L));
    }

    @Test
    void unrelatedUserCannotViewLesson() {
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson("transcript")));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(UserRole.STUDENT)));

        ApiException exception = assertThrows(ApiException.class,
            () -> service.assertCanViewLesson(10L, 7L));

        assertEquals(ErrorCode.ENROLLMENT_REQUIRED, exception.getErrorCode());
    }

    private Lesson lesson(String transcript) {
        return Lesson.builder().id(10L).courseId(20L).title("Bài 1").videoUrl("")
            .transcriptText(transcript).orderIndex(1).build();
    }

    private User user(UserRole role) {
        return User.builder().id(7L).role(role).isActive(true).build();
    }
}
