package vn.edu.learningai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.learningai.dto.request.CreateQuizRequest;
import vn.edu.learningai.dto.request.UpdateQuizRequest;
import vn.edu.learningai.dto.request.WriteQuestionRequest;
import vn.edu.learningai.dto.request.QuizPublicationRequest;
import vn.edu.learningai.dto.response.PageResponse;
import vn.edu.learningai.dto.response.QuizAuthorResponse;
import vn.edu.learningai.dto.response.QuizSummaryResponse;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.service.QuizService;

import java.net.URI;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class QuizController {
    private final QuizService quizService;

    @PostMapping("/lessons/{lesson_id}/quizzes")
    public ResponseEntity<QuizAuthorResponse> create(
        @PathVariable("lesson_id") String lessonId, @RequestBody CreateQuizRequest request) {
        var quiz = quizService.create(positiveLong(lessonId, "lesson_id"), request);
        return ResponseEntity.created(URI.create("/api/v1/quizzes/" + quiz.id())).body(quiz);
    }

    @GetMapping("/lessons/{lesson_id}/quizzes")
    public PageResponse<QuizSummaryResponse> list(
        @PathVariable("lesson_id") String lessonId,
        @RequestParam(name = "page", defaultValue = "1") String page,
        @RequestParam(name = "page_size", defaultValue = "20") String pageSize) {
        return quizService.list(positiveLong(lessonId, "lesson_id"),
            positiveInteger(page, "page"), positiveInteger(pageSize, "page_size"));
    }

    @GetMapping("/quizzes/{quiz_id}")
    public Object get(@PathVariable("quiz_id") String quizId) {
        return quizService.get(positiveLong(quizId, "quiz_id"));
    }

    @PatchMapping("/quizzes/{quiz_id}")
    public QuizAuthorResponse update(@PathVariable("quiz_id") String quizId,
                                     @RequestBody UpdateQuizRequest request) {
        return quizService.update(positiveLong(quizId, "quiz_id"), request);
    }

    @DeleteMapping("/quizzes/{quiz_id}")
    public ResponseEntity<Void> delete(@PathVariable("quiz_id") String quizId) {
        quizService.delete(positiveLong(quizId, "quiz_id"));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/quizzes/{quiz_id}/questions")
    public ResponseEntity<QuizAuthorResponse.QuestionAuthor> addQuestion(
        @PathVariable("quiz_id") String quizId, @RequestBody WriteQuestionRequest request) {
        var question = quizService.addQuestion(positiveLong(quizId, "quiz_id"), request);
        return ResponseEntity.created(URI.create("/api/v1/questions/" + question.id())).body(question);
    }

    @PutMapping("/questions/{question_id}")
    public QuizAuthorResponse.QuestionAuthor replaceQuestion(
        @PathVariable("question_id") String questionId, @RequestBody WriteQuestionRequest request) {
        return quizService.replaceQuestion(positiveLong(questionId, "question_id"), request);
    }

    @DeleteMapping("/questions/{question_id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable("question_id") String questionId) {
        quizService.deleteQuestion(positiveLong(questionId, "question_id"));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/quizzes/{quiz_id}/publication")
    public QuizAuthorResponse changePublication(@PathVariable("quiz_id") String quizId,
                                                @RequestBody QuizPublicationRequest request) {
        return quizService.changePublication(positiveLong(quizId, "quiz_id"), request);
    }

    private static long positiveLong(String value, String field) {
        if (!value.matches("[1-9][0-9]{0,18}")) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, field + " must be a positive BIGINT");
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, field + " exceeds the signed BIGINT range");
        }
    }

    private static int positiveInteger(String value, String field) {
        long number = positiveLong(value, field);
        if (number > Integer.MAX_VALUE) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, field + " exceeds the integer range");
        }
        return (int) number;
    }
}
