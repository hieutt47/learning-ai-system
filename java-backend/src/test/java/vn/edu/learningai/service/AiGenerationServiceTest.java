package vn.edu.learningai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.learningai.ai.AIClient;
import vn.edu.learningai.ai.AIClientException;
import vn.edu.learningai.ai.AiCandidateValidator;
import vn.edu.learningai.ai.AiPromptBuilder;
import vn.edu.learningai.ai.AiProperties;
import vn.edu.learningai.ai.MockAIClient;
import vn.edu.learningai.common.enums.Difficulty;
import vn.edu.learningai.common.enums.GenerationStatus;
import vn.edu.learningai.common.enums.GenerationType;
import vn.edu.learningai.common.enums.SourceType;
import vn.edu.learningai.dto.request.GenerationRequest;
import vn.edu.learningai.dto.response.CardCandidateResponse;
import vn.edu.learningai.dto.response.QuizCandidateResponse;
import vn.edu.learningai.entity.AIGeneration;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.repository.AiGenerationRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiGenerationServiceTest {

    private static final long TEACHER = 2L;
    private static final long LESSON = 10L;
    private static final String TRANSCRIPT =
        "Biến trong Java phải được khai báo kiểu trước khi dùng. Kiểu int lưu số nguyên 32 bit. "
            + "Kiểu double lưu số thực dấu phẩy động.";

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private LessonAccessPolicy access;
    private LessonSourceTextProvider sourceText;
    private AiGenerationRepository repository;
    private AIClient aiClient;
    private AiGenerationService service;

    @BeforeEach
    void setUp() {
        access = mock(LessonAccessPolicy.class);
        sourceText = mock(LessonSourceTextProvider.class);
        repository = mock(AiGenerationRepository.class);
        aiClient = spy(new MockAIClient(jsonMapper));
        when(repository.save(any())).thenAnswer(inv -> {
            AIGeneration g = inv.getArgument(0);
            g.setId(99L);
            return g;
        });
        when(sourceText.getSourceText(LESSON, SourceType.TRANSCRIPT, null)).thenReturn(TRANSCRIPT);
        service = newService(20_000);
    }

    private AiGenerationService newService(int maxSourceChars) {
        AiProperties props = new AiProperties("mock", null, null, null, 90, 8192, maxSourceChars);
        return new AiGenerationService(access, sourceText, new AiPromptBuilder(), aiClient,
            new AiCandidateValidator(jsonMapper), repository, props, jsonMapper);
    }

    private static GenerationRequest quizRequest(int count) {
        return new GenerationRequest(LESSON, SourceType.TRANSCRIPT, null, count, Difficulty.MEDIUM);
    }

    private AIGeneration savedGeneration() {
        ArgumentCaptor<AIGeneration> captor = ArgumentCaptor.forClass(AIGeneration.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void generateQuizReturnsPreviewAndRecordsSucceededGeneration() {
        QuizCandidateResponse response = service.generateQuiz(TEACHER, quizRequest(3));

        assertThat(response.generationId()).isEqualTo("99");
        assertThat(response.lessonId()).isEqualTo("10");
        assertThat(response.sourceTruncated()).isFalse();
        assertThat(response.candidate().questions()).hasSize(3);

        AIGeneration g = savedGeneration();
        assertThat(g.getStatus()).isEqualTo(GenerationStatus.SUCCEEDED);
        assertThat(g.getGenerationType()).isEqualTo(GenerationType.QUIZ);
        assertThat(g.getRequestedCount()).isEqualTo(3);
        assertThat(g.getDifficulty()).isEqualTo(Difficulty.MEDIUM);
        assertThat(g.getProvider()).isEqualTo("mock");
        assertThat(g.getPromptVersion()).isEqualTo(AiPromptBuilder.QUIZ_PROMPT_VERSION);
        assertThat(g.getSourceHash()).hasSize(64);
        assertThat(g.getCreatedBy()).isEqualTo(TEACHER);
        assertThat(g.getCandidateJson()).startsWith("{\"questions\":[").contains("\"is_correct\":true");
        verify(access).assertCanManageLesson(LESSON, TEACHER);
    }

    @Test
    void generateFlashcardsIgnoresDifficulty() {
        GenerationRequest request = new GenerationRequest(LESSON, SourceType.TRANSCRIPT, null, 2, Difficulty.HARD);

        CardCandidateResponse response = service.generateFlashcards(TEACHER, request);

        assertThat(response.candidate().flashcards()).hasSize(2);
        AIGeneration g = savedGeneration();
        assertThat(g.getGenerationType()).isEqualTo(GenerationType.FLASHCARD);
        assertThat(g.getDifficulty()).isNull();
        assertThat(g.getCandidateJson()).contains("\"front_text\"");
    }

    @Test
    void longSourceIsTruncatedAndFlagged() {
        service = newService(50);
        QuizCandidateResponse response = service.generateQuiz(TEACHER, quizRequest(1));

        assertThat(response.sourceTruncated()).isTrue();
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(aiClient).generate(prompt.capture());
        assertThat(prompt.getValue()).contains(TRANSCRIPT.substring(0, 50)).doesNotContain(TRANSCRIPT.substring(0, 51));
    }

    @Test
    void truncateDoesNotSplitSurrogatePair() {
        assertThat(AiGenerationService.truncate("ab😀cd", 3)).isEqualTo("ab");
    }

    @Test
    void quizWithoutDifficultyIsRejectedBeforeCallingAi() {
        GenerationRequest request = new GenerationRequest(LESSON, SourceType.TRANSCRIPT, null, 3, null);
        assertThatThrownBy(() -> service.generateQuiz(TEACHER, request))
            .isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        verify(aiClient, never()).generate(anyString());
    }

    @Test
    void documentSourceRequiresDocumentId() {
        GenerationRequest request = new GenerationRequest(LESSON, SourceType.DOCUMENT, null, 3, Difficulty.EASY);
        assertThatThrownBy(() -> service.generateQuiz(TEACHER, request))
            .isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getDetails().get(0).getField()).isEqualTo("document_id"));
    }

    @Test
    void notOwnerNeverReachesAi() {
        doThrow(new ApiException(ErrorCode.FORBIDDEN)).when(access).assertCanManageLesson(LESSON, TEACHER);
        assertThatThrownBy(() -> service.generateQuiz(TEACHER, quizRequest(3)))
            .isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        verify(aiClient, never()).generate(anyString());
        verify(repository, never()).save(any());
    }

    @Test
    void providerTimeoutIsRecordedAsFailed() {
        AIClient failing = mock(AIClient.class);
        when(failing.provider()).thenReturn("openai-compatible");
        when(failing.model()).thenReturn("m");
        when(failing.generate(anyString()))
            .thenThrow(new AIClientException(AIClientException.Reason.TIMEOUT, "timeout"));
        aiClient = failing;
        service = newService(20_000);

        assertThatThrownBy(() -> service.generateQuiz(TEACHER, quizRequest(3)))
            .isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AI_TIMEOUT));
        AIGeneration g = savedGeneration();
        assertThat(g.getStatus()).isEqualTo(GenerationStatus.FAILED);
        assertThat(g.getErrorCode()).isEqualTo("AI_TIMEOUT");
        assertThat(g.getCandidateJson()).isNull();
    }

    @Test
    void invalidOutputIsRecordedAsFailed() {
        AIClient wrongCount = mock(AIClient.class);
        when(wrongCount.provider()).thenReturn("x");
        when(wrongCount.model()).thenReturn("y");
        when(wrongCount.generate(anyString())).thenReturn("{\"questions\":[]}");
        aiClient = wrongCount;
        service = newService(20_000);

        assertThatThrownBy(() -> service.generateQuiz(TEACHER, quizRequest(3)))
            .isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AI_OUTPUT_INVALID));
        assertThat(savedGeneration().getErrorCode()).isEqualTo("AI_OUTPUT_INVALID");
    }

    private AIGeneration generation(long createdBy, long lessonId, GenerationType type, GenerationStatus status) {
        return AIGeneration.builder().id(5L).createdBy(createdBy).lessonId(lessonId)
            .generationType(type).status(status).build();
    }

    @Test
    void validateForSaveAcceptsMatchingSucceededGeneration() {
        AIGeneration g = generation(TEACHER, LESSON, GenerationType.QUIZ, GenerationStatus.SUCCEEDED);
        when(repository.findById(5L)).thenReturn(Optional.of(g));
        assertThat(service.validateForSave(5L, LESSON, TEACHER, GenerationType.QUIZ)).isSameAs(g);
    }

    @Test
    void validateForSaveRejectsMismatches() {
        when(repository.findById(5L)).thenReturn(Optional.of(
            generation(TEACHER, LESSON, GenerationType.QUIZ, GenerationStatus.SUCCEEDED)));
        assertThatThrownBy(() -> service.validateForSave(5L, LESSON, 3L, GenerationType.QUIZ))
            .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        assertThatThrownBy(() -> service.validateForSave(5L, 11L, TEACHER, GenerationType.QUIZ))
            .hasMessageContaining("Invalid data");
        assertThatThrownBy(() -> service.validateForSave(5L, LESSON, TEACHER, GenerationType.FLASHCARD))
            .isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getDetails().get(0).getMessage()).contains("FLASHCARD"));

        when(repository.findById(6L)).thenReturn(Optional.of(
            generation(TEACHER, LESSON, GenerationType.QUIZ, GenerationStatus.FAILED)));
        assertThatThrownBy(() -> service.validateForSave(6L, LESSON, TEACHER, GenerationType.QUIZ))
            .isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getDetails().get(0).getMessage()).contains("did not succeed"));

        when(repository.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.validateForSave(7L, LESSON, TEACHER, GenerationType.QUIZ))
            .isInstanceOf(ApiException.class);
    }
}
