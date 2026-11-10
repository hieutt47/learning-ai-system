package vn.edu.learningai.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import vn.edu.learningai.support.IntegrationTestBase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql(scripts = {"/test-fixtures.sql", "/enrollment-fixtures.sql"},
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class EnrollmentControllerTest extends IntegrationTestBase {
    private static final String MY_COURSES = "/api/v1/users/me/courses";
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Test
    void firstEnrollmentIsCreated() throws Exception {
        mockMvc.perform(post("/api/v1/courses/6/enroll").header("Authorization", "Bearer " + tokenFor(4)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.course_id").value("6"))
            .andExpect(jsonPath("$.student_id").value("4"))
            .andExpect(jsonPath("$.enrolled_at").exists());
        assertThat(memberCount(6, 4)).isEqualTo(1);
    }
    @Test
    void retryReturnsTheSameMembershipWith200() throws Exception {
        String firstAt = mockMvc.perform(post("/api/v1/courses/6/enroll").header("Authorization", "Bearer " + tokenFor(4)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        java.time.Instant expected = java.time.Instant
            .parse(com.jayway.jsonpath.JsonPath.read(firstAt, "$.enrolled_at"))
            .truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        String secondAt = mockMvc.perform(post("/api/v1/courses/6/enroll").header("Authorization", "Bearer " + tokenFor(4)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        java.time.Instant actual =
            java.time.Instant.parse(com.jayway.jsonpath.JsonPath.read(secondAt, "$.enrolled_at"));

        assertThat(actual.truncatedTo(java.time.temporal.ChronoUnit.MICROS)).isEqualTo(expected);
        assertThat(memberCount(6, 4)).isEqualTo(1);
    }

    @Test
    void enrollingInAnAlreadyJoinedCourseIsIdempotent() throws Exception {

        mockMvc.perform(post("/api/v1/courses/2/enroll").header("Authorization", "Bearer " + tokenFor(4)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.course_id").value("2"));
        assertThat(memberCount(2, 4)).isEqualTo(1);
    }
    @Test
    void concurrentFirstEnrollmentsCreateExactlyOneMembership() throws Exception {

        jdbcTemplate.update("DELETE FROM course_members WHERE course_id = 6 AND student_id = 6");
        jdbcTemplate.update("UPDATE courses SET is_published = TRUE, archived_at = NULL WHERE id = 6");
        int attempts = 6;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch startLine = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < attempts; i++) {
                Callable<Integer> call = () -> {
                    startLine.await(5, TimeUnit.SECONDS);
                    return mockMvc.perform(post("/api/v1/courses/6/enroll").header("Authorization", "Bearer " + tokenFor(6)))
                        .andReturn().getResponse().getStatus();
                };
                futures.add(pool.submit(call));
            }
            startLine.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get(30, TimeUnit.SECONDS));
            }

            assertThat(statuses).containsOnly(200, 201);
            assertThat(statuses).filteredOn(s -> s == 201).hasSize(1);
            assertThat(memberCount(6, 6)).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }
    @Test
    void draftCourseIsNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/courses/1/enroll").header("Authorization", "Bearer " + tokenFor(4)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
    @Test
    void archivedCourseIsNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/courses/5/enroll").header("Authorization", "Bearer " + tokenFor(4)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
    @Test
    void pricedCourseIsCourseNotFree() throws Exception {
        mockMvc.perform(post("/api/v1/courses/7/enroll").header("Authorization", "Bearer " + tokenFor(4)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("COURSE_NOT_FREE"));

        assertThat(memberCount(7, 4)).isZero();
    }
    @Test
    void teacherCannotEnroll() throws Exception {
        mockMvc.perform(post("/api/v1/courses/6/enroll").header("Authorization", "Bearer " + tokenFor(2)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
    @Test
    void adminCannotEnroll() throws Exception {
        mockMvc.perform(post("/api/v1/courses/6/enroll").header("Authorization", "Bearer " + tokenFor(1)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
    @Test
    void anonymousEnrollIsUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/courses/6/enroll"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
    @Test
    void myCoursesListsEnrollmentsWithCourseFields() throws Exception {
        mockMvc.perform(get(MY_COURSES).header("Authorization", "Bearer " + tokenFor(4)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.items[*].id").value(
                org.hamcrest.Matchers.containsInAnyOrder("2", "4")))
            .andExpect(jsonPath("$.items[0].title").exists())
            .andExpect(jsonPath("$.items[0].teacher.full_name").exists())
            .andExpect(jsonPath("$.items[0].currency").value("VND"))
            .andExpect(jsonPath("$.items[0].enrolled_at").exists())
            .andExpect(jsonPath("$.items[0].course").doesNotExist());
    }
    @Test
    void myCoursesKeepsArchivedAndUnpublishedCoursesJoinedEarlier() throws Exception {
        mockMvc.perform(post("/api/v1/courses/6/enroll").header("Authorization", "Bearer " + tokenFor(4)))
            .andExpect(status().isCreated());
        jdbcTemplate.update("UPDATE courses SET archived_at = CURRENT_TIMESTAMP(6) WHERE id = 6");
        mockMvc.perform(get(MY_COURSES).header("Authorization", "Bearer " + tokenFor(4)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[*].id").value(
                org.hamcrest.Matchers.containsInAnyOrder("2", "4", "6")));

        mockMvc.perform(get("/api/v1/courses/6"))
            .andExpect(status().isNotFound());
    }
    @Test
    void myCoursesCarriesLessonCountAndArchiveFlag() throws Exception {
        mockMvc.perform(get(MY_COURSES).header("Authorization", "Bearer " + tokenFor(4)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[?(@.id=='2')].lesson_count").value(2))
            .andExpect(jsonPath("$.items[?(@.id=='2')].archived_at").isEmpty());
    }
    @Test
    void myCoursesForAStudentWithoutEnrollmentsIsEmpty() throws Exception {
        mockMvc.perform(get(MY_COURSES).header("Authorization", "Bearer " + tokenFor(6)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(0))
            .andExpect(jsonPath("$.items.length()").value(0));
    }
    @Test
    void myCoursesDoesNotLeakEmailOrStorageKey() throws Exception {
        mockMvc.perform(get(MY_COURSES).header("Authorization", "Bearer " + tokenFor(4)))
            .andExpect(content().string(not(containsString("@test.local"))))
            .andExpect(content().string(not(containsString("password_hash"))))
            .andExpect(content().string(not(containsString("storage_key"))))
            .andExpect(content().string(not(containsString("youtu.be"))));
    }
    @Test
    void myCoursesRejectsInvalidPagination() throws Exception {
        mockMvc.perform(get(MY_COURSES).header("Authorization", "Bearer " + tokenFor(4)).param("page", "0"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
    @Test
    void ownerListsMembersWithoutEmail() throws Exception {
        mockMvc.perform(get("/api/v1/courses/2/members").header("Authorization", "Bearer " + tokenFor(2)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items[0].student_id").value("4"))
            .andExpect(jsonPath("$.items[0].full_name").value("Student A"))
            .andExpect(jsonPath("$.items[0].enrolled_at").exists())
            .andExpect(content().string(not(containsString("@test.local"))));
    }
    @Test
    void adminListsMembersOfAnyCourse() throws Exception {
        mockMvc.perform(get("/api/v1/courses/2/members").header("Authorization", "Bearer " + tokenFor(1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1));
    }
    @Test
    void otherTeacherCannotListMembers() throws Exception {
        mockMvc.perform(get("/api/v1/courses/2/members").header("Authorization", "Bearer " + tokenFor(3)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
    @Test
    void studentCannotListMembers() throws Exception {
        mockMvc.perform(get("/api/v1/courses/2/members").header("Authorization", "Bearer " + tokenFor(4)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
    @Test
    void membersOfUnknownCourseIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/courses/999/members").header("Authorization", "Bearer " + tokenFor(2)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
    @Test
    void courseWithMembersCannotBeDeleted() throws Exception {
        jdbcTemplate.update("UPDATE courses SET is_published = FALSE, archived_at = NULL WHERE id = 2");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/api/v1/courses/2").header("Authorization", "Bearer " + tokenFor(2)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("INVALID_STATE"));
    }

    private int memberCount(long courseId, long studentId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM course_members WHERE course_id = ? AND student_id = ?",
            Integer.class, courseId, studentId);
        return count == null ? 0 : count;
    }
}
