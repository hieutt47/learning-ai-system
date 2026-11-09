package vn.edu.learningai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.MockMakers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import vn.edu.learningai.common.enums.FlashcardStatus;
import vn.edu.learningai.common.enums.GenerationType;
import vn.edu.learningai.dto.request.CardUpdateRequest;
import vn.edu.learningai.dto.request.CardWriteRequest;
import vn.edu.learningai.dto.response.CardResponse;
import vn.edu.learningai.dto.response.PageResponse;
import vn.edu.learningai.entity.Flashcard;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.mapper.FlashcardMapper;
import vn.edu.learningai.repository.FlashcardRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FlashcardServiceTest {

    private static final long TEACHER = 2L;
    private static final long STUDENT = 4L;
    private static final long LESSON = 10L;

    private FlashcardRepository repository;
    private LessonAccessPolicy access;
    private AiGenerationService aiGenerationService;
    private FlashcardService service;

    @BeforeEach
    void setUp() {
        repository = mock(FlashcardRepository.class);
        access = mock(LessonAccessPolicy.class);
        // Dự án dùng mock-maker-proxy (chỉ mock interface); class thì chọn riêng mock maker kiểu lớp con.
        aiGenerationService = mock(AiGenerationService.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        when(repository.save(any())).thenAnswer(inv -> {
            Flashcard f = inv.getArgument(0);
            if (f.getId() == null) {
                f.setId(50L);
            }
            return f;
        });
        service = new FlashcardService(repository, access, aiGenerationService, Mappers.getMapper(FlashcardMapper.class));
    }

    private Flashcard card(FlashcardStatus status) {
        Flashcard f = Flashcard.builder().id(50L).lessonId(LESSON).frontText("JVM?").backText("Máy ảo Java")
            .status(status).createdBy(TEACHER).build();
        when(repository.findById(50L)).thenReturn(Optional.of(f));
        return f;
    }

    @Test
    void saveDraftWithGenerationMarksGeneratedByAi() {
        CardResponse card = service.saveDraft(LESSON, TEACHER, new CardWriteRequest("  JVM là gì? ", "Máy ảo", 7L));

        verify(access).assertCanManageLesson(LESSON, TEACHER);
        verify(aiGenerationService).validateForSave(7L, LESSON, TEACHER, GenerationType.FLASHCARD);
        assertThat(card.id()).isEqualTo("50");
        assertThat(card.status()).isEqualTo(FlashcardStatus.DRAFT);
        assertThat(card.generatedByAi()).isTrue();
        assertThat(card.frontText()).isEqualTo("JVM là gì?");
    }

    @Test
    void saveDraftWithoutGenerationIsManual() {
        CardResponse card = service.saveDraft(LESSON, TEACHER, new CardWriteRequest("A", "B", null));
        assertThat(card.generatedByAi()).isFalse();
        verify(aiGenerationService, never()).validateForSave(any(), any(), any(), any());
    }

    @Test
    void lifecycleDraftPublishedClosed() {
        Flashcard f = card(FlashcardStatus.DRAFT);

        assertThat(service.changeStatus(50L, TEACHER, FlashcardStatus.PUBLISHED).status())
            .isEqualTo(FlashcardStatus.PUBLISHED);
        assertThat(f.getPublishedAt()).isNotNull();
        // publish lặp lại trả dữ liệu cũ
        assertThat(service.publish(50L, TEACHER).status()).isEqualTo(FlashcardStatus.PUBLISHED);

        assertThat(service.changeStatus(50L, TEACHER, FlashcardStatus.CLOSED).status())
            .isEqualTo(FlashcardStatus.CLOSED);
        assertThat(f.getClosedAt()).isNotNull();

        assertThatThrownBy(() -> service.publish(50L, TEACHER))
            .isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATE));
    }

    @Test
    void draftCannotBeClosedAndStatusDraftIsRejected() {
        card(FlashcardStatus.DRAFT);
        assertThatThrownBy(() -> service.close(50L, TEACHER))
            .isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATE));
        assertThatThrownBy(() -> service.changeStatus(50L, TEACHER, FlashcardStatus.DRAFT))
            .isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void publishedCardIsImmutable() {
        card(FlashcardStatus.PUBLISHED);
        assertThatThrownBy(() -> service.updateDraft(50L, TEACHER, new CardUpdateRequest("x", null)))
            .isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATE));
        assertThatThrownBy(() -> service.deleteDraft(50L, TEACHER))
            .isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATE));
        verify(repository, never()).delete(any());
    }

    @Test
    void updateDraftOnlyChangesSentFields() {
        Flashcard f = card(FlashcardStatus.DRAFT);
        service.updateDraft(50L, TEACHER, new CardUpdateRequest(null, " Mới "));
        assertThat(f.getFrontText()).isEqualTo("JVM?");
        assertThat(f.getBackText()).isEqualTo("Mới");
    }

    @Test
    void studentListsOnlyPublishedAndManagerListsAll() {
        Page<Flashcard> empty = new PageImpl<>(List.of());
        when(repository.findByLessonIdAndStatus(eq(LESSON), eq(FlashcardStatus.PUBLISHED), any(Pageable.class)))
            .thenReturn(empty);
        when(repository.findByLessonId(eq(LESSON), any(Pageable.class))).thenReturn(empty);

        when(access.assertCanViewLesson(LESSON, STUDENT)).thenReturn(LessonAccessPolicy.LessonViewAccess.ENROLLED_STUDENT);
        PageResponse<CardResponse> page = service.listFlashcards(LESSON, STUDENT, 2, 5);
        assertThat(page.getPage()).isEqualTo(2);
        assertThat(page.getPageSize()).isEqualTo(5);
        verify(repository).findByLessonIdAndStatus(eq(LESSON), eq(FlashcardStatus.PUBLISHED), any(Pageable.class));

        when(access.assertCanViewLesson(LESSON, TEACHER)).thenReturn(LessonAccessPolicy.LessonViewAccess.MANAGER);
        service.listFlashcards(LESSON, TEACHER, 1, 20);
        verify(repository).findByLessonId(eq(LESSON), any(Pageable.class));
    }

    @Test
    void listRejectsBadPaging() {
        assertThatThrownBy(() -> service.listFlashcards(LESSON, TEACHER, 0, 20)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.listFlashcards(LESSON, TEACHER, 1, 101)).isInstanceOf(ApiException.class);
    }
}
