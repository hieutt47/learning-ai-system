package vn.edu.learningai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.learningai.dto.response.CourseStatisticsResponse;
import vn.edu.learningai.dto.response.QuizStatisticsResponse;
import vn.edu.learningai.dto.response.SystemStatisticsResponse;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.service.StatisticsService;

@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    private final StatisticsService service;

    @GetMapping("/courses/{id}")
    public CourseStatisticsResponse course(@PathVariable String id) {
        return service.course(parsePositiveId(id));
    }

    @GetMapping("/quizzes/{id}")
    public QuizStatisticsResponse quiz(@PathVariable String id) {
        return service.quiz(parsePositiveId(id));
    }

    @GetMapping("/system")
    public SystemStatisticsResponse system() {
        return service.system();
    }

    private long parsePositiveId(String value) {
        try {
            if (!value.matches("[1-9][0-9]{0,18}")) {
                throw new NumberFormatException();
            }
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "id must be a positive signed BIGINT");
        }
    }
}
