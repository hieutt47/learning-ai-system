package vn.edu.learningai.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.learningai.common.enums.QuizStatus;
import vn.edu.learningai.entity.Question;
import vn.edu.learningai.entity.QuestionOption;
import vn.edu.learningai.entity.Quiz;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QuizMapperTest {
    private final QuizMapper mapper = Mappers.getMapper(QuizMapper.class);
    private final JsonMapper json = JsonMapper.builder().build();

    private Quiz quiz() {
        return Quiz.builder().id(9007199254740993L).lessonId(901L).title("Java")
            .status(QuizStatus.PUBLISHED).createdAt(Instant.parse("2026-11-05T00:00:00Z"))
            .generationId(44L).createdBy(1L).build();
    }

    private Question question(long id, int index) {
        return Question.builder().id(id).quizId(quiz().getId()).content("Question " + id)
            .explanation("Private explanation").orderIndex(index).generatedByAi(true).generationId(44L).build();
    }

    private List<QuestionOption> options(long questionId) {
        return List.of(QuestionOption.builder().id(questionId * 10 + 1).questionId(questionId)
                .content("Wrong").isCorrect(false).orderIndex(1).build(),
            QuestionOption.builder().id(questionId * 10).questionId(questionId)
                .content("Correct").isCorrect(true).orderIndex(0).build());
    }

    @Test
    void studentJsonNeverContainsAnswerKeysOrProvenance() {
        var result = mapper.toStudent(quiz(), List.of(question(10, 0)), Map.of(10L, options(10)));
        var tree = json.readTree(json.writeValueAsString(result));

        assertThat(tree.get("id").asString()).isEqualTo("9007199254740993");
        assertThat(tree.get("lesson_id").asString()).isEqualTo("901");
        assertThat(tree.get("questions").get(0).get("options").get(0).get("id").asString()).isEqualTo("100");
        assertThat(tree.get("questions").get(0).has("explanation")).isFalse();
        assertThat(tree.get("questions").get(0).has("generated_by_ai")).isFalse();
        assertThat(tree.get("questions").get(0).has("generation_id")).isFalse();
        assertThat(tree.get("questions").get(0).get("options").get(0).has("is_correct")).isFalse();
        assertThat(tree.get("questions").get(0).get("options").get(0).has("correct_option_id")).isFalse();
    }

    @Test
    void authorJsonIncludesExplanationCorrectnessAndOrdering() {
        var result = mapper.toAuthor(quiz(), List.of(question(10, 0)), Map.of(10L, options(10)));
        var tree = json.readTree(json.writeValueAsString(result));
        var question = tree.get("questions").get(0);

        assertThat(question.get("explanation").asString()).isEqualTo("Private explanation");
        assertThat(question.get("generated_by_ai").asBoolean()).isTrue();
        assertThat(question.get("order_index").asInt()).isZero();
        assertThat(question.get("options").get(0).get("is_correct").asBoolean()).isTrue();
        assertThat(tree.get("created_at").asString()).isEqualTo("2026-11-05T00:00:00Z");
    }

    @Test
    void bothViewsOrderQuestionsAndOptionsWithoutMutatingInput() {
        var questions = List.of(question(20, 5), question(10, 2));
        var optionMap = Map.of(10L, options(10), 20L, options(20));
        var student = mapper.toStudent(quiz(), questions, optionMap);
        var author = mapper.toAuthor(quiz(), questions, optionMap);

        assertThat(student.questions()).extracting(item -> item.id()).containsExactly("10", "20");
        assertThat(author.questions()).extracting(item -> item.id()).containsExactly("10", "20");
        assertThat(student.questions().get(0).options()).extracting(item -> item.orderIndex()).containsExactly(0, 1);
        assertThat(author.questions().get(0).options()).extracting(item -> item.orderIndex()).containsExactly(0, 1);
        assertThat(questions.get(0).getId()).isEqualTo(20L);
        assertThat(optionMap.get(10L).get(0).getOrderIndex()).isEqualTo(1);
    }

    @Test
    void draftAuthorCanHaveNoQuestions() {
        var draft = quiz();
        draft.setStatus(QuizStatus.DRAFT);
        assertThat(mapper.toAuthor(draft, List.of(), Map.of()).questions()).isEmpty();
    }

    @Test
    void nullableExplanationIsPreserved() {
        var question = question(10, 0);
        question.setExplanation(null);
        question.setGenerationId(null);
        question.setGeneratedByAi(false);
        var result = mapper.toAuthorQuestion(question, options(10));
        assertThat(result.explanation()).isNull();
        assertThat(result.generatedByAi()).isFalse();
    }

    @Test
    void summaryUsesWireIdsAndExplicitQuestionCount() {
        var tree = json.readTree(json.writeValueAsString(mapper.toSummary(quiz(), 7)));
        assertThat(tree.get("id").asString()).isEqualTo("9007199254740993");
        assertThat(tree.get("question_count").asInt()).isEqualTo(7);
        assertThat(tree.has("questions")).isFalse();
    }
}
