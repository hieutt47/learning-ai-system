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
import vn.edu.learningai.common.enums.QuizStatus;
import vn.edu.learningai.dto.request.CreateQuizRequest;
import vn.edu.learningai.dto.request.UpdateQuizRequest;
import vn.edu.learningai.dto.response.PageResponse;
import vn.edu.learningai.dto.response.QuizAuthorResponse;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.exception.GlobalExceptionHandler;
import vn.edu.learningai.exception.QuizExceptionHandler;
import vn.edu.learningai.service.QuizService;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class QuizControllerTest {
    private QuizService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(QuizService.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        var mapper = JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();
        mvc = MockMvcBuilders.standaloneSetup(new QuizController(service))
            .setControllerAdvice(new QuizExceptionHandler(), new GlobalExceptionHandler(mapper))
            .setMessageConverters(new JacksonJsonHttpMessageConverter(mapper)).build();
    }

    @Test
    void createReturns201LocationAndEmptyDraft() throws Exception {
        when(service.create(eq(10L), any())).thenReturn(new QuizAuthorResponse("30", "10", "Java",
            QuizStatus.DRAFT, Instant.parse("2026-11-09T00:00:00Z"), List.of()));
        mvc.perform(post("/api/v1/lessons/10/quizzes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Java\",\"generation_id\":\"9223372036854775807\"}"))
            .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/v1/quizzes/30"))
            .andExpect(jsonPath("$.lesson_id").value("10"))
            .andExpect(jsonPath("$.created_at").value("2026-11-09T00:00:00Z"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.questions").isEmpty());
        verify(service).create(10L, new CreateQuizRequest("Java", Long.MAX_VALUE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "[]", "{}", "{", "{\"title\":42}",
        "{\"title\":\"Java\",\"generation_id\":123}", "{\"title\":\"Java\",\"status\":\"PUBLISHED\"}"})
    void malformedOrInvalidBodyReturns400WithoutCallingService(String body) throws Exception {
        mvc.perform(post("/api/v1/lessons/10/quizzes").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "01", "+1", "1.0", "abc", "9223372036854775808"})
    void invalidQuizIdReturns400(String id) throws Exception {
        mvc.perform(get("/api/v1/quizzes/{quiz_id}", id))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(service);
    }

    @Test
    void defaultPaginationUsesContractAndSnakeCase() throws Exception {
        when(service.list(10L, 1, 20)).thenReturn(new PageResponse<>(List.of(), 1, 20, 0L));
        mvc.perform(get("/api/v1/lessons/10/quizzes"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page_size").value(20))
            .andExpect(jsonPath("$.total").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "1.5", "abc", "2147483648"})
    void invalidPageReturns400(String page) throws Exception {
        mvc.perform(get("/api/v1/lessons/10/quizzes").param("page", page))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void patchReturnsUpdatedAuthorView() throws Exception {
        when(service.update(eq(30L), any())).thenReturn(new QuizAuthorResponse("30", "10", "Updated",
            QuizStatus.DRAFT, Instant.parse("2026-11-09T00:00:00Z"), List.of()));
        mvc.perform(patch("/api/v1/quizzes/30").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Updated\",\"generation_id\":null}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Updated"));
        verify(service).update(30L, new UpdateQuizRequest("Updated", null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "[]", "{\"title\":null}", "{\"status\":\"PUBLISHED\"}",
        "{\"generation_id\":123}"})
    void invalidPatchReturns400(String body) throws Exception {
        mvc.perform(patch("/api/v1/quizzes/30").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(service);
    }

    @Test
    void deleteReturns204WithoutBody() throws Exception {
        mvc.perform(delete("/api/v1/quizzes/30")).andExpect(status().isNoContent())
            .andExpect(content().string(""));
        verify(service).delete(30L);
    }

    @Test
    void deleteWithHistoryReturns409() throws Exception {
        doThrow(new ApiException(ErrorCode.RESOURCE_IN_USE)).when(service).delete(30L);
        mvc.perform(delete("/api/v1/quizzes/30")).andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("RESOURCE_IN_USE"))
            .andExpect(jsonPath("$.details").isArray()).andExpect(jsonPath("$.request_id").isNotEmpty());
    }

    @Test
    void invalidDeleteIdDoesNotCallService() throws Exception {
        mvc.perform(delete("/api/v1/quizzes/0")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    private String questionBody() {
        return """
            {"content":"Question","explanation":null,"order_index":4,"options":[
              {"content":"A","is_correct":true,"order_index":0},
              {"content":"B","is_correct":false,"order_index":1}]}
            """;
    }

    @Test
    void addQuestionReturns201AndLocation() throws Exception {
        when(service.addQuestion(eq(30L), any())).thenReturn(new QuizAuthorResponse.QuestionAuthor(
            "50", "Question", null, 4, false, List.of()));
        mvc.perform(post("/api/v1/quizzes/30/questions").contentType(MediaType.APPLICATION_JSON).content(questionBody()))
            .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/v1/questions/50"))
            .andExpect(jsonPath("$.generated_by_ai").value(false));
    }

    @Test
    void replaceQuestionReturns200() throws Exception {
        when(service.replaceQuestion(eq(50L), any())).thenReturn(new QuizAuthorResponse.QuestionAuthor(
            "50", "Question", null, 4, false, List.of()));
        mvc.perform(put("/api/v1/questions/50").contentType(MediaType.APPLICATION_JSON).content(questionBody()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.order_index").value(4));
    }

    @Test
    void deleteQuestionReturns204() throws Exception {
        mvc.perform(delete("/api/v1/questions/50")).andExpect(status().isNoContent());
        verify(service).deleteQuestion(50L);
    }

    @Test
    void duplicateOptionPositionRetains409ThroughJsonBinding() throws Exception {
        mvc.perform(post("/api/v1/quizzes/30/questions").contentType(MediaType.APPLICATION_JSON)
                .content(questionBody().replace("\"order_index\":1", "\"order_index\":0")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_STATE"));
        verifyNoInteractions(service);
    }

    @Test
    void invalidOptionsAreRejectedBeforeService() throws Exception {
        mvc.perform(put("/api/v1/questions/50").contentType(MediaType.APPLICATION_JSON)
                .content(questionBody().replace("\"is_correct\":true", "\"is_correct\":false")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(service);
    }

    @Test
    void publicationReturnsAuthorViewWithNewStatus() throws Exception {
        when(service.changePublication(eq(30L), any())).thenReturn(new QuizAuthorResponse(
            "30", "10", "Java", QuizStatus.PUBLISHED, Instant.parse("2026-11-09T00:00:00Z"), List.of()));
        mvc.perform(patch("/api/v1/quizzes/30/publication").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PUBLISHED\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "{}", "{\"status\":\"DRAFT\"}", "{\"status\":null}",
        "{\"status\":\"CLOSED\",\"closed_at\":null}"})
    void invalidPublicationBodyIs400(String body) throws Exception {
        mvc.perform(patch("/api/v1/quizzes/30/publication").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(service);
    }

    @Test
    void enrollmentErrorKeeps403AndCode() throws Exception {
        when(service.get(30L)).thenThrow(new ApiException(ErrorCode.ENROLLMENT_REQUIRED));
        mvc.perform(get("/api/v1/quizzes/30")).andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ENROLLMENT_REQUIRED"))
            .andExpect(jsonPath("$.request_id").isNotEmpty());
    }
}
