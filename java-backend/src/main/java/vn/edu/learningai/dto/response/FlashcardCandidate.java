package vn.edu.learningai.dto.response;

import java.util.List;

/** Candidate flashcard: {"flashcards":[...]} — cũng là nội dung lưu vào ai_generations.candidate_json. */
public record FlashcardCandidate(List<CardCandidate> flashcards) {
}
