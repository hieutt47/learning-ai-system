package vn.edu.learningai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.learningai.ai.AIClient;
import vn.edu.learningai.ai.AIClientException;
import vn.edu.learningai.ai.AiCandidateValidator;
import vn.edu.learningai.ai.AiCandidateValidator.InvalidCandidateException;
import vn.edu.learningai.ai.AiPromptBuilder;
import vn.edu.learningai.ai.AiProperties;
import vn.edu.learningai.common.enums.GenerationStatus;
import vn.edu.learningai.common.enums.GenerationType;
import vn.edu.learningai.common.enums.SourceType;
import vn.edu.learningai.dto.request.GenerationRequest;
import vn.edu.learningai.dto.response.CardCandidate;
import vn.edu.learningai.dto.response.CardCandidateResponse;
import vn.edu.learningai.dto.response.FlashcardCandidate;
import vn.edu.learningai.dto.response.QuestionCandidate;
import vn.edu.learningai.dto.response.QuizCandidate;
import vn.edu.learningai.dto.response.QuizCandidateResponse;
import vn.edu.learningai.entity.AIGeneration;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.repository.AiGenerationRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Function;

/**
 * Sinh quiz/flashcard bằng AI (báo cáo 3.3.4, kịch bản 3.4.4). Chỉ trả preview candidate, không tạo quiz/card.
 * Gọi AI ngoài transaction; mỗi lần sinh (thành công hay lỗi) lưu một bản ghi ai_generations để tra vết.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiGenerationService {

    private static final int DEFAULT_MAX_SOURCE_CHARS = 20_000;

    private final LessonAccessPolicy lessonAccessPolicy;
    private final LessonSourceTextProvider lessonSourceTextProvider;
    private final AiPromptBuilder promptBuilder;
    private final AIClient aiClient;
    private final AiCandidateValidator candidateValidator;
    private final AiGenerationRepository aiGenerationRepository;
    private final AiProperties aiProperties;
    private final JsonMapper jsonMapper;

    public QuizCandidateResponse generateQuiz(Long teacherId, GenerationRequest request) {
        if (request.difficulty() == null) {
            throw ApiException.fieldError("difficulty", "difficulty is required for quiz generation");
        }
        Generated<List<QuestionCandidate>> result = generate(teacherId, request, GenerationType.QUIZ,
            AiPromptBuilder.QUIZ_PROMPT_VERSION,
            text -> promptBuilder.buildQuizPrompt(text, request.count(), request.difficulty()),
            raw -> validateQuizCandidate(raw, request.count()),
            QuizCandidate::new);
        return new QuizCandidateResponse(
            String.valueOf(result.generation().getId()),
            String.valueOf(request.lessonId()),
            result.sourceTruncated(),
            new QuizCandidate(result.candidate()));
    }

    public CardCandidateResponse generateFlashcards(Long teacherId, GenerationRequest request) {
        Generated<List<CardCandidate>> result = generate(teacherId, request, GenerationType.FLASHCARD,
            AiPromptBuilder.FLASHCARD_PROMPT_VERSION,
            text -> promptBuilder.buildFlashcardPrompt(text, request.count()),
            raw -> validateCardCandidate(raw, request.count()),
            FlashcardCandidate::new);
        return new CardCandidateResponse(
            String.valueOf(result.generation().getId()),
            String.valueOf(request.lessonId()),
            result.sourceTruncated(),
            new FlashcardCandidate(result.candidate()));
    }

    /**
     * Kiểm tra generation_id trước khi lưu Draft (gọi từ QuizService/FlashcardService): đúng người tạo,
     * đúng bài học, đúng loại và SUCCEEDED. Hợp lệ thì service gọi đặt generated_by_ai = true.
     */
    @Transactional(readOnly = true)
    public AIGeneration validateForSave(Long generationId, Long lessonId, Long teacherId, GenerationType type) {
        AIGeneration generation = aiGenerationRepository.findById(generationId)
            .orElseThrow(() -> ApiException.fieldError("generation_id", "generation not found"));
        if (!generation.getCreatedBy().equals(teacherId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Generation belongs to another user");
        }
        if (!generation.getLessonId().equals(lessonId)) {
            throw ApiException.fieldError("generation_id", "generation belongs to another lesson");
        }
        if (generation.getGenerationType() != type) {
            throw ApiException.fieldError("generation_id", "generation type is not " + type);
        }
        if (generation.getStatus() != GenerationStatus.SUCCEEDED) {
            throw ApiException.fieldError("generation_id", "generation did not succeed");
        }
        return generation;
    }

    private <T> Generated<T> generate(Long teacherId, GenerationRequest request, GenerationType type,
                                      String promptVersion, Function<String, String> promptFactory,
                                      Function<String, T> validator, Function<T, Object> toStoredCandidate) {
        Long documentId = checkSource(request);
        lessonAccessPolicy.assertCanManageLesson(request.lessonId(), teacherId);

        String source = lessonSourceTextProvider.getSourceText(request.lessonId(), request.sourceType(), documentId);
        int limit = aiProperties.maxSourceChars() == null ? DEFAULT_MAX_SOURCE_CHARS : aiProperties.maxSourceChars();
        boolean truncated = source.length() > limit;
        String sentText = truncated ? truncate(source, limit) : source;

        AIGeneration generation = AIGeneration.builder()
            .lessonId(request.lessonId())
            .sourceDocumentId(documentId)
            .sourceType(request.sourceType())
            .generationType(type)
            .requestedCount(request.count())
            .difficulty(type == GenerationType.QUIZ ? request.difficulty() : null)
            .provider(aiClient.provider())
            .model(aiClient.model())
            .promptVersion(promptVersion)
            .sourceHash(sha256(sentText))
            .createdBy(teacherId)
            .build();

        String raw;
        try {
            raw = aiClient.generate(promptFactory.apply(sentText));
        } catch (AIClientException e) {
            ErrorCode code = switch (e.getReason()) {
                case TIMEOUT -> ErrorCode.AI_TIMEOUT;
                case PROVIDER_ERROR -> ErrorCode.AI_PROVIDER_ERROR;
                case INVALID_OUTPUT -> ErrorCode.AI_OUTPUT_INVALID;
            };
            saveFailed(generation, code);
            log.warn("AI generation failed for lesson {}: {} ({})", request.lessonId(), code, e.getMessage());
            throw new ApiException(code);
        }

        T candidate;
        try {
            candidate = validator.apply(raw);
        } catch (InvalidCandidateException e) {
            saveFailed(generation, ErrorCode.AI_OUTPUT_INVALID);
            log.warn("AI output invalid for lesson {}: {}", request.lessonId(), e.getMessage());
            throw new ApiException(ErrorCode.AI_OUTPUT_INVALID);
        }

        generation.setStatus(GenerationStatus.SUCCEEDED);
        generation.setCandidateJson(jsonMapper.writeValueAsString(toStoredCandidate.apply(candidate)));
        return new Generated<>(aiGenerationRepository.save(generation), candidate, truncated);
    }

    private List<QuestionCandidate> validateQuizCandidate(String json, Integer count) {
        return candidateValidator.parseQuizCandidate(json, count);
    }

    private List<CardCandidate> validateCardCandidate(String json, Integer count) {
        return candidateValidator.parseCardCandidate(json, count);
    }

    /** document_id bắt buộc với DOCUMENT, không được gửi với TRANSCRIPT. */
    private static Long checkSource(GenerationRequest request) {
        if (request.sourceType() == SourceType.DOCUMENT && request.documentId() == null) {
            throw ApiException.fieldError("document_id", "document_id is required when source_type is DOCUMENT");
        }
        if (request.sourceType() == SourceType.TRANSCRIPT && request.documentId() != null) {
            throw ApiException.fieldError("document_id", "document_id is only allowed when source_type is DOCUMENT");
        }
        return request.documentId();
    }

    private void saveFailed(AIGeneration generation, ErrorCode code) {
        generation.setStatus(GenerationStatus.FAILED);
        generation.setErrorCode(code.name());
        aiGenerationRepository.save(generation);
    }

    /** Cắt theo số ký tự, không cắt đôi cặp surrogate (emoji, ký tự ngoài BMP). */
    static String truncate(String text, int limit) {
        int end = limit;
        if (end > 0 && Character.isHighSurrogate(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(0, end);
    }

    private static String sha256(String text) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private record Generated<T>(AIGeneration generation, T candidate, boolean sourceTruncated) {
    }
}
