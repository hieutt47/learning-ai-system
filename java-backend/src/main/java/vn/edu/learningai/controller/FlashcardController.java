package vn.edu.learningai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.learningai.dto.request.CardUpdateRequest;
import vn.edu.learningai.dto.request.CardWriteRequest;
import vn.edu.learningai.dto.request.StatusRequest;
import vn.edu.learningai.dto.response.CardResponse;
import vn.edu.learningai.dto.response.PageResponse;
import vn.edu.learningai.security.CurrentUserProvider;
import vn.edu.learningai.service.FlashcardService;

import java.net.URI;

/** CARD-01: flashcard của bài học. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class FlashcardController {

    private final FlashcardService flashcardService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/lessons/{lessonId}/flashcards")
    public PageResponse<CardResponse> listFlashcards(@PathVariable Long lessonId,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return flashcardService.listFlashcards(lessonId, currentUserProvider.get().userId(), page, pageSize);
    }

    @PostMapping("/lessons/{lessonId}/flashcards")
    public ResponseEntity<CardResponse> createFlashcard(@PathVariable Long lessonId,
                                                        @Valid @RequestBody CardWriteRequest request) {
        CardResponse card = flashcardService.saveDraft(lessonId, currentUserProvider.get().userId(), request);
        return ResponseEntity.created(URI.create("/api/v1/flashcards/" + card.id())).body(card);
    }

    @PatchMapping("/flashcards/{cardId}")
    public CardResponse updateFlashcard(@PathVariable Long cardId, @Valid @RequestBody CardUpdateRequest request) {
        return flashcardService.updateDraft(cardId, currentUserProvider.get().userId(), request);
    }

    @DeleteMapping("/flashcards/{cardId}")
    public ResponseEntity<Void> deleteFlashcard(@PathVariable Long cardId) {
        flashcardService.deleteDraft(cardId, currentUserProvider.get().userId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/flashcards/{cardId}/publication")
    public CardResponse publishFlashcard(@PathVariable Long cardId, @Valid @RequestBody StatusRequest request) {
        return flashcardService.changeStatus(cardId, currentUserProvider.get().userId(), request.status());
    }
}
