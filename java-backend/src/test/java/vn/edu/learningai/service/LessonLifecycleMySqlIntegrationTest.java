package vn.edu.learningai.service;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import vn.edu.learningai.support.IntegrationTestBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LessonLifecycleMySqlIntegrationTest extends IntegrationTestBase {
    @Autowired JdbcClient jdbc;

    @Test
    void lessonInDraftCourseIsReadyForCoursePublication() throws Exception {
        mockMvc.perform(as(post("/api/v1/courses/1/lessons"), TEACHER_A)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Bai dau tien\",\"video_url\":\"\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void teacherCanStagePublishAndArchiveWithoutEmptyingPublishedCourse() throws Exception {
        String created = mockMvc.perform(as(post("/api/v1/courses/2/lessons"), TEACHER_A)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Bai thay the\",\"video_url\":\"\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andReturn().getResponse().getContentAsString();
        long replacementId = ((Number) JsonPath.read(created, "$.id")).longValue();

        mockMvc.perform(as(get("/api/v1/lessons/{id}", replacementId), STUDENT_A))
            .andExpect(status().isNotFound());
        mockMvc.perform(as(get("/api/v1/courses/2/lessons"), STUDENT_A))
            .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(as(patch("/api/v1/lessons/{id}/status", replacementId), TEACHER_A)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PUBLISHED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PUBLISHED"));
        mockMvc.perform(as(get("/api/v1/lessons/{id}", replacementId), STUDENT_A))
            .andExpect(status().isOk());

        mockMvc.perform(as(patch("/api/v1/lessons/1/status"), TEACHER_A)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ARCHIVED\"}"))
            .andExpect(status().isOk());
        mockMvc.perform(as(get("/api/v1/lessons/1"), STUDENT_A)).andExpect(status().isNotFound());
        mockMvc.perform(as(delete("/api/v1/lessons/1"), TEACHER_A)).andExpect(status().isConflict());
        mockMvc.perform(as(get("/api/v1/lessons/1/documents"), STUDENT_A)).andExpect(status().isNotFound());
        mockMvc.perform(as(get("/api/v1/lessons/1/flashcards"), STUDENT_A)).andExpect(status().isNotFound());
        mockMvc.perform(as(get("/api/v1/courses/2/lessons"), STUDENT_A))
            .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(as(get("/api/v1/courses/2/lessons"), TEACHER_A))
            .andExpect(jsonPath("$.length()").value(3));
        mockMvc.perform(as(get("/api/v1/courses/2"), STUDENT_A))
            .andExpect(jsonPath("$.outline.length()").value(2));
    }

    @Test
    void lastPublishedLessonCannotBeArchivedOrDeleted() throws Exception {
        mockMvc.perform(as(patch("/api/v1/lessons/1/status"), TEACHER_A)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ARCHIVED\"}"))
            .andExpect(status().isOk());
        mockMvc.perform(as(patch("/api/v1/lessons/2/status"), TEACHER_A)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ARCHIVED\"}"))
            .andExpect(status().isConflict());
        mockMvc.perform(as(delete("/api/v1/lessons/2"), TEACHER_A))
            .andExpect(status().isConflict());
        mockMvc.perform(as(get("/api/v1/lessons/2"), STUDENT_A)).andExpect(status().isOk());
    }

    @Test
    void archivedLessonRejectsNewQuizAccessButKeepsItsRow() throws Exception {
        jdbc.sql("INSERT INTO quizzes (id, lesson_id, title, status, created_by) VALUES (99, 1, 'Quiz cu', 'PUBLISHED', 2)")
            .update();
        mockMvc.perform(as(patch("/api/v1/lessons/1/status"), TEACHER_A)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ARCHIVED\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(as(get("/api/v1/lessons/1/quizzes"), STUDENT_A)).andExpect(status().isNotFound());
        mockMvc.perform(as(get("/api/v1/quizzes/99"), STUDENT_A)).andExpect(status().isNotFound());
        mockMvc.perform(as(post("/api/v1/quizzes/99/attempts"), STUDENT_A)).andExpect(status().isNotFound());
        mockMvc.perform(as(get("/api/v1/quizzes/99"), TEACHER_A)).andExpect(status().isOk());
        mockMvc.perform(as(post("/api/v1/lessons/1/quizzes"), TEACHER_A)
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Quiz moi\"}"))
            .andExpect(status().isConflict());
        mockMvc.perform(as(get("/api/v1/statistics/courses/2"), TEACHER_A))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quiz_count").value(0));
        org.junit.jupiter.api.Assertions.assertEquals(1L,
            jdbc.sql("SELECT COUNT(*) FROM quizzes WHERE id=99").query(Long.class).single());
    }

    @Test
    void unpublishingCourseClosesDirectStudentLessonAndQuizAccess() throws Exception {
        jdbc.sql("INSERT INTO quizzes (id, lesson_id, title, status, created_by) VALUES (99, 1, 'Quiz cu', 'PUBLISHED', 2)")
            .update();
        jdbc.sql("UPDATE courses SET is_published=FALSE WHERE id=2").update();

        mockMvc.perform(as(get("/api/v1/lessons/1"), STUDENT_A)).andExpect(status().isForbidden());
        mockMvc.perform(as(get("/api/v1/courses/2/lessons"), STUDENT_A)).andExpect(status().isForbidden());
        mockMvc.perform(as(get("/api/v1/quizzes/99"), STUDENT_A)).andExpect(status().isForbidden());
        mockMvc.perform(as(post("/api/v1/quizzes/99/attempts"), STUDENT_A)).andExpect(status().isForbidden());
        mockMvc.perform(as(get("/api/v1/users/me/courses"), STUDENT_A))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[?(@.id == '2')].learning_available")
                .value(org.hamcrest.Matchers.contains(false)));
        mockMvc.perform(as(get("/api/v1/lessons/1"), TEACHER_A)).andExpect(status().isOk());
    }

    @Test
    void archivedCourseIsReadOnlyForLessonWrites() throws Exception {
        jdbc.sql("UPDATE courses SET archived_at=UTC_TIMESTAMP(6), is_published=FALSE WHERE id=2").update();

        mockMvc.perform(as(get("/api/v1/lessons/1"), TEACHER_A)).andExpect(status().isOk());
        mockMvc.perform(as(post("/api/v1/courses/2/lessons"), TEACHER_A)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Bai moi\",\"video_url\":\"\"}"))
            .andExpect(status().isConflict());
        mockMvc.perform(as(patch("/api/v1/lessons/1/status"), TEACHER_A)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ARCHIVED\"}"))
            .andExpect(status().isConflict());
        mockMvc.perform(as(delete("/api/v1/lessons/1"), TEACHER_A)).andExpect(status().isConflict());
        mockMvc.perform(as(post("/api/v1/lessons/1/quizzes"), TEACHER_A)
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Quiz moi\"}"))
            .andExpect(status().isConflict());
        mockMvc.perform(as(get("/api/v1/users/me/courses"), STUDENT_A))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[?(@.id == '2')].learning_available")
                .value(org.hamcrest.Matchers.contains(false)));
    }
}
