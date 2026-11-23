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
import vn.edu.learningai.common.enums.QuizStatus;
import vn.edu.learningai.common.enums.UserRole;
import vn.edu.learningai.dto.request.CreateQuizRequest;
import vn.edu.learningai.dto.request.QuizPublicationRequest;
import vn.edu.learningai.dto.request.UpdateQuizRequest;
import vn.edu.learningai.dto.request.WriteQuestionRequest;
import vn.edu.learningai.entity.User;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.repository.QuizRepository;
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
class QuizMySqlIntegrationTest {
    @Autowired QuizService service;
    @Autowired QuizDraftSaveService draftSaves;
    @Autowired QuizRepository quizzes;
    @Autowired JdbcClient jdbc;
    @Autowired PlatformTransactionManager transactions;
    private Long userId;
    private Long categoryId;
    private Long courseId;
    private Long lessonId;
    private Long quizId;

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
        jdbc.sql("INSERT INTO users(full_name,email,password_hash,role) VALUES('Quiz test',:email,'unused','TEACHER')")
            .param("email", key + "@example.invalid").update();
        userId = jdbc.sql("SELECT id FROM users WHERE email=:email").param("email", key + "@example.invalid").query(Long.class).single();
        jdbc.sql("INSERT INTO categories(name) VALUES(:name)").param("name", key).update();
        categoryId = jdbc.sql("SELECT id FROM categories WHERE name=:name").param("name", key).query(Long.class).single();
        jdbc.sql("INSERT INTO courses(teacher_id,category_id,title,description) VALUES(:user,:category,:title,'Test')")
            .param("user", userId).param("category", categoryId).param("title", key).update();
        courseId = jdbc.sql("SELECT id FROM courses WHERE teacher_id=:user").param("user", userId).query(Long.class).single();
        jdbc.sql("INSERT INTO lessons(course_id,title,video_url,order_index,status) VALUES(:course,'Test','unused-test-video',0,'PUBLISHED')")
            .param("course", courseId).update();
        lessonId = jdbc.sql("SELECT id FROM lessons WHERE course_id=:course").param("course", courseId).query(Long.class).single();
        authenticate();
        quizId = Long.valueOf(service.create(lessonId, new CreateQuizRequest("Quiz", null)).id());
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        if (quizId != null) {
            jdbc.sql("DELETE a FROM student_answers a JOIN quiz_attempts t ON t.id=a.attempt_id WHERE t.quiz_id=:id").param("id", quizId).update();
            jdbc.sql("DELETE FROM quiz_attempts WHERE quiz_id=:id").param("id", quizId).update();
            jdbc.sql("DELETE o FROM question_options o JOIN questions q ON q.id=o.question_id WHERE q.quiz_id=:id").param("id", quizId).update();
            jdbc.sql("DELETE FROM questions WHERE quiz_id=:id").param("id", quizId).update();
            jdbc.sql("DELETE FROM quizzes WHERE id=:id").param("id", quizId).update();
        }
        if (lessonId != null) jdbc.sql("DELETE FROM lessons WHERE id=:id").param("id", lessonId).update();
        if (courseId != null) jdbc.sql("DELETE FROM courses WHERE id=:id").param("id", courseId).update();
        if (categoryId != null) jdbc.sql("DELETE FROM categories WHERE id=:id").param("id", categoryId).update();
        if (userId != null) jdbc.sql("DELETE FROM users WHERE id=:id").param("id", userId).update();
    }

    @Test
    void realDatabaseReplacesOptionsAndRollsBackOnFailure() {
        var original = service.addQuestion(quizId, question("Original"));
        List<String> oldIds = original.options().stream().map(option -> option.id()).toList();
        var updated = service.replaceQuestion(Long.valueOf(original.id()), question("Updated"));
        assertThat(updated.options()).extracting(option -> option.id()).doesNotContainAnyElementsOf(oldIds);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM question_options WHERE question_id=:id")
            .param("id", Long.valueOf(original.id())).query(Long.class).single()).isEqualTo(2);
        assertThatThrownBy(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
            service.replaceQuestion(Long.valueOf(original.id()), question("Rolled back"));
            throw new IllegalStateException("Rollback probe");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.sql("SELECT content FROM questions WHERE id=:id").param("id", Long.valueOf(original.id()))
            .query(String.class).single()).isEqualTo("Updated");
        assertThat(jdbc.sql("SELECT id FROM question_options WHERE question_id=:id ORDER BY order_index")
            .param("id", Long.valueOf(original.id())).query(Long.class).list())
            .containsExactlyElementsOf(updated.options().stream().map(option -> Long.valueOf(option.id())).toList());
    }

    @Test
    void historyPreventsDeletionAndForeignKeysProtectQuestions() {
        var question = service.addQuestion(quizId, question("Question"));
        jdbc.sql("INSERT INTO quiz_attempts(quiz_id,student_id,total_questions) VALUES(:quiz,:user,1)")
            .param("quiz", quizId).param("user", userId).update();
        assertThatThrownBy(() -> service.delete(quizId)).isInstanceOfSatisfying(ApiException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_IN_USE));
        assertThat(jdbc.sql("SELECT COUNT(*) FROM quizzes WHERE id=:id").param("id", quizId).query(Long.class).single()).isEqualTo(1);
        assertThatThrownBy(() -> jdbc.sql("DELETE FROM questions WHERE id=:id").param("id", Long.valueOf(question.id())).update())
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    private vn.edu.learningai.dto.request.SaveQuizDraftRequest snapshot(String key, String title) {
        return new vn.edu.learningai.dto.request.SaveQuizDraftRequest(key,title,List.of(
            new vn.edu.learningai.dto.request.SaveQuizDraftRequest.Item(null,question("New question"))));
    }

    @Test
    void atomicSnapshotReplayReturnsSameIdsAndRejectsChangedPayload() {
        String key=UUID.randomUUID().toString();
        var request=snapshot(key,"Saved title");
        var saved=draftSaves.save(quizId,request);
        var replay=draftSaves.save(quizId,request);
        assertThat(replay).isEqualTo(saved);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM questions WHERE quiz_id=:id").param("id",quizId).query(Long.class).single()).isEqualTo(1);
        assertThatThrownBy(() -> draftSaves.save(quizId,snapshot(key,"Different"))).isInstanceOf(ApiException.class);
        assertThat(quizzes.findById(quizId).orElseThrow().getTitle()).isEqualTo("Saved title");
    }

    @Test
    void atomicSnapshotKeepsExistingQuestionIdsWhenReorderingAndDeleting() {
        var a=service.addQuestion(quizId,question("A"));
        var b=service.addQuestion(quizId,new WriteQuestionRequest("B",null,1,question("B").options(),null));
        var request=new vn.edu.learningai.dto.request.SaveQuizDraftRequest(UUID.randomUUID().toString(),"Reordered",List.of(
            new vn.edu.learningai.dto.request.SaveQuizDraftRequest.Item(Long.valueOf(b.id()),question("B updated")),
            new vn.edu.learningai.dto.request.SaveQuizDraftRequest.Item(Long.valueOf(a.id()),new WriteQuestionRequest("A",null,1,question("A").options(),null))));
        var result=draftSaves.save(quizId,request);
        assertThat(result.questions()).extracting(q->q.id()).containsExactly(b.id(),a.id());
        var deleted=new vn.edu.learningai.dto.request.SaveQuizDraftRequest(UUID.randomUUID().toString(),"Removed",List.of(
            new vn.edu.learningai.dto.request.SaveQuizDraftRequest.Item(Long.valueOf(a.id()),question("A"))));
        assertThat(draftSaves.save(quizId,deleted).questions()).extracting(q->q.id()).containsExactly(a.id());
    }

    @Test
    void snapshotReplayDoesNotRestoreOldSnapshotAfterNewSave() {
        var old=snapshot(UUID.randomUUID().toString(),"Old");
        draftSaves.save(quizId,old);
        draftSaves.save(quizId,snapshot(UUID.randomUUID().toString(),"New"));
        assertThat(draftSaves.save(quizId,old).title()).isEqualTo("Old");
        assertThat(quizzes.findById(quizId).orElseThrow().getTitle()).isEqualTo("New");
    }

    @Test
    void snapshotFailureRollsBackTitleQuestionsAndReceipt() {
        var original=service.addQuestion(quizId,question("Original"));
        jdbc.sql("CREATE TRIGGER test_snapshot_fail BEFORE INSERT ON question_options FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='injected option write failure'").update();
        try {
            assertThatThrownBy(() -> draftSaves.save(quizId,snapshot(UUID.randomUUID().toString(),"Must rollback")))
                .isInstanceOf(RuntimeException.class);
            assertThat(quizzes.findById(quizId).orElseThrow().getTitle()).isEqualTo("Quiz");
            assertThat(jdbc.sql("SELECT id FROM questions WHERE quiz_id=:id").param("id",quizId).query(Long.class).list())
                .containsExactly(Long.valueOf(original.id()));
            assertThat(jdbc.sql("SELECT COUNT(*) FROM quiz_draft_save_receipts WHERE quiz_id=:id").param("id",quizId).query(Long.class).single()).isZero();
        } finally { jdbc.sql("DROP TRIGGER test_snapshot_fail").update(); }
    }

    @Test
    void concurrentSnapshotRetryCreatesExactlyOneQuestion() throws Exception {
        var request=snapshot(UUID.randomUUID().toString(),"Concurrent");
        var pool=Executors.newFixedThreadPool(2);
        try {
            var one=pool.submit(() -> { authenticate(); try { return draftSaves.save(quizId,request); } finally { SecurityContextHolder.clearContext(); } });
            var two=pool.submit(() -> { authenticate(); try { return draftSaves.save(quizId,request); } finally { SecurityContextHolder.clearContext(); } });
            assertThat(one.get(15,TimeUnit.SECONDS)).isEqualTo(two.get(15,TimeUnit.SECONDS));
            assertThat(jdbc.sql("SELECT COUNT(*) FROM questions WHERE quiz_id=:id").param("id",quizId).query(Long.class).single()).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }

    @Test
    void snapshotCannotModifyPublishedQuizOrUseForeignQuestion() {
        var request=snapshot(UUID.randomUUID().toString(),"Published");
        var foreign=new vn.edu.learningai.dto.request.SaveQuizDraftRequest(UUID.randomUUID().toString(),"Foreign",List.of(
            new vn.edu.learningai.dto.request.SaveQuizDraftRequest.Item(Long.MAX_VALUE,question("Foreign"))));
        assertThatThrownBy(() -> draftSaves.save(quizId,foreign)).isInstanceOf(ApiException.class);
        service.addQuestion(quizId,question("Original"));
        service.changePublication(quizId,new QuizPublicationRequest(QuizStatus.PUBLISHED));
        assertThatThrownBy(() -> draftSaves.save(quizId,request)).isInstanceOf(ApiException.class);
    }

    @Test
    void twoPhaseReorderPreservesQuestionIdsAndUniquePositions() {
        var a = service.addQuestion(quizId, question("A"));
        var b = service.addQuestion(quizId, new WriteQuestionRequest("B", null, 1, question("B").options(), null));
        assertThatThrownBy(() -> service.replaceQuestion(Long.valueOf(b.id()), question("B")))
            .isInstanceOf(ApiException.class);
        service.replaceQuestion(Long.valueOf(a.id()), new WriteQuestionRequest("A", null, 3, question("A").options(), null));
        service.replaceQuestion(Long.valueOf(b.id()), new WriteQuestionRequest("B", null, 4, question("B").options(), null));
        service.replaceQuestion(Long.valueOf(b.id()), question("B"));
        service.replaceQuestion(Long.valueOf(a.id()), new WriteQuestionRequest("A", null, 1, question("A").options(), null));
        assertThat(jdbc.sql("SELECT id FROM questions WHERE quiz_id=:id ORDER BY order_index")
            .param("id", quizId).query(Long.class).list())
            .containsExactly(Long.valueOf(b.id()), Long.valueOf(a.id()));
    }

    @Test
    void publicationSerializesWithConcurrentEdit() {
        service.addQuestion(quizId, question("Question"));
        assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
            var executor = Executors.newFixedThreadPool(2);
            var locked = new CountDownLatch(1);
            var editing = new CountDownLatch(1);
            try {
                var publisher = executor.submit(() -> {
                    authenticate();
                    try {
                        new TransactionTemplate(transactions).executeWithoutResult(status -> {
                            quizzes.findByIdForUpdate(quizId).orElseThrow();
                            locked.countDown();
                            await(editing);
                            service.changePublication(quizId, new QuizPublicationRequest(QuizStatus.PUBLISHED));
                        });
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                });
                var editor = executor.submit(() -> {
                    authenticate();
                    try {
                        await(locked);
                        editing.countDown();
                        assertThatThrownBy(() -> service.update(quizId, new UpdateQuizRequest("Must not persist", null)))
                            .isInstanceOfSatisfying(ApiException.class,
                                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATE));
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                });
                publisher.get(15, TimeUnit.SECONDS);
                editor.get(15, TimeUnit.SECONDS);
                assertThat(jdbc.sql("SELECT title FROM quizzes WHERE id=:id").param("id", quizId).query(String.class).single()).isEqualTo("Quiz");
            } finally {
                executor.shutdownNow();
            }
        });
    }

    private WriteQuestionRequest question(String content) {
        return new WriteQuestionRequest(content, null, 0, List.of(
            new WriteQuestionRequest.Option("Yes", true, 0), new WriteQuestionRequest.Option("No", false, 1)), null);
    }

    private void authenticate() {
        var principal = CustomUserDetails.build(User.builder().id(userId).role(UserRole.TEACHER)
            .email("quiz-test@example.invalid").fullName("Quiz test").passwordHash("unused").build());
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out waiting for concurrent operation");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
