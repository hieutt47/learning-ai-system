package vn.edu.learningai.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.learningai.common.enums.Difficulty;
import vn.edu.learningai.common.enums.GenerationStatus;
import vn.edu.learningai.common.enums.GenerationType;
import vn.edu.learningai.common.enums.SourceType;
import vn.edu.learningai.config.RequestIdFilter;
import vn.edu.learningai.config.SecurityConfig;
import vn.edu.learningai.dto.response.AdminAiGenerationItemResponse;
import vn.edu.learningai.dto.response.PageResponse;
import vn.edu.learningai.exception.GlobalExceptionHandler;
import vn.edu.learningai.repository.UserRepository;
import vn.edu.learningai.security.custom.CustomAccessDeniedHandler;
import vn.edu.learningai.security.custom.CustomAuthenticationEntryPoint;
import vn.edu.learningai.security.custom.CustomUserDetailsService;
import vn.edu.learningai.security.jwt.JwtAuthenticationFilter;
import vn.edu.learningai.security.jwt.JwtService;
import vn.edu.learningai.service.AdminAiService;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(properties = {
    "spring.jackson.property-naming-strategy=SNAKE_CASE",
    "jwt.jwt-signing-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@ContextConfiguration(classes = {
    AdminAiController.class, SecurityConfig.class, JwtAuthenticationFilter.class,
    CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class,
    GlobalExceptionHandler.class, RequestIdFilter.class,
    AdminAiControllerTest.MockConfig.class
})
class AdminAiControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private AdminAiService adminAiService;

    @BeforeEach
    void setUp() {
        reset(adminAiService);
    }

    @Test
    void anonymousUserCannotReadLessonAiGenerations() throws Exception {
        mvc.perform(get("/api/v1/lessons/10/ai-generations"))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(adminAiService);
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentIsForbiddenFromLessonAiGenerations() throws Exception {
        mvc.perform(get("/api/v1/lessons/10/ai-generations"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(adminAiService);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCanRequestLessonAiGenerationsWithFilters() throws Exception {
        when(adminAiService.listLessonAiGenerations(10L, GenerationType.QUIZ, GenerationStatus.SUCCEEDED, 2, 10))
            .thenReturn(PageResponse.<AdminAiGenerationItemResponse>builder()
                .items(List.of()).page(2).pageSize(10).total(0L).build());

        mvc.perform(get("/api/v1/lessons/10/ai-generations")
                .param("generation_type", "QUIZ")
                .param("status", "SUCCEEDED")
                .param("page", "2")
                .param("page_size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(2))
            .andExpect(jsonPath("$.page_size").value(10));

        verify(adminAiService).listLessonAiGenerations(10L, GenerationType.QUIZ, GenerationStatus.SUCCEEDED, 2, 10);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanListLessonAiGenerationsWithSnakeCaseJson() throws Exception {
        var item = new AdminAiGenerationItemResponse(
            "7", "10", "Bai 1", "3", "Khoa Java",
            null, null, SourceType.TRANSCRIPT, GenerationType.FLASHCARD,
            5, Difficulty.MEDIUM, "mock", "mock-model", "flashcard-v2",
            GenerationStatus.SUCCEEDED, "{\"flashcards\":[]}", null,
            "2", "Demo Teacher", Instant.parse("2026-11-25T10:00:00Z")
        );
        when(adminAiService.listLessonAiGenerations(10L, null, null, 1, 20))
            .thenReturn(PageResponse.<AdminAiGenerationItemResponse>builder()
                .items(List.of(item)).page(1).pageSize(20).total(1L).build());

        mvc.perform(get("/api/v1/lessons/10/ai-generations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].id").value("7"))
            .andExpect(jsonPath("$.items[0].course_title").value("Khoa Java"))
            .andExpect(jsonPath("$.items[0].lesson_title").value("Bai 1"))
            .andExpect(jsonPath("$.items[0].generation_type").value("FLASHCARD"))
            .andExpect(jsonPath("$.items[0].creator_name").value("Demo Teacher"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MockConfig {
        @Bean
        UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        AdminAiService adminAiService() {
            return mock(AdminAiService.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        }

        @Bean
        CustomUserDetailsService userDetailsService() {
            return mock(CustomUserDetailsService.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        }

        @Bean
        JwtService jwtService() {
            return mock(JwtService.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        }
    }
}
