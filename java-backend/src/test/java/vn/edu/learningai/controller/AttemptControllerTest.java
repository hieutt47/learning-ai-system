package vn.edu.learningai.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockMakers;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.learningai.common.enums.AttemptStatus;
import vn.edu.learningai.dto.response.AttemptResponse;
import vn.edu.learningai.dto.response.AttemptResultResponse;
import vn.edu.learningai.exception.GlobalExceptionHandler;
import vn.edu.learningai.exception.QuizExceptionHandler;
import vn.edu.learningai.service.AttemptService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AttemptControllerTest {
    private AttemptService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(AttemptService.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        var mapper = JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();
        mvc = MockMvcBuilders.standaloneSetup(new AttemptController(service))
            .setControllerAdvice(new QuizExceptionHandler(), new GlobalExceptionHandler(mapper))
            .setMessageConverters(new JacksonJsonHttpMessageConverter(mapper)).build();
    }

    @Test
    void historyEndpointExists() throws Exception {
        var item = new vn.edu.learningai.dto.response.AttemptSummaryResponse(
            "50", "30", "Quiz", "20", "Course", AttemptStatus.IN_PROGRESS, null,
            Instant.parse("2026-11-14T00:00:00Z"), null);
        when(service.history(1, 20, null, null)).thenReturn(
            new vn.edu.learningai.dto.response.PageResponse<>(List.of(item), 1, 20, 1L));
        mvc.perform(get("/api/v1/users/me/attempts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.page_size").value(20))
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items[0].id").value("50"))
            .andExpect(jsonPath("$.items[0].quiz_title").value("Quiz"))
            .andExpect(jsonPath("$.items[0].started_at").value("2026-11-14T00:00:00Z"))
            .andExpect(jsonPath("$.items[0].score").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.items[0].review").doesNotExist())
            .andExpect(jsonPath("$.items[0].correct_option_id").doesNotExist())
            .andExpect(jsonPath("$.items[0].explanation").doesNotExist());
    }

    @Test
    void historyPassesPaginationAndBothFilters() throws Exception {
        when(service.history(2, 10, 30L, 20L)).thenReturn(
            new vn.edu.learningai.dto.response.PageResponse<>(List.of(), 2, 10, 0L));
        mvc.perform(get("/api/v1/users/me/attempts?page=2&page_size=10&quiz_id=30&course_id=20"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
        verify(service).history(2, 10, 30L, 20L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "01", "abc", "9223372036854775808", ""})
    void historyRejectsInvalidFilterIds(String id) throws Exception {
        for (String filter : List.of("quiz_id", "course_id")) {
            mvc.perform(get("/api/v1/users/me/attempts").param(filter, id))
                .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    void historyRejectsNonNumericPagination() throws Exception {
        mvc.perform(get("/api/v1/users/me/attempts?page=abc"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void newStartReturns201AndLocation() throws Exception {
        var attempt = new AttemptResponse("50", "30", "4", AttemptStatus.IN_PROGRESS, 3,
            Instant.parse("2026-11-14T00:00:00Z"));
        when(service.start(30L)).thenReturn(new AttemptService.StartResult(attempt, true));

        mvc.perform(post("/api/v1/quizzes/30/attempts"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/attempts/50"))
            .andExpect(jsonPath("$.id").value("50"))
            .andExpect(jsonPath("$.total_questions").value(3))
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void repeatedStartReturns200WithoutLocation() throws Exception {
        var attempt = new AttemptResponse("50", "30", "4", AttemptStatus.IN_PROGRESS, 3,
            Instant.parse("2026-11-14T00:00:00Z"));
        when(service.start(30L)).thenReturn(new AttemptService.StartResult(attempt, false));

        mvc.perform(post("/api/v1/quizzes/30/attempts"))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist("Location"))
            .andExpect(jsonPath("$.id").value("50"));
    }

    @Test
    void submitReturnsServerResultAndReview() throws Exception {
        var review = new AttemptResultResponse.Review("101", "Question", "1001", true,
            "1001", "Explanation", List.of(
                new AttemptResultResponse.Option("1001", "A"),
                new AttemptResultResponse.Option("1002", "B")));
        var result = new AttemptResultResponse("50", "30", "Quiz", "4", AttemptStatus.SUBMITTED,
            new BigDecimal("66.67"), 3, 2, Instant.parse("2026-11-14T00:00:00Z"),
            Instant.parse("2026-11-14T00:05:00Z"), List.of(review));
        when(service.submit(eq(50L), any())).thenReturn(result);

        mvc.perform(post("/api/v1/attempts/50/submit").contentType(MediaType.APPLICATION_JSON)
                .content("{\"answers\":[{\"question_id\":\"101\",\"selected_option_id\":\"1001\"}]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.score").value(66.67))
            .andExpect(jsonPath("$.correct_count").value(2))
            .andExpect(jsonPath("$.review[0].correct_option_id").value("1001"))
            .andExpect(jsonPath("$.review[0].explanation").value("Explanation"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{}", "[]", "{\"answers\":[],\"score\":100}"})
    void malformedSubmitReturns400WithoutCallingService(String body) throws Exception {
        mvc.perform(post("/api/v1/attempts/50/submit").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(service);
    }

    @Test
    void submitExtraFieldPreservesValidationMessage() throws Exception {
        mvc.perform(post("/api/v1/attempts/50/submit").contentType(MediaType.APPLICATION_JSON)
                .content("{\"answers\":[],\"score\":100}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.message").value("Only answers is allowed and required"));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "01", "abc", "9223372036854775808"})
    void invalidIdsReturn400(String id) throws Exception {
        mvc.perform(post("/api/v1/quizzes/{quiz_id}/attempts", id))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(service);
    }
}
