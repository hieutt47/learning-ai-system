package vn.edu.learningai.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import vn.edu.learningai.common.enums.UserRole;
import vn.edu.learningai.dto.request.SubmitAttemptRequest;
import vn.edu.learningai.entity.User;
import vn.edu.learningai.security.custom.CustomUserDetails;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "QUIZ_TEST_MYSQL_URL",
    matches = "jdbc:mysql://[^/]+/learningai_quiz_test(?:_[A-Za-z0-9]+)?(?:\\?.*)?")
class AttemptMySqlIntegrationTest {
    @Autowired AttemptService service;
    @Autowired JdbcClient jdbc;
    @Autowired PlatformTransactionManager transactions;

    private Long teacherId;
    private Long studentId;
    private Long categoryId;
    private Long courseId;
    private Long lessonId;
    private Long quizId;
    private List<Long> questionIds;
    private List<Long> correctOptionIds;
    private List<Long> wrongOptionIds;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> System.getenv("QUIZ_TEST_MYSQL_URL"));
        properties.add("spring.datasource.username", () -> System.getenv("QUIZ_TEST_MYSQL_USERNAME"));
        properties.add("spring.datasource.password", () -> System.getenv().getOrDefault("QUIZ_TEST_MYSQL_PASSWORD", ""));
        properties.add("spring.jpa.show-sql", () -> false);
        properties.add("jwt.jwt-signing-key", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @BeforeEach
    void fixture() {
        String key = UUID.randomUUID().toString();
        teacherId = insertUser("TEACHER", "teacher-" + key + "@example.invalid");
        studentId = insertUser("STUDENT", "student-" + key + "@example.invalid");
        jdbc.sql("INSERT INTO categories(name) VALUES(:name)").param("name", key).update();
        categoryId = jdbc.sql("SELECT id FROM categories WHERE name=:name").param("name", key)
            .query(Long.class).single();
        jdbc.sql("INSERT INTO courses(teacher_id,category_id,title,description,is_published) " +
                "VALUES(:teacher,:category,:title,'Test',TRUE)")
            .param("teacher", teacherId).param("category", categoryId).param("title", key).update();
        courseId = jdbc.sql("SELECT id FROM courses WHERE teacher_id=:teacher AND title=:title")
            .param("teacher", teacherId).param("title", key).query(Long.class).single();
        jdbc.sql("INSERT INTO lessons(course_id,title,video_url,order_index,status) VALUES(:course,'Test','unused',0,'PUBLISHED')")
            .param("course", courseId).update();
        lessonId = jdbc.sql("SELECT id FROM lessons WHERE course_id=:course").param("course", courseId)
            .query(Long.class).single();
        jdbc.sql("INSERT INTO course_members(course_id,student_id) VALUES(:course,:student)")
            .param("course", courseId).param("student", studentId).update();
        jdbc.sql("INSERT INTO quizzes(lesson_id,title,status,created_by,published_at) " +
                "VALUES(:lesson,'Quiz','PUBLISHED',:teacher,UTC_TIMESTAMP(6))")
            .param("lesson", lessonId).param("teacher", teacherId).update();
        quizId = jdbc.sql("SELECT id FROM quizzes WHERE lesson_id=:lesson").param("lesson", lessonId)
            .query(Long.class).single();

        for (int index = 0; index < 3; index++) {
            jdbc.sql("INSERT INTO questions(quiz_id,content,explanation,order_index) " +
                    "VALUES(:quiz,:content,:explanation,:position)")
                .param("quiz", quizId).param("content", "Q" + index)
                .param("explanation", "E" + index).param("position", index).update();
        }
        questionIds = jdbc.sql("SELECT id FROM questions WHERE quiz_id=:quiz ORDER BY order_index")
            .param("quiz", quizId).query(Long.class).list();
        for (Long questionId : questionIds) {
            jdbc.sql("INSERT INTO question_options(question_id,content,is_correct,order_index) " +
                    "VALUES(:question,'Correct',TRUE,0),(:question,'Wrong',FALSE,1)")
                .param("question", questionId).update();
        }
        correctOptionIds = optionIds(true);
        wrongOptionIds = optionIds(false);
        authenticateStudent();
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        if (quizId != null) {
            jdbc.sql("DELETE a FROM student_answers a JOIN quiz_attempts t ON t.id=a.attempt_id WHERE t.quiz_id=:quiz")
                .param("quiz", quizId).update();
            jdbc.sql("DELETE FROM quiz_attempts WHERE quiz_id=:quiz").param("quiz", quizId).update();
            jdbc.sql("DELETE o FROM question_options o JOIN questions q ON q.id=o.question_id WHERE q.quiz_id=:quiz")
                .param("quiz", quizId).update();
            jdbc.sql("DELETE FROM questions WHERE quiz_id=:quiz").param("quiz", quizId).update();
            jdbc.sql("DELETE FROM quizzes WHERE id=:quiz").param("quiz", quizId).update();
        }
        if (courseId != null && studentId != null) {
            jdbc.sql("DELETE FROM course_members WHERE course_id=:course AND student_id=:student")
                .param("course", courseId).param("student", studentId).update();
        }
        if (lessonId != null) jdbc.sql("DELETE FROM lessons WHERE id=:id").param("id", lessonId).update();
        if (courseId != null) jdbc.sql("DELETE FROM courses WHERE id=:id").param("id", courseId).update();
        if (categoryId != null) jdbc.sql("DELETE FROM categories WHERE id=:id").param("id", categoryId).update();
        if (studentId != null) jdbc.sql("DELETE FROM users WHERE id=:id").param("id", studentId).update();
        if (teacherId != null) jdbc.sql("DELETE FROM users WHERE id=:id").param("id", teacherId).update();
    }

    @Test
    void concurrentStartReturnsOneActiveAttempt() {
        assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
            var executor = Executors.newFixedThreadPool(2);
            var ready = new CountDownLatch(2);
            var start = new CountDownLatch(1);
            try {
                var first = executor.submit(() -> callStart(ready, start));
                var second = executor.submit(() -> callStart(ready, start));
                await(ready);
                start.countDown();
                var a = first.get(15, TimeUnit.SECONDS);
                var b = second.get(15, TimeUnit.SECONDS);
                assertThat(a.attempt().id()).isEqualTo(b.attempt().id());
                assertThat(jdbc.sql("SELECT COUNT(*) FROM quiz_attempts WHERE quiz_id=:quiz AND status='IN_PROGRESS'")
                    .param("quiz", quizId).query(Long.class).single()).isEqualTo(1);
            } finally {
                executor.shutdownNow();
            }
        });
    }

    @Test
    void concurrentSubmitReturnsStoredResultAndOneAnswerSet() {
        Long attemptId = Long.valueOf(service.start(quizId).attempt().id());
        assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
            var executor = Executors.newFixedThreadPool(2);
            var ready = new CountDownLatch(2);
            var start = new CountDownLatch(1);
            try {
                var first = executor.submit(() -> callSubmit(attemptId, request(correctOptionIds), ready, start));
                var second = executor.submit(() -> callSubmit(attemptId, request(wrongOptionIds), ready, start));
                await(ready);
                start.countDown();
                var a = first.get(15, TimeUnit.SECONDS);
                var b = second.get(15, TimeUnit.SECONDS);
                assertThat(a.score()).isEqualByComparingTo(b.score());
                assertThat(a.correctCount()).isEqualTo(b.correctCount());
                assertThat(jdbc.sql("SELECT COUNT(*) FROM student_answers WHERE attempt_id=:attempt")
                    .param("attempt", attemptId).query(Long.class).single()).isEqualTo(3);
            } finally {
                executor.shutdownNow();
            }
        });
    }

    @Test
    void submitRollsBackAnswersAndResultThenRetrySucceeds() {
        Long attemptId = Long.valueOf(service.start(quizId).attempt().id());
        var transaction = new TransactionTemplate(transactions);
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            service.submit(attemptId, request(correctOptionIds));
            throw new IllegalStateException("Rollback probe");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(jdbc.sql("SELECT status FROM quiz_attempts WHERE id=:attempt")
            .param("attempt", attemptId).query(String.class).single()).isEqualTo("IN_PROGRESS");
        assertThat(jdbc.sql("SELECT COUNT(*) FROM student_answers WHERE attempt_id=:attempt")
            .param("attempt", attemptId).query(Long.class).single()).isZero();

        var result = service.submit(attemptId, request(correctOptionIds));
        assertThat(result.score()).isEqualByComparingTo("100.00");
        assertThat(result.correctCount()).isEqualTo(3);
    }

    private Long insertUser(String role, String email) {
        jdbc.sql("INSERT INTO users(full_name,email,password_hash,role) VALUES('Attempt test',:email,'unused',:role)")
            .param("email", email).param("role", role).update();
        return jdbc.sql("SELECT id FROM users WHERE email=:email").param("email", email)
            .query(Long.class).single();
    }

    private List<Long> optionIds(boolean correct) {
        return jdbc.sql("SELECT o.id FROM question_options o JOIN questions q ON q.id=o.question_id " +
                "WHERE q.quiz_id=:quiz AND o.is_correct=:correct ORDER BY q.order_index")
            .param("quiz", quizId).param("correct", correct).query(Long.class).list();
    }

    private SubmitAttemptRequest request(List<Long> selectedOptions) {
        return new SubmitAttemptRequest(java.util.stream.IntStream.range(0, questionIds.size())
            .mapToObj(index -> new SubmitAttemptRequest.Answer(
                questionIds.get(index).toString(), selectedOptions.get(index).toString()))
            .toList());
    }

    private AttemptService.StartResult callStart(CountDownLatch ready, CountDownLatch start) {
        authenticateStudent();
        try {
            ready.countDown();
            await(start);
            return service.start(quizId);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private vn.edu.learningai.dto.response.AttemptResultResponse callSubmit(
        Long attemptId, SubmitAttemptRequest request, CountDownLatch ready, CountDownLatch start) {
        authenticateStudent();
        try {
            ready.countDown();
            await(start);
            return service.submit(attemptId, request);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticateStudent() {
        var principal = CustomUserDetails.build(User.builder().id(studentId).role(UserRole.STUDENT)
            .email("student@example.invalid").fullName("Student").passwordHash("unused").isActive(true).build());
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out waiting");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
