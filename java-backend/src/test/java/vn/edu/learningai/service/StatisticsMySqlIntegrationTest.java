package vn.edu.learningai.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import com.jayway.jsonpath.JsonPath;
import java.util.Base64;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named="QUIZ_TEST_MYSQL_URL",matches="jdbc:mysql://[^/]+/learningai_quiz_test_documents(?:\\?.*)?")
@Sql("/test-fixtures.sql")
class StatisticsMySqlIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url",()->System.getenv("QUIZ_TEST_MYSQL_URL"));
        p.add("spring.datasource.username",()->System.getenv("QUIZ_TEST_MYSQL_USERNAME"));
        p.add("spring.datasource.password",()->System.getenv().getOrDefault("QUIZ_TEST_MYSQL_PASSWORD",""));
        p.add("jwt.jwt-signing-key",()->Base64.getEncoder().encodeToString(new byte[32]));
        p.add("spring.jpa.show-sql",()->false);
        p.add("app.ai.provider",()->"mock");
    }
    private String token(String email) throws Exception {
        String body=mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\""+email+"\",\"password\":\"DemoPass123!\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body,"$.access_token");
    }
    private void seed() {
        jdbc.sql("INSERT INTO quizzes(id,lesson_id,title,status,created_by) VALUES(30,1,'Q1','PUBLISHED',2),(31,1,'Q2','PUBLISHED',2)").update();
        jdbc.sql("INSERT INTO questions(id,quiz_id,content,order_index) VALUES(100,30,'Q',0),(101,30,'Q omitted',1)").update();
        jdbc.sql("INSERT INTO course_members(course_id,student_id) VALUES(2,6)").update();
        jdbc.sql("""
            INSERT INTO quiz_attempts(id,quiz_id,student_id,status,total_questions,score,correct_count,submitted_at)
            VALUES(50,30,4,'SUBMITTED',1,100,1,UTC_TIMESTAMP()),(51,30,4,'SUBMITTED',1,0,0,UTC_TIMESTAMP()),
            (52,30,6,'IN_PROGRESS',1,NULL,NULL,NULL),(53,31,4,'SUBMITTED',1,100,1,UTC_TIMESTAMP())
            """).update();
        jdbc.sql("INSERT INTO question_options(id,question_id,content,is_correct,order_index) VALUES(1000,100,'Correct',TRUE,0)").update();
        jdbc.sql("INSERT INTO student_answers(attempt_id,question_id,selected_option_id,is_correct) VALUES(50,100,1000,TRUE),(51,100,NULL,FALSE)").update();
    }
    @Test void submittedOnlyAndCompletionUseEveryPublishedQuiz() throws Exception {
        seed(); String t=token("teacher.a@test.local");
        mvc.perform(get("/api/v1/statistics/courses/2").header("Authorization","Bearer "+t))
            .andExpect(status().isOk()).andExpect(jsonPath("$.quiz_count").value(2))
            .andExpect(jsonPath("$.submitted_attempts").value(3)).andExpect(jsonPath("$.completed_students").value(1))
            .andExpect(jsonPath("$.completion_rate").value(50));
        mvc.perform(get("/api/v1/statistics/quizzes/30").header("Authorization","Bearer "+t))
            .andExpect(status().isOk()).andExpect(jsonPath("$.submitted_attempts").value(2))
            .andExpect(jsonPath("$.unique_students").value(1)).andExpect(jsonPath("$.average_score").value(50))
            .andExpect(jsonPath("$.best_scores[0].student_id").value("4"))
            .andExpect(jsonPath("$.best_scores[0].score").value(100))
            .andExpect(jsonPath("$.hardest_questions[0].question_id").value("101"))
            .andExpect(jsonPath("$.hardest_questions[0].wrong_count").value(2))
            .andExpect(jsonPath("$.hardest_questions[1].wrong_count").value(1));
        jdbc.sql("UPDATE quizzes SET status='CLOSED' WHERE id=31").update();
        mvc.perform(get("/api/v1/statistics/courses/2").header("Authorization","Bearer "+t))
            .andExpect(status().isOk()).andExpect(jsonPath("$.quiz_count").value(1))
            .andExpect(jsonPath("$.completion_rate").value(50));
    }
    @Test void zeroEnrollmentOrPublishedQuizYieldsZeroAndNoAverage() throws Exception {
        String t=token("teacher.a@test.local");
        mvc.perform(get("/api/v1/statistics/courses/1").header("Authorization","Bearer "+t))
            .andExpect(status().isOk()).andExpect(jsonPath("$.completion_rate").value(0));
        seed(); jdbc.sql("DELETE FROM course_members WHERE course_id=2").update();
        mvc.perform(get("/api/v1/statistics/courses/2").header("Authorization","Bearer "+t))
            .andExpect(status().isOk()).andExpect(jsonPath("$.completion_rate").value(0));
        jdbc.sql("DELETE FROM quiz_attempts WHERE quiz_id=31").update();
        jdbc.sql("INSERT INTO quiz_attempts(quiz_id,student_id,status,total_questions) VALUES(31,4,'IN_PROGRESS',1)").update();
        mvc.perform(get("/api/v1/statistics/quizzes/31").header("Authorization","Bearer "+t))
            .andExpect(status().isOk()).andExpect(jsonPath("$.submitted_attempts").value(0))
            .andExpect(jsonPath("$.unique_students").value(0))
            .andExpect(jsonPath("$.average_score").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.best_scores").isEmpty()).andExpect(jsonPath("$.hardest_questions").isEmpty());
    }
    @Test void averageScoreRoundsAllSubmittedAttemptsToTwoDecimals() throws Exception {
        seed();
        jdbc.sql("""
            INSERT INTO quiz_attempts(id,quiz_id,student_id,status,total_questions,score,correct_count,submitted_at)
            VALUES(54,30,6,'SUBMITTED',1,0,0,UTC_TIMESTAMP())
            """).update();
        String teacher = token("teacher.a@test.local");
        mvc.perform(get("/api/v1/statistics/quizzes/30").header("Authorization", "Bearer " + teacher))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.submitted_attempts").value(3))
            .andExpect(jsonPath("$.unique_students").value(2))
            .andExpect(jsonPath("$.average_score").value(33.33));
    }
    @Test void statisticsEnforceOwnershipAndAdmin() throws Exception {
        seed();
        mvc.perform(get("/api/v1/statistics/courses/2")).andExpect(status().isUnauthorized());
        for(String email:java.util.List.of("teacher.b@test.local","student.a@test.local")) {
            mvc.perform(get("/api/v1/statistics/courses/2").header("Authorization","Bearer "+token(email)))
                .andExpect(status().isForbidden());
            mvc.perform(get("/api/v1/statistics/quizzes/30").header("Authorization","Bearer "+token(email)))
                .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/v1/statistics/system").header("Authorization","Bearer "+token("teacher.a@test.local")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/statistics/system").header("Authorization","Bearer "+token("student.a@test.local")))
            .andExpect(status().isForbidden());
        for(String path:java.util.List.of("/api/v1/statistics/courses/2","/api/v1/statistics/quizzes/30")) {
            mvc.perform(get(path).header("Authorization","Bearer "+token("admin@test.local"))).andExpect(status().isOk());
        }
        mvc.perform(get("/api/v1/statistics/system").header("Authorization","Bearer "+token("admin@test.local")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.submitted_attempts").value(3));
    }
    @Test void aiMockPreviewDoesNotPersistQuizUntilExplicitSaveAndChecksOwner() throws Exception {
        jdbc.sql("UPDATE lessons SET transcript_text='Java variables types loops classes examples' WHERE id=1").update();
        String request="{\"lesson_id\":\"1\",\"source_type\":\"TRANSCRIPT\",\"count\":2,\"difficulty\":\"EASY\"}";
        mvc.perform(post("/api/v1/ai/generate-quiz").header("Authorization","Bearer "+token("teacher.b@test.local"))
            .contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isForbidden());
        String teacher=token("teacher.a@test.local");
        String generated=mvc.perform(post("/api/v1/ai/generate-quiz").header("Authorization","Bearer "+teacher)
            .contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isOk())
            .andExpect(jsonPath("$.candidate.questions.length()").value(2))
            .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(jdbc.sql("SELECT COUNT(*) FROM quizzes").query(Long.class).single()).isZero();
        String generation=JsonPath.read(generated,"$.generation_id").toString();
        String created=mvc.perform(post("/api/v1/lessons/1/quizzes").header("Authorization","Bearer "+teacher)
            .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"AI draft\",\"generation_id\":\""+generation+"\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("DRAFT"))
            .andReturn().getResponse().getContentAsString();
        String quiz=JsonPath.read(created,"$.id").toString();
        var mapper=tools.jackson.databind.json.JsonMapper.builder().build();
        java.util.List<java.util.Map<String,Object>> candidates=JsonPath.read(generated,"$.candidate.questions");
        for(int index=0;index<candidates.size();index++) {
            var question=new java.util.LinkedHashMap<>(candidates.get(index));
            question.put("order_index",index); question.put("generation_id",generation);
            @SuppressWarnings("unchecked")
            var options=(java.util.List<java.util.Map<String,Object>>)question.get("options");
            for(int position=0;position<options.size();position++) options.get(position).put("order_index",position);
            mvc.perform(post("/api/v1/quizzes/"+quiz+"/questions").header("Authorization","Bearer "+teacher)
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(question)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.generated_by_ai").value(true));
        }
        mvc.perform(patch("/api/v1/quizzes/"+quiz+"/publication").header("Authorization","Bearer "+teacher)
            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PUBLISHED\"}"))
            .andExpect(status().isOk());
        String student=token("student.a@test.local");
        mvc.perform(get("/api/v1/quizzes/"+quiz).header("Authorization","Bearer "+student))
            .andExpect(status().isOk()).andExpect(jsonPath("$.questions[0].explanation").doesNotExist())
            .andExpect(jsonPath("$.questions[0].options[0].is_correct").doesNotExist());
        String started=mvc.perform(post("/api/v1/quizzes/"+quiz+"/attempts").header("Authorization","Bearer "+student))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String attempt=JsonPath.read(started,"$.id").toString();
        mvc.perform(post("/api/v1/attempts/"+attempt+"/submit").header("Authorization","Bearer "+student)
            .contentType(MediaType.APPLICATION_JSON).content("{\"answers\":[]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.score").value(0));
        mvc.perform(get("/api/v1/users/me/attempts").header("Authorization","Bearer "+student))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items[0].id").value(attempt));
        mvc.perform(get("/api/v1/statistics/quizzes/"+quiz).header("Authorization","Bearer "+teacher))
            .andExpect(status().isOk()).andExpect(jsonPath("$.submitted_attempts").value(1))
            .andExpect(jsonPath("$.average_score").value(0));
    }
}
