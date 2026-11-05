package vn.edu.learningai.ai;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.learningai.ai.AiCandidateValidator.InvalidCandidateException;
import vn.edu.learningai.dto.response.CardCandidate;
import vn.edu.learningai.dto.response.QuestionCandidate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiCandidateValidatorTest {

    private final AiCandidateValidator validator = new AiCandidateValidator(JsonMapper.builder().build());

    private static String question(String content, String options) {
        return "{\"content\":\"" + content + "\",\"options\":[" + options + "],\"explanation\":\"Vì vậy.\"}";
    }

    private static final String TWO_OPTIONS =
        "{\"content\":\"Chạy bytecode Java\",\"is_correct\":true},{\"content\":\"Lưu video\",\"is_correct\":false}";

    @Test
    void parsesValidQuiz() {
        String json = "{\"questions\":[" + question("JVM có vai trò gì?", TWO_OPTIONS) + ","
            + question("Bytecode là gì?", TWO_OPTIONS) + "]}";

        List<QuestionCandidate> questions = validator.parseQuizCandidate(json, 2);

        assertThat(questions).hasSize(2);
        assertThat(questions.get(0).content()).isEqualTo("JVM có vai trò gì?");
        assertThat(questions.get(0).options()).hasSize(2);
        assertThat(questions.get(0).options().get(0).isCorrect()).isTrue();
        assertThat(questions.get(0).explanation()).isEqualTo("Vì vậy.");
    }

    @Test
    void acceptsSingleJsonCodeFenceAndMissingExplanation() {
        String json = "```json\n{\"questions\":[{\"content\":\"Q?\",\"options\":[" + TWO_OPTIONS + "]}]}\n```";

        List<QuestionCandidate> questions = validator.parseQuizCandidate(json, 1);

        assertThat(questions.get(0).explanation()).isNull();
    }

    @Test
    void rejectsWrongCount() {
        String json = "{\"questions\":[" + question("Q?", TWO_OPTIONS) + "]}";
        assertThatThrownBy(() -> validator.parseQuizCandidate(json, 3))
            .isInstanceOf(InvalidCandidateException.class)
            .hasMessageContaining("exactly 3");
    }

    @Test
    void rejectsTwoCorrectOptions() {
        String options = "{\"content\":\"A\",\"is_correct\":true},{\"content\":\"B\",\"is_correct\":true}";
        String json = "{\"questions\":[" + question("Q?", options) + "]}";
        assertThatThrownBy(() -> validator.parseQuizCandidate(json, 1))
            .isInstanceOf(InvalidCandidateException.class)
            .hasMessageContaining("exactly one correct");
    }

    @Test
    void rejectsTooFewAndTooManyOptions() {
        String one = "{\"questions\":[" + question("Q?", "{\"content\":\"A\",\"is_correct\":true}") + "]}";
        assertThatThrownBy(() -> validator.parseQuizCandidate(one, 1)).hasMessageContaining("2-6 options");

        StringBuilder seven = new StringBuilder("{\"content\":\"O0\",\"is_correct\":true}");
        for (int i = 1; i < 7; i++) {
            seven.append(",{\"content\":\"O").append(i).append("\",\"is_correct\":false}");
        }
        String json = "{\"questions\":[" + question("Q?", seven.toString()) + "]}";
        assertThatThrownBy(() -> validator.parseQuizCandidate(json, 1)).hasMessageContaining("2-6 options");
    }

    @Test
    void rejectsUnexpectedFieldsAtEveryLevel() {
        assertThatThrownBy(() -> validator.parseQuizCandidate(
            "{\"questions\":[" + question("Q?", TWO_OPTIONS) + "],\"note\":\"x\"}", 1))
            .hasMessageContaining("unexpected field 'note'");
        assertThatThrownBy(() -> validator.parseQuizCandidate(
            "{\"questions\":[{\"content\":\"Q?\",\"options\":[" + TWO_OPTIONS + "],\"answer\":0}]}", 1))
            .hasMessageContaining("unexpected field 'answer'");
        assertThatThrownBy(() -> validator.parseQuizCandidate(
            "{\"questions\":[" + question("Q?",
                "{\"content\":\"A\",\"is_correct\":true,\"id\":1},{\"content\":\"B\",\"is_correct\":false}") + "]}", 1))
            .hasMessageContaining("unexpected field 'id'");
    }

    @Test
    void rejectsDuplicateQuestionsAfterNormalize() {
        String json = "{\"questions\":[" + question("JVM là gì?", TWO_OPTIONS) + ","
            + question("  jvm   LÀ gì?", TWO_OPTIONS) + "]}";
        assertThatThrownBy(() -> validator.parseQuizCandidate(json, 2)).hasMessageContaining("duplicates");
    }

    @Test
    void rejectsDuplicateOptions() {
        String options = "{\"content\":\"Đúng\",\"is_correct\":true},{\"content\":\"đúng\",\"is_correct\":false}";
        assertThatThrownBy(() -> validator.parseQuizCandidate("{\"questions\":[" + question("Q?", options) + "]}", 1))
            .hasMessageContaining("duplicate options");
    }

    @Test
    void rejectsNonBooleanIsCorrectAndBlankContent() {
        String stringFlag = "{\"content\":\"A\",\"is_correct\":\"true\"},{\"content\":\"B\",\"is_correct\":false}";
        assertThatThrownBy(() -> validator.parseQuizCandidate("{\"questions\":[" + question("Q?", stringFlag) + "]}", 1))
            .hasMessageContaining("must be a boolean");
        assertThatThrownBy(() -> validator.parseQuizCandidate("{\"questions\":[" + question("   ", TWO_OPTIONS) + "]}", 1))
            .hasMessageContaining("content must be 1-4000");
    }

    @Test
    void rejectsNonJsonAndProse() {
        assertThatThrownBy(() -> validator.parseQuizCandidate("Here are your questions: ...", 1))
            .hasMessageContaining("not valid JSON");
        assertThatThrownBy(() -> validator.parseQuizCandidate("", 1)).hasMessageContaining("empty");
        assertThatThrownBy(() -> validator.parseQuizCandidate("[]", 1)).hasMessageContaining("must be an object");
    }

    @Test
    void parsesValidCardsAndRejectsDuplicatesOrMissingSide() {
        List<CardCandidate> cards = validator.parseCardCandidate(
            "{\"flashcards\":[{\"front_text\":\"JVM là gì?\",\"back_text\":\"Máy ảo Java.\"}]}", 1);
        assertThat(cards).containsExactly(new CardCandidate("JVM là gì?", "Máy ảo Java."));

        assertThatThrownBy(() -> validator.parseCardCandidate(
            "{\"flashcards\":[{\"front_text\":\"A\",\"back_text\":\"1\"},{\"front_text\":\"a \",\"back_text\":\"2\"}]}", 2))
            .hasMessageContaining("duplicates");
        assertThatThrownBy(() -> validator.parseCardCandidate(
            "{\"flashcards\":[{\"front_text\":\"A\"}]}", 1))
            .hasMessageContaining("back_text must be a string");
    }
}
