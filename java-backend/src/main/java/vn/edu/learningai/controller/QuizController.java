package vn.edu.learningai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.learningai.dto.request.CreateQuizRequest;
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
