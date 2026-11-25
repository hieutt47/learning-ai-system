package vn.edu.learningai.controller;

import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.learningai.dto.response.QuizAuthorResponse;
import vn.edu.learningai.common.enums.QuizStatus;
import vn.edu.learningai.service.QuizDraftSaveService;
import vn.edu.learningai.exception.GlobalExceptionHandler;
import vn.edu.learningai.exception.QuizExceptionHandler;
import java.time.Instant;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class QuizDraftSaveControllerTest {
    @Test void acceptsAtomicPutAndRejectsUnknownFields() throws Exception {
        var service=mock(QuizDraftSaveService.class,withSettings().mockMaker(MockMakers.SUBCLASS));
        var json=JsonMapper.builder().build();
        var mvc=MockMvcBuilders.standaloneSetup(new QuizDraftSaveController(service))
            .setControllerAdvice(new QuizExceptionHandler(), new GlobalExceptionHandler(json))
            .setMessageConverters(new JacksonJsonHttpMessageConverter(json)).build();
        when(service.save(eq(30L),any())).thenReturn(new QuizAuthorResponse("30","10","Q",QuizStatus.DRAFT,Instant.now(),List.of()));
        String body="{\"request_id\":\"11111111-1111-4111-8111-111111111111\",\"title\":\"Q\",\"questions\":[]}";
        mvc.perform(put("/api/v1/quizzes/30/draft").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value("30"));
        clearInvocations(service);
        mvc.perform(put("/api/v1/quizzes/30/draft").contentType(MediaType.APPLICATION_JSON).content(body.replace("\"title\"","\"score\"")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.message").value("Unknown field: score"));
        verifyNoInteractions(service);
    }

    @Test void preservesDuplicateOptionMessageAndOrderConflictFromJsonCreator() throws Exception {
        var service = mock(QuizDraftSaveService.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        var json = JsonMapper.builder().build();
        var mvc = MockMvcBuilders.standaloneSetup(new QuizDraftSaveController(service))
            .setControllerAdvice(new QuizExceptionHandler(), new GlobalExceptionHandler(json))
            .setMessageConverters(new JacksonJsonHttpMessageConverter(json)).build();

        String duplicateContent = """
            {"request_id":"11111111-1111-4111-8111-111111111111","title":"Q","questions":[
              {"content":"Q1","explanation":null,"order_index":0,"options":[
                {"content":"Hà Nội","is_correct":true,"order_index":0},
                {"content":"  hà   nội ","is_correct":false,"order_index":1}
              ]}
            ]}
            """;
        mvc.perform(put("/api/v1/quizzes/30/draft").contentType(MediaType.APPLICATION_JSON).content(duplicateContent))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.message").value("Option content must be unique after normalization"));

        String duplicateOrder = """
            {"request_id":"11111111-1111-4111-8111-111111111111","title":"Q","questions":[
              {"content":"Q1","explanation":null,"order_index":0,"options":[
                {"content":"A","is_correct":true,"order_index":0},
                {"content":"B","is_correct":false,"order_index":0}
              ]}
            ]}
            """;
        mvc.perform(put("/api/v1/quizzes/30/draft").contentType(MediaType.APPLICATION_JSON).content(duplicateOrder))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("INVALID_STATE"))
            .andExpect(jsonPath("$.message").value("Option order_index must be unique"));
        verifyNoInteractions(service);
    }
}
