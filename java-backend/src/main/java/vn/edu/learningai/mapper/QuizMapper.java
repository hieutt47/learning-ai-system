package vn.edu.learningai.mapper;

import org.springframework.stereotype.Component;
import vn.edu.learningai.dto.response.QuizAuthorResponse;
import vn.edu.learningai.dto.response.QuizStudentResponse;
import vn.edu.learningai.dto.response.QuizSummaryResponse;
import vn.edu.learningai.entity.Question;
import vn.edu.learningai.entity.QuestionOption;
import vn.edu.learningai.entity.Quiz;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class QuizMapper {
    public QuizAuthorResponse toAuthor(Quiz quiz, List<Question> questions,
                                        Map<Long, List<QuestionOption>> options) {
        return new QuizAuthorResponse(quiz.getId().toString(), quiz.getLessonId().toString(), quiz.getTitle(),
            quiz.getStatus(), quiz.getCreatedAt(), ordered(questions).stream()
            .map(question -> toAuthorQuestion(question, options.get(question.getId()))).toList());
    }

    public QuizAuthorResponse.QuestionAuthor toAuthorQuestion(Question question, List<QuestionOption> options) {
        return new QuizAuthorResponse.QuestionAuthor(question.getId().toString(), question.getContent(),
            question.getExplanation(), question.getOrderIndex(), question.getGeneratedByAi(), orderedOptions(options)
            .stream().map(option -> new QuizAuthorResponse.OptionAuthor(option.getId().toString(),
                option.getContent(), option.getIsCorrect(), option.getOrderIndex())).toList());
    }

    public QuizStudentResponse toStudent(Quiz quiz, List<Question> questions,
                                          Map<Long, List<QuestionOption>> options) {
        return new QuizStudentResponse(quiz.getId().toString(), quiz.getLessonId().toString(), quiz.getTitle(),
            quiz.getStatus(), ordered(questions).stream()
            .map(question -> new QuizStudentResponse.StudentQuestion(question.getId().toString(),
                question.getContent(), question.getOrderIndex(), orderedOptions(options.get(question.getId())).stream()
                .map(option -> new QuizStudentResponse.StudentOption(option.getId().toString(), option.getContent(),
                    option.getOrderIndex())).toList())).toList());
    }

    public QuizSummaryResponse toSummary(Quiz quiz, long questionCount) {
        return new QuizSummaryResponse(quiz.getId().toString(), quiz.getLessonId().toString(), quiz.getTitle(),
            quiz.getStatus(), questionCount);
    }

    private List<Question> ordered(List<Question> questions) {
        return questions.stream().sorted(Comparator.comparing(Question::getOrderIndex)).toList();
    }

    private List<QuestionOption> orderedOptions(List<QuestionOption> options) {
        return options.stream().sorted(Comparator.comparing(QuestionOption::getOrderIndex)).toList();
    }
}
