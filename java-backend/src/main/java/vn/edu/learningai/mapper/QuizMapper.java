package vn.edu.learningai.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.edu.learningai.dto.response.QuizAuthorResponse;
import vn.edu.learningai.dto.response.QuizStudentResponse;
import vn.edu.learningai.dto.response.QuizSummaryResponse;
import vn.edu.learningai.entity.Question;
import vn.edu.learningai.entity.QuestionOption;
import vn.edu.learningai.entity.Quiz;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface QuizMapper {

    default QuizAuthorResponse toAuthor(Quiz quiz, List<Question> questions,
                                        Map<Long, List<QuestionOption>> options) {
        if (quiz == null) {
            return null;
        }
        return new QuizAuthorResponse(
            quiz.getId() != null ? quiz.getId().toString() : null,
            quiz.getLessonId() != null ? quiz.getLessonId().toString() : null,
            quiz.getTitle(),
            quiz.getStatus(),
            quiz.getCreatedAt(),
            ordered(questions).stream()
                .map(question -> toAuthorQuestion(question, options != null ? options.get(question.getId()) : null))
                .toList()
        );
    }

    default QuizAuthorResponse.QuestionAuthor toAuthorQuestion(Question question, List<QuestionOption> options) {
        if (question == null) {
            return null;
        }
        return new QuizAuthorResponse.QuestionAuthor(
            question.getId() != null ? question.getId().toString() : null,
            question.getContent(),
            question.getExplanation(),
            question.getOrderIndex(),
            question.getGeneratedByAi(),
            orderedOptions(options).stream()
                .map(this::toAuthorOption)
                .toList()
        );
    }

    QuizAuthorResponse.OptionAuthor toAuthorOption(QuestionOption option);

    default QuizStudentResponse toStudent(Quiz quiz, List<Question> questions,
                                          Map<Long, List<QuestionOption>> options) {
        if (quiz == null) {
            return null;
        }
        return new QuizStudentResponse(
            quiz.getId() != null ? quiz.getId().toString() : null,
            quiz.getLessonId() != null ? quiz.getLessonId().toString() : null,
            quiz.getTitle(),
            quiz.getStatus(),
            ordered(questions).stream()
                .map(question -> toStudentQuestion(question, options != null ? options.get(question.getId()) : null))
                .toList()
        );
    }

    default QuizStudentResponse.StudentQuestion toStudentQuestion(Question question, List<QuestionOption> options) {
        if (question == null) {
            return null;
        }
        return new QuizStudentResponse.StudentQuestion(
            question.getId() != null ? question.getId().toString() : null,
            question.getContent(),
            question.getOrderIndex(),
            orderedOptions(options).stream()
                .map(this::toStudentOption)
                .toList()
        );
    }

    QuizStudentResponse.StudentOption toStudentOption(QuestionOption option);

    @Mapping(target = "id", expression = "java(quiz.getId() != null ? quiz.getId().toString() : null)")
    @Mapping(target = "lessonId", expression = "java(quiz.getLessonId() != null ? quiz.getLessonId().toString() : null)")
    @Mapping(target = "questionCount", source = "questionCount")
    QuizSummaryResponse toSummary(Quiz quiz, long questionCount);

    private List<Question> ordered(List<Question> questions) {
        if (questions == null) {
            return List.of();
        }
        return questions.stream().sorted(Comparator.comparing(Question::getOrderIndex)).toList();
    }

    private List<QuestionOption> orderedOptions(List<QuestionOption> options) {
        if (options == null) {
            return List.of();
        }
        return options.stream().sorted(Comparator.comparing(QuestionOption::getOrderIndex)).toList();
    }
}

