package vn.edu.learningai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.learningai.common.enums.AttemptStatus;
import vn.edu.learningai.common.enums.QuizStatus;
import vn.edu.learningai.common.enums.LessonStatus;
import vn.edu.learningai.common.enums.UserRole;
import vn.edu.learningai.dto.request.SubmitAttemptRequest;
import vn.edu.learningai.dto.response.AttemptResponse;
import vn.edu.learningai.dto.response.AttemptSummaryResponse;
import vn.edu.learningai.dto.response.PageResponse;
import vn.edu.learningai.repository.AttemptHistoryRepository;
import vn.edu.learningai.dto.response.AttemptResultResponse;
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
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttemptService {
    private final QuizAttemptRepository attemptRepository;
    private final StudentAnswerRepository answerRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final QuizContextRepository contextRepository;
    private final SecurityHelper security;
    private final AttemptHistoryRepository historyRepository;

    @Transactional(readOnly = true)
    public PageResponse<AttemptSummaryResponse> history(
            int page, int pageSize, Long quizId, Long courseId) {
        User student = currentStudent();
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                "page must be >= 1 and page_size must be between 1 and 100");
        }
        return historyRepository.findMine(student.getId(), quizId, courseId, page, pageSize);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public StartResult start(Long quizId) {
        User student = currentStudent();
        Quiz quiz = quizRepository.findByIdForUpdate(quizId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Quiz not found"));
        var lesson = contextRepository.findLesson(quiz.getLessonId())
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Lesson not found"));
        if (!contextRepository.isEnrolled(lesson.courseId(), student.getId())) {
            throw new ApiException(ErrorCode.ENROLLMENT_REQUIRED);
        }
        var existing = attemptRepository.findByQuizIdAndStudentIdAndStatus(
            quizId, student.getId(), AttemptStatus.IN_PROGRESS);
        if (existing.isPresent()) {
            return new StartResult(toAttempt(existing.get()), false);
        }
        if (lesson.status() != LessonStatus.PUBLISHED) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Lesson not found");
        }
        if (quiz.getStatus() != QuizStatus.PUBLISHED) {
            throw new ApiException(ErrorCode.INVALID_STATE, "Only PUBLISHED quizzes can start new attempts");
        }
        long questionCount = questionRepository.countByQuizId(quizId);
        if (questionCount < 1 || questionCount > Integer.MAX_VALUE) {
            throw new ApiException(ErrorCode.INVALID_STATE, "Quiz has no questions");
        }
        QuizAttempt attempt = QuizAttempt.builder()
            .quizId(quizId)
            .studentId(student.getId())
            .status(AttemptStatus.IN_PROGRESS)
            .totalQuestions((int) questionCount)
            .build();
        return new StartResult(toAttempt(attemptRepository.saveAndFlush(attempt)), true);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AttemptResultResponse submit(Long attemptId, SubmitAttemptRequest request) {
        User student = currentStudent();
        QuizAttempt attempt = ownedAttemptForUpdate(attemptId, student.getId());
        Quiz quiz = quizRepository.findById(attempt.getQuizId())
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Quiz not found"));
        List<Question> questions = questionRepository.findByQuizIdOrderByOrderIndexAsc(quiz.getId());
        Map<Long, List<QuestionOption>> optionsByQuestion = loadOptions(questions);
        if (attempt.getStatus() == AttemptStatus.SUBMITTED) {
            return storedResult(attempt, quiz, questions, optionsByQuestion);
        }
        Map<Long, Long> selected = validateAnswers(request, questions, optionsByQuestion);
        int correctCount = 0;
        var storedAnswers = new java.util.ArrayList<StudentAnswer>(questions.size());
        for (Question question : questions) {
            Long optionId = selected.get(question.getId());
            boolean correct = optionId != null && optionsByQuestion.get(question.getId()).stream()
                .anyMatch(option -> option.getId().equals(optionId) && Boolean.TRUE.equals(option.getIsCorrect()));
            if (correct) correctCount++;
            storedAnswers.add(StudentAnswer.builder()
                .attemptId(attempt.getId())
                .questionId(question.getId())
                .selectedOptionId(optionId)
                .isCorrect(correct)
                .build());
        }
        answerRepository.saveAll(storedAnswers);
        answerRepository.flush();
        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setTotalQuestions(questions.size());
        attempt.setCorrectCount(correctCount);
        attempt.setScore(BigDecimal.valueOf(correctCount)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(questions.size()), 2, RoundingMode.HALF_UP));
        attempt.setSubmittedAt(Instant.now());
        attemptRepository.flush();
        return toResult(attempt, quiz, questions, optionsByQuestion, storedAnswers);
    }

    @Transactional(readOnly = true)
    public Object get(Long attemptId) {
        User student = currentStudent();
        QuizAttempt attempt = attemptRepository.findById(attemptId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Attempt not found"));
        requireOwner(attempt, student.getId());
        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
            return toAttempt(attempt);
        }
        Quiz quiz = quizRepository.findById(attempt.getQuizId())
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Quiz not found"));
        List<Question> questions = questionRepository.findByQuizIdOrderByOrderIndexAsc(quiz.getId());
        return storedResult(attempt, quiz, questions, loadOptions(questions));
    }

    private AttemptResultResponse storedResult(QuizAttempt attempt, Quiz quiz, List<Question> questions,
                                                Map<Long, List<QuestionOption>> optionsByQuestion) {
        List<StudentAnswer> answers = answerRepository.findByAttemptIdOrderByQuestionId(attempt.getId());
        return toResult(attempt, quiz, questions, optionsByQuestion, answers);
    }

    private Map<Long, Long> validateAnswers(SubmitAttemptRequest request, List<Question> questions,
                                            Map<Long, List<QuestionOption>> optionsByQuestion) {
        Map<Long, Question> questionById = questions.stream()
            .collect(Collectors.toMap(Question::getId, Function.identity()));
        Map<Long, Long> selected = new HashMap<>();
        var seen = new HashSet<Long>();
        for (SubmitAttemptRequest.Answer answer : request.answers()) {
            Long questionId = parseId(answer.questionId(), "question_id");
            if (!seen.add(questionId)) {
                throw new ApiException(ErrorCode.INVALID_ANSWER, "Duplicate question_id");
            }
            if (!questionById.containsKey(questionId)) {
                throw new ApiException(ErrorCode.INVALID_ANSWER, "Question does not belong to the attempt quiz");
            }
            Long optionId = answer.selectedOptionId() == null ? null
                : parseId(answer.selectedOptionId(), "selected_option_id");
            if (optionId != null && optionsByQuestion.get(questionId).stream()
                .noneMatch(option -> option.getId().equals(optionId))) {
                throw new ApiException(ErrorCode.INVALID_ANSWER, "Option does not belong to the question");
            }
            selected.put(questionId, optionId);
        }
        return selected;
    }

    private AttemptResultResponse toResult(QuizAttempt attempt, Quiz quiz, List<Question> questions,
                                           Map<Long, List<QuestionOption>> optionsByQuestion,
                                           List<StudentAnswer> answers) {
        Map<Long, StudentAnswer> answersByQuestion = answers.stream()
            .collect(Collectors.toMap(StudentAnswer::getQuestionId, Function.identity()));
        List<AttemptResultResponse.Review> review = questions.stream().map(question -> {
            StudentAnswer answer = answersByQuestion.get(question.getId());
            List<QuestionOption> options = optionsByQuestion.get(question.getId());
            QuestionOption correct = options.stream().filter(option -> Boolean.TRUE.equals(option.getIsCorrect()))
                .findFirst().orElseThrow(() -> new ApiException(ErrorCode.INVALID_STATE, "Question has no correct option"));
            return new AttemptResultResponse.Review(
                question.getId().toString(),
                question.getContent(),
                answer == null || answer.getSelectedOptionId() == null ? null : answer.getSelectedOptionId().toString(),
                answer != null && Boolean.TRUE.equals(answer.getIsCorrect()),
                correct.getId().toString(),
                question.getExplanation(),
                options.stream().map(option -> new AttemptResultResponse.Option(
                    option.getId().toString(), option.getContent())).toList()
            );
        }).toList();
        return new AttemptResultResponse(
            attempt.getId().toString(),
            quiz.getId().toString(),
            quiz.getTitle(),
            attempt.getStudentId().toString(),
            attempt.getStatus(),
            attempt.getScore(),
            attempt.getTotalQuestions(),
            attempt.getCorrectCount(),
            attempt.getStartedAt(),
            attempt.getSubmittedAt(),
            review
        );
    }

    private Map<Long, List<QuestionOption>> loadOptions(List<Question> questions) {
        if (questions.isEmpty()) return Map.of();
        Map<Long, List<QuestionOption>> grouped = optionRepository.findByQuestionIdIn(
                questions.stream().map(Question::getId).toList()).stream()
            .sorted(Comparator.comparing(QuestionOption::getOrderIndex))
            .collect(Collectors.groupingBy(QuestionOption::getQuestionId));
        for (Question question : questions) {
            grouped.putIfAbsent(question.getId(), List.of());
        }
        return grouped;
    }

    private QuizAttempt ownedAttemptForUpdate(Long attemptId, Long studentId) {
        QuizAttempt attempt = attemptRepository.findByIdForUpdate(attemptId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Attempt not found"));
        requireOwner(attempt, studentId);
        return attempt;
    }

    private void requireOwner(QuizAttempt attempt, Long studentId) {
        if (!attempt.getStudentId().equals(studentId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Attempt not found");
        }
    }

    private User currentStudent() {
        User user = security.getCurrentUser();
        if (user.getRole() != UserRole.STUDENT) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        return user;
    }

    private AttemptResponse toAttempt(QuizAttempt attempt) {
        return new AttemptResponse(
            attempt.getId().toString(),
            attempt.getQuizId().toString(),
            attempt.getStudentId().toString(),
            attempt.getStatus(),
            attempt.getTotalQuestions(),
            attempt.getStartedAt()
        );
    }

    private Long parseId(String value, String field) {
        try {
            return Long.valueOf(value);
        } catch (RuntimeException exception) {
            throw new ApiException(ErrorCode.INVALID_ANSWER, field + " is invalid");
        }
    }

    public record StartResult(AttemptResponse attempt, boolean created) {}
}
