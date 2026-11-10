package vn.edu.learningai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import vn.edu.learningai.common.enums.QuizStatus;
import vn.edu.learningai.common.enums.UserRole;
import vn.edu.learningai.dto.request.CreateQuizRequest;
import vn.edu.learningai.dto.request.UpdateQuizRequest;
import vn.edu.learningai.dto.request.WriteQuestionRequest;
import vn.edu.learningai.dto.request.QuizPublicationRequest;
import vn.edu.learningai.common.enums.QuestionType;
import vn.edu.learningai.dto.response.PageResponse;
import vn.edu.learningai.dto.response.QuizAuthorResponse;
import vn.edu.learningai.dto.response.QuizSummaryResponse;
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

import java.sql.SQLException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final QuizContextRepository contextRepository;
    private final QuizMapper mapper;
    private final SecurityHelper security;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public QuizAuthorResponse create(Long lessonId, CreateQuizRequest request) {
        User user = security.getCurrentUser();
        var lesson = lesson(lessonId);
        if (!isManager(user, lesson)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        if (request.generationId() != null) {
            validateGeneration(request.generationId(), lessonId, user.getId());
        }
        Quiz quiz = Quiz.builder().lessonId(lessonId).title(request.title())
            .generationId(request.generationId()).createdBy(user.getId()).build();
        return mapper.toAuthor(quizRepository.save(quiz), List.of(), Map.of());
    }

    @Transactional(readOnly = true)
    public PageResponse<QuizSummaryResponse> list(Long lessonId, int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid pagination");
        }
        User user = security.getCurrentUser();
        var lesson = lesson(lessonId);
        boolean manager = isManager(user, lesson);
        if (!manager) {
            requireEnrollment(user, lesson);
        }
        var pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        var quizzes = manager ? quizRepository.findByLessonId(lessonId, pageable)
            : quizRepository.findByLessonIdAndStatus(lessonId, QuizStatus.PUBLISHED, pageable);
        Map<Long, Long> counts = quizzes.isEmpty() ? Map.of() : questionRepository
            .countByQuizIds(quizzes.getContent().stream().map(Quiz::getId).toList()).stream()
            .collect(Collectors.toMap(QuestionRepository.QuizQuestionCount::getQuizId,
                QuestionRepository.QuizQuestionCount::getQuestionCount));
        return PageResponse.from(quizzes.map(quiz -> mapper.toSummary(quiz, counts.getOrDefault(quiz.getId(), 0L))));
    }

    @Transactional(readOnly = true)
    public Object get(Long quizId) {
        User user = security.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Quiz not found"));
        var lesson = lesson(quiz.getLessonId());
        boolean manager = isManager(user, lesson);
        if (!manager) {
            if (user.getRole() == UserRole.STUDENT && quiz.getStatus() != QuizStatus.PUBLISHED) {
                throw new ApiException(ErrorCode.NOT_FOUND, "Quiz not found");
            }
            requireEnrollment(user, lesson);
        }
        List<Question> questions = questionRepository.findByQuizIdOrderByOrderIndexAsc(quizId);
        Map<Long, List<QuestionOption>> options = loadOptions(questions);
        return manager ? mapper.toAuthor(quiz, questions, options) : mapper.toStudent(quiz, questions, options);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public QuizAuthorResponse update(Long quizId, UpdateQuizRequest request) {
        User user = security.getCurrentUser();
        Quiz quiz = managedDraft(quizId, user);
        if (request.generationId() != null) {
            validateGeneration(request.generationId(), quiz.getLessonId(), user.getId());
        }
        if (request.title() != null) {
            quiz.setTitle(request.title());
        }
        if (request.generationId() != null) {
            quiz.setGenerationId(request.generationId());
        }
        quizRepository.flush();
        List<Question> questions = questionRepository.findByQuizIdOrderByOrderIndexAsc(quizId);
        Map<Long, List<QuestionOption>> options = loadOptions(questions);
        return mapper.toAuthor(quiz, questions, options);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void delete(Long quizId) {
        User user = security.getCurrentUser();
        Quiz quiz = managedDraft(quizId, user);
        if (contextRepository.hasLearningHistory(quizId)) {
            throw new ApiException(ErrorCode.RESOURCE_IN_USE, "Quiz has learning history");
        }
        try {
            List<Question> questions = questionRepository.findByQuizIdOrderByOrderIndexAsc(quizId);
            for (Question question : questions) {
                optionRepository.deleteByQuestionId(question.getId());
            }
            optionRepository.flush();
            questionRepository.deleteAll(questions);
            questionRepository.flush();
            quizRepository.delete(quiz);
            quizRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof SQLException sql && sql.getErrorCode() == 1451) {
                    throw new ApiException(ErrorCode.RESOURCE_IN_USE, "Quiz content is referenced by learning history");
                }
            }
            throw exception;
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public QuizAuthorResponse.QuestionAuthor addQuestion(Long quizId, WriteQuestionRequest request) {
        User user = security.getCurrentUser();
        Quiz quiz = managedDraft(quizId, user);
        requireNoHistory(quizId);
        request.validate();
        if (questionRepository.existsByQuizIdAndOrderIndex(quizId, request.orderIndex())) {
            throw new ApiException(ErrorCode.INVALID_STATE, "Question order_index already exists");
        }
        if (request.generationId() != null) {
            validateGeneration(request.generationId(), quiz.getLessonId(), user.getId());
        }
        Question question = Question.builder().quizId(quizId).build();
        applyQuestion(question, request);
        questionRepository.saveAndFlush(question);
        return mapper.toAuthorQuestion(question, saveOptions(question.getId(), request));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public QuizAuthorResponse.QuestionAuthor replaceQuestion(Long questionId, WriteQuestionRequest request) {
        User user = security.getCurrentUser();
        Long quizId = questionRepository.findQuizIdById(questionId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Question not found"));
        Quiz quiz = managedDraft(quizId, user);
        Question question = questionRepository.findByIdAndQuizIdForUpdate(questionId, quizId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Question not found"));
        requireNoHistory(quizId);
        request.validate();
        if (questionRepository.existsByQuizIdAndOrderIndexAndIdNot(quizId, request.orderIndex(), questionId)) {
            throw new ApiException(ErrorCode.INVALID_STATE, "Question order_index already exists");
        }
        if (request.generationId() != null) {
            validateGeneration(request.generationId(), quiz.getLessonId(), user.getId());
        }
        applyQuestion(question, request);
        optionRepository.deleteByQuestionId(questionId);
        optionRepository.flush();
        questionRepository.flush();
        return mapper.toAuthorQuestion(question, saveOptions(questionId, request));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deleteQuestion(Long questionId) {
        User user = security.getCurrentUser();
        Long quizId = questionRepository.findQuizIdById(questionId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Question not found"));
        managedDraft(quizId, user);
        Question question = questionRepository.findByIdAndQuizIdForUpdate(questionId, quizId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Question not found"));
        requireNoHistory(quizId);
        optionRepository.deleteByQuestionId(questionId);
        optionRepository.flush();
        questionRepository.delete(question);
        questionRepository.flush();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public QuizAuthorResponse changePublication(Long quizId, QuizPublicationRequest request) {
        User user = security.getCurrentUser();
        Quiz quiz = managedQuiz(quizId, user);
        QuizStatus target = request.status();
        boolean publishing = quiz.getStatus() == QuizStatus.DRAFT && target == QuizStatus.PUBLISHED;
        boolean closing = quiz.getStatus() == QuizStatus.PUBLISHED && target == QuizStatus.CLOSED;
        if (!publishing && !closing) {
            throw new ApiException(ErrorCode.INVALID_STATE, "Only DRAFT to PUBLISHED or PUBLISHED to CLOSED is allowed");
        }
        List<Question> questions = questionRepository.findByQuizIdOrderByOrderIndexAsc(quizId);
        Map<Long, List<QuestionOption>> options = loadOptions(questions);
        if (publishing) {
            requireNoHistory(quizId);
            validateForPublication(quiz, questions, options);
            quiz.setPublishedAt(Instant.now());
        } else {
            quiz.setClosedAt(Instant.now());
        }
        quiz.setStatus(target);
        quizRepository.flush();
        return mapper.toAuthor(quiz, questions, options);
    }

    private void validateForPublication(Quiz quiz, List<Question> questions, Map<Long, List<QuestionOption>> options) {
        if (quiz.getTitle() == null || quiz.getTitle().isBlank()
            || quiz.getTitle().codePointCount(0, quiz.getTitle().length()) > 200 || questions.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_QUIZ);
        }
        var positions = new HashSet<Integer>();
        for (Question question : questions) {
            if (question.getQuestionType() != QuestionType.MULTIPLE_CHOICE || !positions.add(question.getOrderIndex())) {
                throw new ApiException(ErrorCode.INVALID_QUIZ);
            }
            var values = options.get(question.getId()).stream().map(option ->
                new WriteQuestionRequest.Option(option.getContent(), option.getIsCorrect(), option.getOrderIndex())).toList();
            try {
                new WriteQuestionRequest(question.getContent(), question.getExplanation(), question.getOrderIndex(),
                    values, question.getGenerationId()).validate();
            } catch (ApiException exception) {
                throw new ApiException(ErrorCode.INVALID_QUIZ, exception.getMessage());
            }
        }
    }

    private void requireNoHistory(Long quizId) {
        if (contextRepository.hasLearningHistory(quizId)) {
            throw new ApiException(ErrorCode.RESOURCE_IN_USE, "Quiz has learning history");
        }
    }

    private void applyQuestion(Question question, WriteQuestionRequest request) {
        question.setContent(request.content());
        question.setExplanation(request.explanation());
        question.setOrderIndex(request.orderIndex());
        if (request.generationId() != null) {
            question.setGenerationId(request.generationId());
        }
        question.setGeneratedByAi(question.getGenerationId() != null);
    }

    private List<QuestionOption> saveOptions(Long questionId, WriteQuestionRequest request) {
        return optionRepository.saveAllAndFlush(request.options().stream().map(option -> QuestionOption.builder()
            .questionId(questionId).content(option.content()).isCorrect(option.isCorrect())
            .orderIndex(option.orderIndex()).build()).toList());
    }

    private Map<Long, List<QuestionOption>> loadOptions(List<Question> questions) {
        Map<Long, List<QuestionOption>> options = questions.stream()
            .collect(Collectors.toMap(Question::getId, question -> new java.util.ArrayList<>()));
        if (!questions.isEmpty()) {
            for (QuestionOption option : optionRepository.findByQuestionIdIn(
                questions.stream().map(Question::getId).toList())) {
                options.get(option.getQuestionId()).add(option);
            }
        }
        return options;
    }

    private Quiz managedDraft(Long quizId, User user) {
        Quiz quiz = managedQuiz(quizId, user);
        if (quiz.getStatus() != QuizStatus.DRAFT) {
            throw new ApiException(ErrorCode.INVALID_STATE, "Only DRAFT quizzes can be changed");
        }
        return quiz;
    }

    private Quiz managedQuiz(Long quizId, User user) {
        Quiz quiz = quizRepository.findByIdForUpdate(quizId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Quiz not found"));
        if (!isManager(user, lesson(quiz.getLessonId()))) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        return quiz;
    }

    private QuizContextRepository.LessonContext lesson(Long lessonId) {
        return contextRepository.findLesson(lessonId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Lesson not found"));
    }

    private boolean isManager(User user, QuizContextRepository.LessonContext lesson) {
        return user.getRole() == UserRole.ADMIN
            || user.getRole() == UserRole.TEACHER && user.getId().equals(lesson.teacherId());
    }

    private void requireEnrollment(User user, QuizContextRepository.LessonContext lesson) {
        if (user.getRole() != UserRole.STUDENT) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        if (!contextRepository.isEnrolled(lesson.courseId(), user.getId())) {
            throw new ApiException(ErrorCode.ENROLLMENT_REQUIRED);
        }
    }

    private void validateGeneration(Long generationId, Long lessonId, Long userId) {
        var generation = contextRepository.findGeneration(generationId)
            .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_ERROR, "Generation not found"));
        if (!userId.equals(generation.createdBy())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Generation belongs to another user");
        }
        if (!lessonId.equals(generation.lessonId()) || !"QUIZ".equals(generation.type())
            || !"SUCCEEDED".equals(generation.status())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Generation is not a successful quiz generation for this lesson");
        }
    }
}
