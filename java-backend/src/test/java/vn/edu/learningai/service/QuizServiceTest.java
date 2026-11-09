package vn.edu.learningai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockMakers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.learningai.common.enums.QuizStatus;
import vn.edu.learningai.common.enums.UserRole;
import vn.edu.learningai.dto.request.CreateQuizRequest;
import vn.edu.learningai.dto.request.UpdateQuizRequest;
import vn.edu.learningai.dto.response.QuizAuthorResponse;
import vn.edu.learningai.dto.response.QuizStudentResponse;
import vn.edu.learningai.entity.Question;
import vn.edu.learningai.entity.QuestionOption;
import vn.edu.learningai.entity.Quiz;
import vn.edu.learningai.entity.User;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.mapper.QuizMapper;
import vn.edu.learningai.repository.QuestionOptionRepository;
import vn.edu.learningai.repository.QuestionRepository;
import vn.edu.learningai.repository.QuizContextRepository;
import vn.edu.learningai.repository.QuizRepository;
import vn.edu.learningai.security.SecurityHelper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {
    @Mock QuizRepository quizzes;
    @Mock QuestionRepository questions;
    @Mock QuestionOptionRepository options;
    @Mock(mockMaker = MockMakers.SUBCLASS) QuizContextRepository context;
    @Mock(mockMaker = MockMakers.SUBCLASS) SecurityHelper security;
    QuizService service;

    @BeforeEach
    void setUp() {
        service = new QuizService(quizzes, questions, options, context, new QuizMapper(), security);
    }

    @Test
    void ownerCreatesEmptyDraftWithAuthenticatedCreator() {
        actor(7L, UserRole.TEACHER);
        lesson();
        when(quizzes.save(any())).thenAnswer(invocation -> {
            Quiz quiz = invocation.getArgument(0);
            quiz.setId(30L);
            quiz.setCreatedAt(Instant.parse("2026-11-09T00:00:00Z"));
            return quiz;
        });
        var result = service.create(10L, new CreateQuizRequest("Java", null));
        assertThat(result.id()).isEqualTo("30");
        assertThat(result.status()).isEqualTo(QuizStatus.DRAFT);
        assertThat(result.questions()).isEmpty();
        var saved = ArgumentCaptor.forClass(Quiz.class);
        verify(quizzes).save(saved.capture());
        assertThat(saved.getValue().getCreatedBy()).isEqualTo(7L);
        assertThat(saved.getValue().getLessonId()).isEqualTo(10L);
        assertThat(saved.getValue().getGenerationId()).isNull();
        verify(context, never()).findGeneration(any());
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"TEACHER", "STUDENT"})
    void nonOwnerCannotCreate(UserRole role) {
        actor(8L, role);
        lesson();
        fails(ErrorCode.FORBIDDEN, () -> service.create(10L, new CreateQuizRequest("Java", null)));
        verifyNoInteractions(quizzes);
    }

    @Test
    void adminCanCreateUsingOwnGeneration() {
        actor(8L, UserRole.ADMIN);
        lesson();
        when(context.findGeneration(40L)).thenReturn(Optional.of(
            new QuizContextRepository.GenerationContext(10L, 8L, "QUIZ", "SUCCEEDED")));
        when(quizzes.save(any())).thenAnswer(invocation -> {
            Quiz quiz = invocation.getArgument(0);
            quiz.setId(30L);
            return quiz;
        });
        service.create(10L, new CreateQuizRequest("Java", 40L));
        var saved = ArgumentCaptor.forClass(Quiz.class);
        verify(quizzes).save(saved.capture());
        assertThat(saved.getValue().getGenerationId()).isEqualTo(40L);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"TEACHER", "ADMIN"})
    void evenAdminCannotUseAnotherActorsGeneration(UserRole role) {
        actor(7L, role);
        lesson();
        when(context.findGeneration(40L)).thenReturn(Optional.of(
            new QuizContextRepository.GenerationContext(10L, 8L, "QUIZ", "SUCCEEDED")));
        fails(ErrorCode.FORBIDDEN, () -> service.create(10L, new CreateQuizRequest("Java", 40L)));
        verifyNoInteractions(quizzes);
    }

    @ParameterizedTest
    @CsvSource({"11, QUIZ, SUCCEEDED", "10, FLASHCARD, SUCCEEDED", "10, QUIZ, FAILED"})
    void invalidGenerationIsRejected(long lessonId, String type, String status) {
        actor(7L, UserRole.TEACHER);
        lesson();
        when(context.findGeneration(40L)).thenReturn(Optional.of(
            new QuizContextRepository.GenerationContext(lessonId, 7L, type, status)));
        fails(ErrorCode.VALIDATION_ERROR, () -> service.create(10L, new CreateQuizRequest("Java", 40L)));
        verifyNoInteractions(quizzes);
    }

    @Test
    void missingGenerationIsValidationError() {
        actor(7L, UserRole.TEACHER);
        lesson();
        when(context.findGeneration(40L)).thenReturn(Optional.empty());
        fails(ErrorCode.VALIDATION_ERROR, () -> service.create(10L, new CreateQuizRequest("Java", 40L)));
    }

    @Test
    void missingLessonIsNotFound() {
        actor(7L, UserRole.TEACHER);
        fails(ErrorCode.NOT_FOUND, () -> service.create(10L, new CreateQuizRequest("Java", null)));
    }

    @ParameterizedTest
    @EnumSource(QuizStatus.class)
    void courseOwnerReadsAuthorViewEvenWhenSomeoneElseCreatedQuiz(QuizStatus status) {
        actor(7L, UserRole.TEACHER);
        lesson();
        detail(status);
        assertThat(service.get(30L)).isInstanceOf(QuizAuthorResponse.class);
    }

    @Test
    void adminReadsDraftFromAnotherTeachersCourse() {
        actor(8L, UserRole.ADMIN);
        lesson();
        detail(QuizStatus.DRAFT);
        assertThat(service.get(30L)).isInstanceOf(QuizAuthorResponse.class);
    }

    @Test
    void anotherTeacherCannotReadQuiz() {
        actor(8L, UserRole.TEACHER);
        lesson();
        detail(QuizStatus.PUBLISHED);
        fails(ErrorCode.FORBIDDEN, () -> service.get(30L));
        verifyNoInteractions(questions, options);
    }

    @ParameterizedTest
    @EnumSource(value = QuizStatus.class, names = {"DRAFT", "CLOSED"})
    void studentCannotDiscoverNonPublishedQuiz(QuizStatus status) {
        actor(8L, UserRole.STUDENT);
        lesson();
        detail(status);
        fails(ErrorCode.NOT_FOUND, () -> service.get(30L));
        verify(context, never()).isEnrolled(any(), any());
        verifyNoInteractions(questions, options);
    }

    @Test
    void publishedQuizRequiresEnrollment() {
        actor(8L, UserRole.STUDENT);
        lesson();
        detail(QuizStatus.PUBLISHED);
        fails(ErrorCode.ENROLLMENT_REQUIRED, () -> service.get(30L));
        verifyNoInteractions(questions, options);
    }

    @Test
    void enrolledStudentGetsNoAnswersExplanationOrProvenance() {
        actor(8L, UserRole.STUDENT);
        lesson();
        detail(QuizStatus.PUBLISHED);
        when(context.isEnrolled(20L, 8L)).thenReturn(true);
        var question = Question.builder().id(50L).quizId(30L).content("Question")
            .explanation("Secret explanation").generatedByAi(true).orderIndex(0).build();
        var option = QuestionOption.builder().id(60L).questionId(50L).content("Option")
            .isCorrect(true).orderIndex(0).build();
        when(questions.findByQuizIdOrderByOrderIndexAsc(30L)).thenReturn(List.of(question));
        when(options.findByQuestionIdIn(List.of(50L))).thenReturn(List.of(option));
        Object result = service.get(30L);
        assertThat(result).isInstanceOf(QuizStudentResponse.class);
        String body = JsonMapper.builder().build().writeValueAsString(result);
        assertThat(body).contains("Option").doesNotContain("is_correct", "explanation", "generation", "generated_by_ai");
    }

    @Test
    void missingQuizIsNotFound() {
        actor(8L, UserRole.STUDENT);
        fails(ErrorCode.NOT_FOUND, () -> service.get(30L));
    }

    @Test
    void ownerListUsesStableSortAndOneBasedPagination() {
        actor(7L, UserRole.TEACHER);
        lesson();
        when(quizzes.findByLessonId(eq(10L), any())).thenAnswer(invocation ->
            new PageImpl<>(List.of(), invocation.getArgument(1), 0));
        var result = service.list(10L, 2, 20);
        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(quizzes).findByLessonId(eq(10L), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getSort().toString()).isEqualTo("createdAt: DESC,id: DESC");
        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getItems()).isEmpty();
        verifyNoInteractions(questions);
    }

    @Test
    void studentListFiltersBeforePaginationAndCountsQuestionsInBatch() {
        actor(8L, UserRole.STUDENT);
        lesson();
        when(context.isEnrolled(20L, 8L)).thenReturn(true);
        Quiz quiz = quiz(QuizStatus.PUBLISHED);
        when(quizzes.findByLessonIdAndStatus(eq(10L), eq(QuizStatus.PUBLISHED), any()))
            .thenAnswer(invocation -> new PageImpl<>(List.of(quiz), invocation.getArgument(2), 1));
        var count = mock(QuestionRepository.QuizQuestionCount.class);
        when(count.getQuizId()).thenReturn(30L);
        when(count.getQuestionCount()).thenReturn(3L);
        when(questions.countByQuizIds(List.of(30L))).thenReturn(List.of(count));
        var result = service.list(10L, 1, 20);
        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getItems().get(0).questionCount()).isEqualTo(3L);
        verify(quizzes, never()).findByLessonId(any(), any());
    }

    @Test
    void unenrolledStudentCannotList() {
        actor(8L, UserRole.STUDENT);
        lesson();
        fails(ErrorCode.ENROLLMENT_REQUIRED, () -> service.list(10L, 1, 20));
        verifyNoInteractions(quizzes);
    }

    @ParameterizedTest
    @CsvSource({"0,20", "-1,20", "1,0", "1,101"})
    void invalidPaginationIsRejected(int page, int pageSize) {
        fails(ErrorCode.VALIDATION_ERROR, () -> service.list(10L, page, pageSize));
        verifyNoInteractions(quizzes, context);
    }

    @Test
    void titleOnlyUpdateRetainsProvenanceAndReturnsQuestions() {
        actor(7L, UserRole.TEACHER);
        lesson();
        Quiz draft = quiz(QuizStatus.DRAFT);
        draft.setGenerationId(40L);
        when(quizzes.findByIdForUpdate(30L)).thenReturn(Optional.of(draft));
        var question = Question.builder().id(50L).quizId(30L).content("Question").orderIndex(0).build();
        when(questions.findByQuizIdOrderByOrderIndexAsc(30L)).thenReturn(List.of(question));
        when(options.findByQuestionIdIn(List.of(50L))).thenReturn(List.of());
        var result = service.update(30L, new UpdateQuizRequest("Updated", null));
        assertThat(result.title()).isEqualTo("Updated");
        assertThat(result.questions()).hasSize(1);
        assertThat(draft.getGenerationId()).isEqualTo(40L);
        verify(context, never()).findGeneration(any());
        verify(quizzes).flush();
        verify(quizzes, never()).findById(any());
    }

    @Test
    void adminUpdatesProvenanceOnlyWithOwnValidGeneration() {
        actor(8L, UserRole.ADMIN);
        lesson();
        Quiz draft = quiz(QuizStatus.DRAFT);
        when(quizzes.findByIdForUpdate(30L)).thenReturn(Optional.of(draft));
        when(context.findGeneration(40L)).thenReturn(Optional.of(
            new QuizContextRepository.GenerationContext(10L, 8L, "QUIZ", "SUCCEEDED")));
        service.update(30L, new UpdateQuizRequest(null, 40L));
        assertThat(draft.getTitle()).isEqualTo("Java");
        assertThat(draft.getGenerationId()).isEqualTo(40L);
    }

    @Test
    void invalidGenerationDoesNotPartiallyChangeTitle() {
        actor(7L, UserRole.TEACHER);
        lesson();
        Quiz draft = quiz(QuizStatus.DRAFT);
        when(quizzes.findByIdForUpdate(30L)).thenReturn(Optional.of(draft));
        fails(ErrorCode.VALIDATION_ERROR, () -> service.update(30L, new UpdateQuizRequest("Changed", 40L)));
        assertThat(draft.getTitle()).isEqualTo("Java");
        verify(quizzes, never()).flush();
    }

    @ParameterizedTest
    @EnumSource(value = QuizStatus.class, names = {"PUBLISHED", "CLOSED"})
    void publishedAndClosedQuizzesCannotBeUpdatedOrDeleted(QuizStatus status) {
        actor(7L, UserRole.TEACHER);
        lesson();
        when(quizzes.findByIdForUpdate(30L)).thenReturn(Optional.of(quiz(status)));
        fails(ErrorCode.INVALID_STATE, () -> service.update(30L, new UpdateQuizRequest("Changed", null)));
        fails(ErrorCode.INVALID_STATE, () -> service.delete(30L));
        verifyNoInteractions(questions, options);
        verify(quizzes, never()).delete(any());
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"TEACHER", "STUDENT"})
    void nonOwnerCannotMutateDraft(UserRole role) {
        actor(8L, role);
        lesson();
        when(quizzes.findByIdForUpdate(30L)).thenReturn(Optional.of(quiz(QuizStatus.DRAFT)));
        fails(ErrorCode.FORBIDDEN, () -> service.update(30L, new UpdateQuizRequest("Changed", null)));
        fails(ErrorCode.FORBIDDEN, () -> service.delete(30L));
        verifyNoInteractions(questions, options);
    }

    @Test
    void missingDraftReturns404ForUpdateAndRepeatedDelete() {
        actor(7L, UserRole.TEACHER);
        fails(ErrorCode.NOT_FOUND, () -> service.update(30L, new UpdateQuizRequest("Changed", null)));
        fails(ErrorCode.NOT_FOUND, () -> service.delete(30L));
    }

    @Test
    void draftWithHistoryCannotBeDeleted() {
        actor(7L, UserRole.TEACHER);
        lesson();
        when(quizzes.findByIdForUpdate(30L)).thenReturn(Optional.of(quiz(QuizStatus.DRAFT)));
        when(context.hasLearningHistory(30L)).thenReturn(true);
        fails(ErrorCode.RESOURCE_IN_USE, () -> service.delete(30L));
        verifyNoInteractions(questions, options);
        verify(quizzes, never()).delete(any());
    }

    @Test
    void deleteFlushesOptionsBeforeQuestionsBeforeQuiz() {
        actor(8L, UserRole.ADMIN);
        lesson();
        Quiz draft = quiz(QuizStatus.DRAFT);
        when(quizzes.findByIdForUpdate(30L)).thenReturn(Optional.of(draft));
        var children = List.of(Question.builder().id(50L).quizId(30L).build());
        when(questions.findByQuizIdOrderByOrderIndexAsc(30L)).thenReturn(children);
        service.delete(30L);
        var order = inOrder(quizzes, context, questions, options);
        order.verify(quizzes).findByIdForUpdate(30L);
        order.verify(context).findLesson(10L);
        order.verify(context).hasLearningHistory(30L);
        order.verify(questions).findByQuizIdOrderByOrderIndexAsc(30L);
        order.verify(options).deleteByQuestionId(50L);
        order.verify(options).flush();
        order.verify(questions).deleteAll(children);
        order.verify(questions).flush();
        order.verify(quizzes).delete(draft);
        order.verify(quizzes).flush();
    }

    @Test
    void foreignKeyDeleteConflictBecomesResourceInUse() {
        actor(7L, UserRole.TEACHER);
        lesson();
        when(quizzes.findByIdForUpdate(30L)).thenReturn(Optional.of(quiz(QuizStatus.DRAFT)));
        doThrow(new org.springframework.dao.DataIntegrityViolationException("FK",
            new java.sql.SQLException("Referenced", "23000", 1451))).when(quizzes).flush();
        fails(ErrorCode.RESOURCE_IN_USE, () -> service.delete(30L));
    }

    @Test
    void unrelatedDatabaseFailureIsNotDisguisedAsHistoryConflict() {
        actor(7L, UserRole.TEACHER);
        lesson();
        when(quizzes.findByIdForUpdate(30L)).thenReturn(Optional.of(quiz(QuizStatus.DRAFT)));
        var failure = new org.springframework.dao.DataIntegrityViolationException("Other failure");
        doThrow(failure).when(quizzes).flush();
        assertThatThrownBy(() -> service.delete(30L)).isSameAs(failure);
    }

    private void actor(Long id, UserRole role) {
        when(security.getCurrentUser()).thenReturn(User.builder().id(id).role(role).build());
    }

    private void lesson() {
        when(context.findLesson(10L)).thenReturn(Optional.of(new QuizContextRepository.LessonContext(20L, 7L)));
    }

    private void detail(QuizStatus status) {
        when(quizzes.findById(30L)).thenReturn(Optional.of(quiz(status)));
    }

    private Quiz quiz(QuizStatus status) {
        return Quiz.builder().id(30L).lessonId(10L).title("Java").createdBy(99L).status(status).build();
    }

    private void fails(ErrorCode code, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ApiException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(code));
    }
}
