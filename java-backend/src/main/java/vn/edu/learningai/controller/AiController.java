package vn.edu.learningai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.learningai.dto.request.GenerationRequest;
import vn.edu.learningai.dto.response.CardCandidateResponse;
import vn.edu.learningai.dto.response.QuizCandidateResponse;
import vn.edu.learningai.security.CurrentUserProvider;
import vn.edu.learningai.service.AiGenerationService;

/** AI-01: sinh preview quiz/flashcard. Chỉ owner của lesson hoặc admin; không tạo nội dung. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiGenerationService aiGenerationService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/generate-quiz")
    public QuizCandidateResponse generateQuiz(@Valid @RequestBody GenerationRequest request) {
        return aiGenerationService.generateQuiz(currentUserProvider.get().userId(), request);
    }

    @PostMapping("/generate-flashcards")
    public CardCandidateResponse generateFlashcards(@Valid @RequestBody GenerationRequest request) {
        return aiGenerationService.generateFlashcards(currentUserProvider.get().userId(), request);
    }
}
