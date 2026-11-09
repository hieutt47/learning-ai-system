package vn.edu.learningai.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import vn.edu.learningai.support.IntegrationTestBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CourseControllerTest extends IntegrationTestBase {
    private static final String COURSES = "/api/v1/courses";
    @Test
    void teacherCreatesDraftCourse() throws Exception {
        mockMvc.perform(post(COURSES)
                .header("Authorization", "Bearer " + tokenFor(2))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Khoa moi","description":"Mo ta","category_id":"1","price":150000.00}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Khoa moi"))
            .andExpect(jsonPath("$.is_published").value(false))
            .andExpect(jsonPath("$.teacher_id").value("2"))
            .andExpect(jsonPath("$.category_id").value("1"))
            .andExpect(jsonPath("$.price").value("150000.00"));
    }
    @Test
    void studentCannotCreateCourse() throws Exception {
        mockMvc.perform(post(COURSES)
                .header("Authorization", "Bearer " + tokenFor(4))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Khoa moi","description":"Mo ta","category_id":"1","price":0}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
    @Test
    void missingIdentityIsUnauthenticated() throws Exception {
        mockMvc.perform(post(COURSES)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Khoa moi","description":"Mo ta","category_id":"1","price":0}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
    @Test
    void inactiveAccountCannotLogInAndSoCannotCallAnything() throws Exception {

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"inactive@test.local","password":"DemoPass123!"}"""))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("ACCOUNT_INACTIVE"));
    }
    @Test
    void unknownCategoryIsNotFound() throws Exception {
        mockMvc.perform(post(COURSES)
                .header("Authorization", "Bearer " + tokenFor(2))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Khoa moi","description":"Mo ta","category_id":"999","price":0}
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
    @Test
    void invalidPayloadIsValidationError() throws Exception {
        mockMvc.perform(post(COURSES)
                .header("Authorization", "Bearer " + tokenFor(2))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"","description":"Mo ta","category_id":"1","price":-5}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.details").isArray())
            .andExpect(jsonPath("$.request_id").exists());
    }
    @Test
    void patchAppliesOnlyFieldsSent() throws Exception {
        mockMvc.perform(patch(COURSES + "/1")
                .header("Authorization", "Bearer " + tokenFor(2))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"price":42000.50}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Khoa draft cua A"))
            .andExpect(jsonPath("$.price").value("42000.50"));
    }
    @Test
    void otherTeacherCannotPatchCourse() throws Exception {
        mockMvc.perform(patch(COURSES + "/1")
                .header("Authorization", "Bearer " + tokenFor(3))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Chiem khoa"}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
    @Test
    void ownerResponseCarriesLessonAndStudentCounts() throws Exception {

        mockMvc.perform(get("/api/v1/teaching/courses/2").header("Authorization", "Bearer " + tokenFor(2)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lesson_count").value(2))
            .andExpect(jsonPath("$.total_students").value(1));
    }
    @Test
    void courseListIncludesCountsForEveryRow() throws Exception {

        String body = mockMvc.perform(get("/api/v1/teaching/courses").header("Authorization", "Bearer " + tokenFor(2)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        java.util.List<java.util.Map<String, Object>> items =
            com.jayway.jsonpath.JsonPath.read(body, "$.items");
        java.util.Map<String, Object> course1 = items.stream()
            .filter(item -> "1".equals(item.get("id"))).findFirst().orElseThrow();
        java.util.Map<String, Object> course2 = items.stream()
            .filter(item -> "2".equals(item.get("id"))).findFirst().orElseThrow();
        org.assertj.core.api.Assertions.assertThat(course1.get("lesson_count")).isEqualTo(0);
        org.assertj.core.api.Assertions.assertThat(course1.get("total_students")).isEqualTo(0);
        org.assertj.core.api.Assertions.assertThat(course2.get("lesson_count")).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(course2.get("total_students")).isEqualTo(1);
    }
    @Test
    void teacherListsOnlyOwnCourses() throws Exception {
        mockMvc.perform(get("/api/v1/teaching/courses").header("Authorization", "Bearer " + tokenFor(2)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.page_size").value(20));
    }
    @Test
    void adminListsAllCourses() throws Exception {
        mockMvc.perform(get("/api/v1/teaching/courses").header("Authorization", "Bearer " + tokenFor(1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(5));
    }
    @Test
    void invalidPaginationIsValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/teaching/courses")
                .header("Authorization", "Bearer " + tokenFor(2))
                .param("page", "0"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
    @Test
    void nonNumericPaginationIsValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/teaching/courses")
                .header("Authorization", "Bearer " + tokenFor(2))
                .param("page", "abc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.request_id").exists());
    }
    @Test
    void deleteDraftCourseSucceeds() throws Exception {
        mockMvc.perform(delete(COURSES + "/1").header("Authorization", "Bearer " + tokenFor(2)))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/teaching/courses/1").header("Authorization", "Bearer " + tokenFor(2)))
            .andExpect(status().isNotFound());
    }
    @Test
    void deletePublishedCourseIsInvalidState() throws Exception {
        mockMvc.perform(delete(COURSES + "/2").header("Authorization", "Bearer " + tokenFor(2)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("INVALID_STATE"));
    }
}
