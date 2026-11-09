package vn.edu.learningai.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import vn.edu.learningai.support.IntegrationTestBase;
import vn.edu.learningai.support.TestImages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * COURSE-01 publication and archive (scenarios T10 and T33).
 *
 * Course 1 is a draft with no lessons and no thumbnail; course 2 is already
 * published, free, with two lessons. Tests that need a publishable draft build
 * one from course 1.
 */
class CoursePublicationControllerTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void publishingRequiresEveryPrecondition() throws Exception {
        // Course 1 has no thumbnail and no lessons.
        mockMvc.perform(patch("/api/v1/courses/1/publication")
                .header("Authorization", "Bearer " + tokenFor(2))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"is_published":true}"""))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("INVALID_STATE"))
            .andExpect(jsonPath("$.message").value(
                org.hamcrest.Matchers.containsString("thumbnail")));

        assertThat(isPublished(1)).isFalse();
    }

    @Test
    void publishingWithoutLessonsIsRejected() throws Exception {
        giveThumbnail(1);

        mockMvc.perform(patch("/api/v1/courses/1/publication")
                .header("Authorization", "Bearer " + tokenFor(2))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"is_published":true}"""))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("INVALID_STATE"))
            .andExpect(jsonPath("$.message").value(
                org.hamcrest.Matchers.containsString("lesson")));

        assertThat(isPublished(1)).isFalse();
    }

    @Test
    void pricedCourseCannotBePublished() throws Exception {
        // Course 3 is a draft priced at 100000, teacher B.
        giveThumbnail(3);
        addLesson(3);

        mockMvc.perform(patch("/api/v1/courses/3/publication")
                .header("Authorization", "Bearer " + tokenFor(3))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"is_published":true}"""))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("COURSE_NOT_FREE"));

        assertThat(isPublished(3)).isFalse();
    }

    @Test
    void draftBecomesPublishedAndAppearsInTheMarketplace() throws Exception {
        giveThumbnail(1);
        addLesson(1);

        // Not visible to guests before.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/courses/1"))
            .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/courses/1/publication")
                .header("Authorization", "Bearer " + tokenFor(2))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"is_published":true}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.is_published").value(true))
            .andExpect(jsonPath("$.archived_at").doesNotExist());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/courses/1"))
            .andExpect(status().isOk());
    }

    @Test
    void unpublishingRemovesItFromTheMarketplaceButKeepsItForTheOwner() throws Exception {
        mockMvc.perform(patch("/api/v1/courses/2/publication")
                .header("Authorization", "Bearer " + tokenFor(2))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"is_published":false}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.is_published").value(false));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/courses/2"))
            .andExpect(status().isNotFound());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/teaching/courses/2").header("Authorization", "Bearer " + tokenFor(2)))
            .andExpect(status().isOk());
    }

    @Test
    void republishingAnAlreadyPublishedCourseIsIdempotent() throws Exception {
        // Course 2 is published and unchanged; repeat publish must not re-run
        // preconditions that a later edit could have invalidated.
        mockMvc.perform(patch("/api/v1/courses/2/publication")
                .header("Authorization", "Bearer " + tokenFor(2))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"is_published":true}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.is_published").value(true));
    }

    @Test
    void otherTeacherCannotPublish() throws Exception {
        giveThumbnail(1);
        addLesson(1);

        mockMvc.perform(patch("/api/v1/courses/1/publication")
                .header("Authorization", "Bearer " + tokenFor(3))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"is_published":true}"""))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        assertThat(isPublished(1)).isFalse();
    }

    @Test
    void studentCannotPublish() throws Exception {
        mockMvc.perform(patch("/api/v1/courses/2/publication")
                .header("Authorization", "Bearer " + tokenFor(4))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"is_published":false}"""))
            .andExpect(status().isForbidden());
    }

    @Test
    void missingFlagIsValidationError() throws Exception {
        mockMvc.perform(patch("/api/v1/courses/2/publication")
                .header("Authorization", "Bearer " + tokenFor(2))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            // The contract reports field names as the client sends them.
            .andExpect(jsonPath("$.details[0].field").value("is_published"));
    }

    @Test
    void archiveHidesTheCourseAndUnpublishesIt() throws Exception {
        mockMvc.perform(patch("/api/v1/courses/2/archive")
                .header("Authorization", "Bearer " + tokenFor(2))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"archived":true}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.archived_at").isNotEmpty())
            .andExpect(jsonPath("$.is_published").value(false));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/courses/2"))
            .andExpect(status().isNotFound());
    }

    @Test
    void archiveKeepsEnrolledStudentsAndTheirAccess() throws Exception {
        // Student 4 is enrolled in course 2 (T33).
        mockMvc.perform(patch("/api/v1/courses/2/archive")
                .header("Authorization", "Bearer " + tokenFor(2))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"archived":true}"""))
            .andExpect(status().isOk());

        // The marketplace no longer offers it.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/courses/2"))
            .andExpect(status().isNotFound());

        // ...but the membership and "my courses" entry survive.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/users/me/courses").header("Authorization", "Bearer " + tokenFor(4)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[*].id").value(
                org.hamcrest.Matchers.hasItem("2")));

        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM course_members WHERE course_id = 2 AND student_id = 4",
            Integer.class)).isEqualTo(1);
    }

    @Test
    void archiveIsIdempotentAndKeepsTheOriginalTimestamp() throws Exception {
        String first = mockMvc.perform(patch("/api/v1/courses/2/archive")
                .header("Authorization", "Bearer " + tokenFor(2))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"archived":true}"""))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String archivedAt = com.jayway.jsonpath.JsonPath.read(first, "$.archived_at");

        mockMvc.perform(patch("/api/v1/courses/2/archive")
                .header("Authorization", "Bearer " + tokenFor(2))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"archived":true}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.archived_at").value(archivedAt));
    }

    @Test
    void archivedCourseCannotBeRepublished() throws Exception {
        // Course 5 is already archived in the fixture.
        mockMvc.perform(patch("/api/v1/courses/5/publication")
                .header("Authorization", "Bearer " + tokenFor(3))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"is_published":true}"""))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("INVALID_STATE"))
            .andExpect(jsonPath("$.message").value(
                org.hamcrest.Matchers.containsString("Archived")));
    }

    @Test
    void archiveFalseIsRejected() throws Exception {
        mockMvc.perform(patch("/api/v1/courses/2/archive")
                .header("Authorization", "Bearer " + tokenFor(2))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"archived":false}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void otherTeacherCannotArchive() throws Exception {
        mockMvc.perform(patch("/api/v1/courses/2/archive")
                .header("Authorization", "Bearer " + tokenFor(3))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"archived":true}"""))
            .andExpect(status().isForbidden());
    }

    @Test
    void archivedCourseCannotBeDeleted() throws Exception {
        mockMvc.perform(patch("/api/v1/courses/2/archive")
                .header("Authorization", "Bearer " + tokenFor(2))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"archived":true}"""))
            .andExpect(status().isOk());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/api/v1/courses/2").header("Authorization", "Bearer " + tokenFor(2)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("INVALID_STATE"));
    }

    @Test
    void publishableDraftAfterEveryEditStillWorks() throws Exception {
        // The full path a teacher takes: create, thumbnail, lesson, then publish.
        String created = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/v1/courses")
                .header("Authorization", "Bearer " + tokenFor(2))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Khoa hoan chinh","description":"Mo ta day du","category_id":"1","price":0}"""))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(created, "$.id");

        mockMvc.perform(multipart("/api/v1/courses/" + id + "/thumbnail")
                .file(new MockMultipartFile("file", "cover.png", "image/png", TestImages.png()))
                .header("Authorization", "Bearer " + tokenFor(2)))
            .andExpect(status().isOk());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/v1/courses/" + id + "/lessons")
                .header("Authorization", "Bearer " + tokenFor(2))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Bai 1","video_url":"https://www.youtube.com/watch?v=dQw4w9WgXcQ","order_index":1}"""))
            .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/v1/courses/" + id + "/publication")
                .header("Authorization", "Bearer " + tokenFor(2))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"is_published":true}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.is_published").value(true));
    }

    private void giveThumbnail(long courseId) {
        jdbcTemplate.update(
            "UPDATE courses SET thumbnail_url = ?, thumbnail_storage_key = ? WHERE id = ?",
            "/api/v1/files/00000000-0000-0000-0000-000000000000.png",
            "00000000-0000-0000-0000-000000000000.png", courseId);
    }

    private void addLesson(long courseId) {
        Integer maxOrder = jdbcTemplate.queryForObject(
            "SELECT COALESCE(MAX(order_index), 0) FROM lessons WHERE course_id = ?", Integer.class, courseId);
        jdbcTemplate.update(
            "INSERT INTO lessons (course_id, title, video_url, order_index) VALUES (?, ?, ?, ?)",
            courseId, "Bai hoc", "https://youtu.be/x", (maxOrder == null ? 0 : maxOrder) + 1);
    }

    private boolean isPublished(long courseId) {
        Boolean value = jdbcTemplate.queryForObject(
            "SELECT is_published FROM courses WHERE id = ?", Boolean.class, courseId);
        return Boolean.TRUE.equals(value);
    }
}
