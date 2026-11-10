package vn.edu.learningai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import java.time.Instant;

/**
 * Flashcard của bài học (CARD-01). Vòng đời DRAFT → PUBLISHED → CLOSED; chỉ Draft được sửa/xóa,
 * Published bất biến. Student đã ghi danh chỉ thấy Published.
 */
@Service
@RequiredArgsConstructor
public class FlashcardService {

    private final FlashcardRepository flashcardRepository;
    private final LessonAccessPolicy lessonAccessPolicy;
    private final AiGenerationService aiGenerationService;
    private final FlashcardMapper flashcardMapper;

    @Transactional(readOnly = true)
    public PageResponse<CardResponse> listFlashcards(Long lessonId, Long userId, int page, int pageSize) {
        if (page < 1) {
            throw ApiException.fieldError("page", "page must be >= 1");
        }
        if (pageSize < 1 || pageSize > 100) {
            throw ApiException.fieldError("page_size", "page_size must be between 1 and 100");
        }
        LessonAccessPolicy.LessonViewAccess access = lessonAccessPolicy.assertCanViewLesson(lessonId, userId);

        PageRequest pageable = PageRequest.of(page - 1, pageSize,
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<Flashcard> result = access == LessonAccessPolicy.LessonViewAccess.MANAGER
            ? flashcardRepository.findByLessonId(lessonId, pageable)
            : flashcardRepository.findByLessonIdAndStatus(lessonId, FlashcardStatus.PUBLISHED, pageable);

        return PageResponse.<CardResponse>builder()
            .items(result.getContent().stream().map(flashcardMapper::toCardResponse).toList())
            .page(page)
            .pageSize(pageSize)
            .total(result.getTotalElements())
            .build();
    }

    /** Tạo flashcard Draft; có generation_id hợp lệ thì generated_by_ai = true (server suy ra). */
    @Transactional
    public CardResponse saveDraft(Long lessonId, Long teacherId, CardWriteRequest request) {
        lessonAccessPolicy.assertCanManageLesson(lessonId, teacherId);
        boolean generatedByAi = false;
        if (request.generationId() != null) {
            aiGenerationService.validateForSave(request.generationId(), lessonId, teacherId, GenerationType.FLASHCARD);
            generatedByAi = true;
        }
        Flashcard card = Flashcard.builder()
            .lessonId(lessonId)
            .frontText(request.frontText().strip())
            .backText(request.backText().strip())
            .status(FlashcardStatus.DRAFT)
            .generatedByAi(generatedByAi)
            .generationId(request.generationId())
            .createdBy(teacherId)
            .build();
        return flashcardMapper.toCardResponse(flashcardRepository.save(card));
    }

    @Transactional
    public CardResponse updateDraft(Long cardId, Long teacherId, CardUpdateRequest request) {
        Flashcard card = loadManagedDraft(cardId, teacherId);
        if (request.frontText() != null) {
            card.setFrontText(request.frontText().strip());
        }
        if (request.backText() != null) {
            card.setBackText(request.backText().strip());
        }
        return flashcardMapper.toCardResponse(flashcardRepository.save(card));
    }

    @Transactional
    public void deleteDraft(Long cardId, Long teacherId) {
        flashcardRepository.delete(loadManagedDraft(cardId, teacherId));
    }

    /** PATCH publication: PUBLISHED gọi publish, CLOSED gọi close. */
    @Transactional
    public CardResponse changeStatus(Long cardId, Long teacherId, FlashcardStatus target) {
        return switch (target) {
            case PUBLISHED -> publish(cardId, teacherId);
            case CLOSED -> close(cardId, teacherId);
            case DRAFT -> throw ApiException.fieldError("status", "status must be PUBLISHED or CLOSED");
        };
    }

    /** DRAFT → PUBLISHED sau khi kiểm tra hai mặt không rỗng. Publish lặp lại trả dữ liệu cũ. */
    @Transactional
    public CardResponse publish(Long cardId, Long teacherId) {
        Flashcard card = loadManaged(cardId, teacherId);
        if (card.getStatus() == FlashcardStatus.PUBLISHED) {
            return flashcardMapper.toCardResponse(card);
        }
        if (card.getStatus() != FlashcardStatus.DRAFT) {
            throw new ApiException(ErrorCode.INVALID_STATE, "Only a draft flashcard can be published");
        }
        if (card.getFrontText().isBlank() || card.getBackText().isBlank()) {
            throw new ApiException(ErrorCode.INVALID_STATE, "Front and back text must not be empty");
        }
        card.setStatus(FlashcardStatus.PUBLISHED);
        card.setPublishedAt(Instant.now());
        return flashcardMapper.toCardResponse(flashcardRepository.save(card));
    }

    /** PUBLISHED → CLOSED: student không còn thấy. Close lặp lại trả dữ liệu cũ. */
    @Transactional
    public CardResponse close(Long cardId, Long teacherId) {
        Flashcard card = loadManaged(cardId, teacherId);
        if (card.getStatus() == FlashcardStatus.CLOSED) {
            return flashcardMapper.toCardResponse(card);
        }
        if (card.getStatus() != FlashcardStatus.PUBLISHED) {
            throw new ApiException(ErrorCode.INVALID_STATE, "Only a published flashcard can be closed");
        }
        card.setStatus(FlashcardStatus.CLOSED);
        card.setClosedAt(Instant.now());
        return flashcardMapper.toCardResponse(flashcardRepository.save(card));
    }

    private Flashcard loadManaged(Long cardId, Long teacherId) {
        Flashcard card = flashcardRepository.findById(cardId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Flashcard not found"));
        lessonAccessPolicy.assertCanManageLesson(card.getLessonId(), teacherId);
        return card;
    }

    private Flashcard loadManagedDraft(Long cardId, Long teacherId) {
        Flashcard card = loadManaged(cardId, teacherId);
        if (card.getStatus() != FlashcardStatus.DRAFT) {
            throw new ApiException(ErrorCode.INVALID_STATE, "Only a draft flashcard can be changed");
        }
        return card;
    }
}
