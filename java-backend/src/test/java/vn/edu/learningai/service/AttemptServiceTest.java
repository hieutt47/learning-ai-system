package vn.edu.learningai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockMakers;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.learningai.common.enums.AttemptStatus;
import vn.edu.learningai.common.enums.QuizStatus;
import vn.edu.learningai.common.enums.LessonStatus;
import vn.edu.learningai.common.enums.UserRole;
import vn.edu.learningai.dto.request.SubmitAttemptRequest;
import vn.edu.learningai.entity.Question;
import vn.edu.learningai.entity.QuestionOption;
import vn.edu.learningai.entity.Quiz;
import vn.edu.learningai.entity.QuizAttempt;
import vn.edu.learningai.entity.StudentAnswer;
import vn.edu.learningai.entity.User;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.repository.QuestionOptionRepository;
import vn.edu.learningai.repository.QuestionRepository;
import vn.edu.learningai.repository.QuizAttemptRepository;
import vn.edu.learningai.repository.QuizContextRepository;
import vn.edu.learningai.repository.QuizRepository;
import vn.edu.learningai.repository.StudentAnswerRepository;
import vn.edu.learningai.security.SecurityHelper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttemptServiceTest {
    @Mock QuizAttemptRepository attempts;
    @Mock StudentAnswerRepository answers;
    @Mock QuizRepository quizzes;
    @Mock QuestionRepository questions;
    @Mock QuestionOptionRepository options;
    @Mock(mockMaker = MockMakers.SUBCLASS) QuizContextRepository context;
    @Mock(mockMaker = MockMakers.SUBCLASS) SecurityHelper security;
    @Mock(mockMaker = MockMakers.SUBCLASS) vn.edu.learningai.repository.AttemptHistoryRepository history;
    AttemptService service;

    @BeforeEach
    void setUp() {
        service = new AttemptService(attempts, answers, quizzes, questions, options, context, security, history);
        when(security.getCurrentUser()).thenReturn(User.builder().id(4L).role(UserRole.STUDENT).isActive(true).build());
    }

    @Test
    void startCreatesOneInProgressAttemptForEnrolledStudent() {
        Quiz quiz = quiz(QuizStatus.PUBLISHED);
        when(quizzes.findByIdForUpdate(30L)).thenReturn(Optional.of(quiz));
        when(context.findLesson(10L)).thenReturn(Optional.of(new QuizContextRepository.LessonContext(20L, 2L)));
        when(context.isEnrolled(20L, 4L)).thenReturn(true);
        when(attempts.findByQuizIdAndStudentIdAndStatus(30L, 4L, AttemptStatus.IN_PROGRESS)).thenReturn(Optional.empty());
        when(questions.countByQuizId(30L)).thenReturn(3L);
        when(attempts.saveAndFlush(any())).thenAnswer(invocation -> {
            QuizAttempt attempt = invocation.getArgument(0);
            attempt.setId(50L);
            attempt.setStartedAt(Instant.parse("2026-11-14T00:00:00Z"));
            return attempt;
        });

        var result = service.start(30L);

        assertThat(result.created()).isTrue();
        assertThat(result.attempt().id()).isEqualTo("50");
        assertThat(result.attempt().status()).isEqualTo(AttemptStatus.IN_PROGRESS);
        assertThat(result.attempt().totalQuestions()).isEqualTo(3);
    }

    @Test
    void unenrolledStudentCannotStartAttempt() {
        when(quizzes.findByIdForUpdate(30L)).thenReturn(Optional.of(quiz(QuizStatus.PUBLISHED)));
        when(context.findLesson(10L)).thenReturn(Optional.of(new QuizContextRepository.LessonContext(20L, 2L)));
        when(context.isEnrolled(20L, 4L)).thenReturn(false);

        assertThatThrownBy(() -> service.start(30L))
            .isInstanceOfSatisfying(ApiException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ENROLLMENT_REQUIRED));
        verify(attempts, never()).saveAndFlush(any());
    }

    @Test
    void draftQuizCannotStartAttempt() {
        when(quizzes.findByIdForUpdate(30L)).thenReturn(Optional.of(quiz(QuizStatus.DRAFT)));
        when(context.findLesson(10L)).thenReturn(Optional.of(new QuizContextRepository.LessonContext(20L, 2L)));
        when(context.isEnrolled(20L, 4L)).thenReturn(true);
        when(attempts.findByQuizIdAndStudentIdAndStatus(30L, 4L, AttemptStatus.IN_PROGRESS))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.start(30L))
            .isInstanceOfSatisfying(ApiException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATE));
        verify(attempts, never()).saveAndFlush(any());
    }

    @Test
    void repeatedStartReturnsExistingAttempt() {
        Quiz quiz = quiz(QuizStatus.PUBLISHED);
        QuizAttempt existing = attempt(AttemptStatus.IN_PROGRESS);
        when(quizzes.findByIdForUpdate(30L)).thenReturn(Optional.of(quiz));
        when(context.findLesson(10L)).thenReturn(Optional.of(new QuizContextRepository.LessonContext(20L, 2L)));
        when(context.isEnrolled(20L, 4L)).thenReturn(true);
        when(attempts.findByQuizIdAndStudentIdAndStatus(30L, 4L, AttemptStatus.IN_PROGRESS))
            .thenReturn(Optional.of(existing));

        var result = service.start(30L);
        assertThat(result.created()).isFalse();
        assertThat(result.attempt().id()).isEqualTo("50");
        verify(attempts, never()).saveAndFlush(any());
    }

    @Test
    void archivedLessonCannotReopenAnExistingAttempt() {
        when(quizzes.findByIdForUpdate(30L)).thenReturn(Optional.of(quiz(QuizStatus.PUBLISHED)));
        when(context.findLesson(10L)).thenReturn(Optional.of(
            new QuizContextRepository.LessonContext(20L, 2L, LessonStatus.ARCHIVED)));
        when(context.isEnrolled(20L, 4L)).thenReturn(true);

        assertThatThrownBy(() -> service.start(30L))
            .isInstanceOfSatisfying(ApiException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        verify(attempts, never()).findByQuizIdAndStudentIdAndStatus(any(), any(), any());
    }

    @Test
    void closedQuizReturnsExistingInProgressAttempt() {
        when(quizzes.findByIdForUpdate(30L)).thenReturn(Optional.of(quiz(QuizStatus.CLOSED)));
        when(context.findLesson(10L)).thenReturn(Optional.of(new QuizContextRepository.LessonContext(20L, 2L)));
        when(context.isEnrolled(20L, 4L)).thenReturn(true);
        when(attempts.findByQuizIdAndStudentIdAndStatus(30L, 4L, AttemptStatus.IN_PROGRESS))
            .thenReturn(Optional.of(attempt(AttemptStatus.IN_PROGRESS)));

        var result = service.start(30L);

        assertThat(result.created()).isFalse();
        assertThat(result.attempt().id()).isEqualTo("50");
    }

    @Test
    void closedQuizCannotStartNewAttempt() {
        when(quizzes.findByIdForUpdate(30L)).thenReturn(Optional.of(quiz(QuizStatus.CLOSED)));
        when(context.findLesson(10L)).thenReturn(Optional.of(new QuizContextRepository.LessonContext(20L, 2L)));
        when(context.isEnrolled(20L, 4L)).thenReturn(true);
        when(attempts.findByQuizIdAndStudentIdAndStatus(30L, 4L, AttemptStatus.IN_PROGRESS))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.start(30L))
            .isInstanceOfSatisfying(ApiException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATE));
        verify(attempts, never()).saveAndFlush(any());
    }

    @Test
    void submitGradesTwoOfThreeAsSixtySixPointSixtySevenAndStoresBlank() {
        QuizAttempt attempt = attempt(AttemptStatus.IN_PROGRESS);
        when(attempts.findByIdForUpdate(50L)).thenReturn(Optional.of(attempt));
        when(quizzes.findById(30L)).thenReturn(Optional.of(quiz(QuizStatus.CLOSED)));
        when(questions.findByQuizIdOrderByOrderIndexAsc(30L)).thenReturn(questionSet());
        when(options.findByQuestionIdIn(List.of(101L, 102L, 103L))).thenReturn(optionSet());
        when(answers.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.submit(50L, new SubmitAttemptRequest(List.of(
            new SubmitAttemptRequest.Answer("101", "1001"),
            new SubmitAttemptRequest.Answer("102", "1003"),
            new SubmitAttemptRequest.Answer("103", null)
        )));

        assertThat(result.score()).isEqualByComparingTo("66.67");
        assertThat(result.correctCount()).isEqualTo(2);
        assertThat(result.totalQuestions()).isEqualTo(3);
        assertThat(result.review()).hasSize(3);
        assertThat(result.review().get(2).selectedOptionId()).isNull();
        assertThat(result.review().get(2).isCorrect()).isFalse();
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.SUBMITTED);
    }

    @Test
    void submitRejectsDuplicateQuestionWithoutWriting() {
        when(attempts.findByIdForUpdate(50L)).thenReturn(Optional.of(attempt(AttemptStatus.IN_PROGRESS)));
        when(quizzes.findById(30L)).thenReturn(Optional.of(quiz(QuizStatus.PUBLISHED)));
        when(questions.findByQuizIdOrderByOrderIndexAsc(30L)).thenReturn(questionSet());
        when(options.findByQuestionIdIn(List.of(101L, 102L, 103L))).thenReturn(optionSet());

        var request = new SubmitAttemptRequest(List.of(
            new SubmitAttemptRequest.Answer("101", "1001"),
            new SubmitAttemptRequest.Answer("101", "1002")
        ));

        assertThatThrownBy(() -> service.submit(50L, request))
            .isInstanceOfSatisfying(ApiException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_ANSWER));
        verify(answers, never()).saveAll(any());
    }

    @Test
    void submitRejectsQuestionOutsideQuizWithoutWriting() {
        when(attempts.findByIdForUpdate(50L)).thenReturn(Optional.of(attempt(AttemptStatus.IN_PROGRESS)));
        when(quizzes.findById(30L)).thenReturn(Optional.of(quiz(QuizStatus.PUBLISHED)));
        when(questions.findByQuizIdOrderByOrderIndexAsc(30L)).thenReturn(questionSet());
        when(options.findByQuestionIdIn(List.of(101L, 102L, 103L))).thenReturn(optionSet());

        var request = new SubmitAttemptRequest(List.of(
            new SubmitAttemptRequest.Answer("999", "1001")
        ));

        assertThatThrownBy(() -> service.submit(50L, request))
            .isInstanceOfSatisfying(ApiException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_ANSWER));
        verify(answers, never()).saveAll(any());
    }

    @Test
    void submitRejectsOptionFromAnotherQuestionWithoutWriting() {
        when(attempts.findByIdForUpdate(50L)).thenReturn(Optional.of(attempt(AttemptStatus.IN_PROGRESS)));
        when(quizzes.findById(30L)).thenReturn(Optional.of(quiz(QuizStatus.PUBLISHED)));
        when(questions.findByQuizIdOrderByOrderIndexAsc(30L)).thenReturn(questionSet());
        when(options.findByQuestionIdIn(List.of(101L, 102L, 103L))).thenReturn(optionSet());

        var request = new SubmitAttemptRequest(List.of(
            new SubmitAttemptRequest.Answer("101", "1003")
        ));

        assertThatThrownBy(() -> service.submit(50L, request))
            .isInstanceOfSatisfying(ApiException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_ANSWER));
        verify(answers, never()).saveAll(any());
    }

    @Test
    void retryWithDifferentAnswersReturnsStoredResultWithoutRewriting() {
        QuizAttempt submitted = attempt(AttemptStatus.SUBMITTED);
        submitted.setScore(new BigDecimal("66.67"));
        submitted.setCorrectCount(2);
        submitted.setSubmittedAt(Instant.parse("2026-11-14T00:05:00Z"));
        when(attempts.findByIdForUpdate(50L)).thenReturn(Optional.of(submitted));
        when(quizzes.findById(30L)).thenReturn(Optional.of(quiz(QuizStatus.CLOSED)));
        when(questions.findByQuizIdOrderByOrderIndexAsc(30L)).thenReturn(questionSet());
        when(options.findByQuestionIdIn(List.of(101L, 102L, 103L))).thenReturn(optionSet());
        when(answers.findByAttemptIdOrderByQuestionId(50L)).thenReturn(List.of(
            StudentAnswer.builder().attemptId(50L).questionId(101L).selectedOptionId(1001L).isCorrect(true).build(),
            StudentAnswer.builder().attemptId(50L).questionId(102L).selectedOptionId(1003L).isCorrect(true).build(),
            StudentAnswer.builder().attemptId(50L).questionId(103L).selectedOptionId(null).isCorrect(false).build()
        ));

        var result = service.submit(50L, new SubmitAttemptRequest(List.of(
            new SubmitAttemptRequest.Answer("101", "1002")
        )));

        assertThat(result.score()).isEqualByComparingTo("66.67");
        assertThat(result.correctCount()).isEqualTo(2);
        verify(answers, never()).saveAll(any());
    }

    @Test
    void historyUsesAuthenticatedStudentAndBothFilters() {
        var expected = new vn.edu.learningai.dto.response.PageResponse<vn.edu.learningai.dto.response.AttemptSummaryResponse>(
            List.of(), 2, 10, 0L);
        when(history.findMine(4L, 30L, 20L, 2, 10)).thenReturn(expected);
        assertThat(service.history(2, 10, 30L, 20L)).isSameAs(expected);
        verify(history).findMine(4L, 30L, 20L, 2, 10);
    }

    @Test
    void historyRejectsInvalidPaginationWithoutQuerying() {
        for (int[] input : List.of(new int[]{0, 20}, new int[]{-1, 20},
                new int[]{1, 0}, new int[]{1, 101})) {
            assertThatThrownBy(() -> service.history(input[0], input[1], null, null))
                .isInstanceOfSatisfying(ApiException.class,
                    e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        }
        org.mockito.Mockito.verifyNoInteractions(history);
    }

    @Test
    void nonStudentCannotReadHistory() {
        when(security.getCurrentUser()).thenReturn(User.builder().id(2L).role(UserRole.TEACHER).build());
        assertThatThrownBy(() -> service.history(1, 20, null, null))
            .isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        org.mockito.Mockito.verifyNoInteractions(history);
    }

    @Test
    void otherStudentCannotReadOrSubmitAttempt() {
        QuizAttempt other = attempt(AttemptStatus.IN_PROGRESS);
        other.setStudentId(6L);
        when(attempts.findById(50L)).thenReturn(Optional.of(other));
        when(attempts.findByIdForUpdate(50L)).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> service.get(50L)).isInstanceOfSatisfying(ApiException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        assertThatThrownBy(() -> service.submit(50L, new SubmitAttemptRequest(List.of())))
            .isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        org.mockito.Mockito.verifyNoInteractions(answers, quizzes, questions, options);
    }

    @Test
    void courseOwnerAndAdminCanReadSubmittedAttemptWhileOtherTeacherIsForbidden() {
        QuizAttempt submitted = attempt(AttemptStatus.SUBMITTED);
        submitted.setScore(new BigDecimal("66.67"));
        submitted.setCorrectCount(2);
        submitted.setSubmittedAt(Instant.parse("2026-11-14T00:05:00Z"));
        when(attempts.findById(50L)).thenReturn(Optional.of(submitted));
        when(quizzes.findById(30L)).thenReturn(Optional.of(quiz(QuizStatus.PUBLISHED)));
        when(context.findLesson(10L)).thenReturn(Optional.of(new QuizContextRepository.LessonContext(20L, 2L)));
        when(questions.findByQuizIdOrderByOrderIndexAsc(30L)).thenReturn(questionSet());
        when(options.findByQuestionIdIn(List.of(101L, 102L, 103L))).thenReturn(optionSet());
        when(answers.findByAttemptIdOrderByQuestionId(50L)).thenReturn(List.of(
            StudentAnswer.builder().attemptId(50L).questionId(101L).selectedOptionId(1001L).isCorrect(true).build(),
            StudentAnswer.builder().attemptId(50L).questionId(102L).selectedOptionId(1003L).isCorrect(true).build(),
            StudentAnswer.builder().attemptId(50L).questionId(103L).selectedOptionId(null).isCorrect(false).build()
        ));

        // Course owner (teacher id=2) can read result
        when(security.getCurrentUser()).thenReturn(User.builder().id(2L).role(UserRole.TEACHER).isActive(true).build());
        var ownerResult = (vn.edu.learningai.dto.response.AttemptResultResponse) service.get(50L);
        assertThat(ownerResult.score()).isEqualByComparingTo("66.67");
        assertThat(ownerResult.studentId()).isEqualTo("4");

        // Admin (id=1) can read result
        when(security.getCurrentUser()).thenReturn(User.builder().id(1L).role(UserRole.ADMIN).isActive(true).build());
        var adminResult = (vn.edu.learningai.dto.response.AttemptResultResponse) service.get(50L);
        assertThat(adminResult.score()).isEqualByComparingTo("66.67");

        // Another teacher (id=3) is forbidden
        when(security.getCurrentUser()).thenReturn(User.builder().id(3L).role(UserRole.TEACHER).isActive(true).build());
        assertThatThrownBy(() -> service.get(50L))
            .isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    private Quiz quiz(QuizStatus status) {
        return Quiz.builder().id(30L).lessonId(10L).title("Quiz").status(status).createdBy(2L).build();
    }

    private QuizAttempt attempt(AttemptStatus status) {
        return QuizAttempt.builder().id(50L).quizId(30L).studentId(4L).status(status).totalQuestions(3)
            .startedAt(Instant.parse("2026-11-14T00:00:00Z")).build();
    }

    private List<Question> questionSet() {
        return List.of(
            Question.builder().id(101L).quizId(30L).content("Q1").explanation("E1").orderIndex(0).build(),
            Question.builder().id(102L).quizId(30L).content("Q2").explanation("E2").orderIndex(1).build(),
            Question.builder().id(103L).quizId(30L).content("Q3").explanation("E3").orderIndex(2).build()
        );
    }

    private List<QuestionOption> optionSet() {
        return List.of(
            QuestionOption.builder().id(1001L).questionId(101L).content("A").isCorrect(true).orderIndex(0).build(),
            QuestionOption.builder().id(1002L).questionId(101L).content("B").isCorrect(false).orderIndex(1).build(),
            QuestionOption.builder().id(1003L).questionId(102L).content("A").isCorrect(true).orderIndex(0).build(),
            QuestionOption.builder().id(1004L).questionId(102L).content("B").isCorrect(false).orderIndex(1).build(),
            QuestionOption.builder().id(1005L).questionId(103L).content("A").isCorrect(true).orderIndex(0).build(),
            QuestionOption.builder().id(1006L).questionId(103L).content("B").isCorrect(false).orderIndex(1).build()
        );
    }
}
