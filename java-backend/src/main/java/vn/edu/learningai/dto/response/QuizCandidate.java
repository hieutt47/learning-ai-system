package vn.edu.learningai.dto.response;

import java.util.List;

/** Candidate quiz: {"questions":[...]} — cũng là nội dung lưu vào ai_generations.candidate_json. */
public record QuizCandidate(List<QuestionCandidate> questions) {
}
