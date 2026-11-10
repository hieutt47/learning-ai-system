package vn.edu.learningai.dto.request;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WriteQuestionRequestTest {
    private final JsonMapper json = JsonMapper.builder().build();

    private String body(String options) {
        return "{\"content\":\"Question\",\"explanation\":null,\"order_index\":5,\"options\":" + options + "}";
    }

    private String option(String content, String correct, String position) {
        return "{\"content\":\"" + content + "\",\"is_correct\":" + correct + ",\"order_index\":" + position + "}";
    }

    @Test
    void manualQuestionPreservesNonContiguousPositionsAndNullableExplanation() {
        var request = json.readValue(body("[" + option("A", "true", "7") + "," + option("B", "false", "2") + "]"),
            WriteQuestionRequest.class);
        assertThat(request.orderIndex()).isEqualTo(5);
        assertThat(request.explanation()).isNull();
        assertThat(request.generationId()).isNull();
        assertThat(request.options()).extracting(WriteQuestionRequest.Option::orderIndex).containsExactly(7, 2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "[{}]", "null", "{}", "true"})
    void invalidOptionArrayIsRejected(String options) {
        rejected(body(options), ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void rejectsZeroOrMultipleCorrectOptions() {
        rejected(body("[" + option("A", "false", "0") + "," + option("B", "false", "1") + "]"), ErrorCode.VALIDATION_ERROR);
        rejected(body("[" + option("A", "true", "0") + "," + option("B", "true", "1") + "]"), ErrorCode.VALIDATION_ERROR);
    }

    @ParameterizedTest
    @ValueSource(strings = {" A ", "a", "A\\t", "A\\u00a0"})
    void duplicateNormalizedContentIsRejected(String content) {
        rejected(body("[" + option("A", "true", "0") + "," + option(content, "false", "1") + "]"), ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void unicodeEquivalentOptionsAreDuplicates() {
        rejected(body("[" + option("é", "true", "0") + "," + option("e\u0301", "false", "1") + "]"), ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void duplicatePositionIsConflict() {
        rejected(body("[" + option("A", "true", "3") + "," + option("B", "false", "3") + "]"), ErrorCode.INVALID_STATE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "1.0", "\"1\"", "null", "true", "2147483648"})
    void optionPositionsMustBeIntegerWithoutCoercion(String position) {
        rejected(body("[" + option("A", "true", position) + "," + option("B", "false", "1") + "]"), ErrorCode.VALIDATION_ERROR);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "\"true\"", "null", "[]"})
    void correctnessMustBeBoolean(String correct) {
        rejected(body("[" + option("A", correct, "0") + "," + option("B", "false", "1") + "]"), ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void rejectsUnknownServerFieldsAndMissingExplanation() {
        String valid = body("[" + option("A", "true", "0") + "," + option("B", "false", "1") + "]");
        rejected(valid.replace("\"explanation\":null,", ""), ErrorCode.VALIDATION_ERROR);
        rejected(valid.replace("\"content\":\"Question\"", "\"id\":\"50\",\"content\":\"Question\""), ErrorCode.VALIDATION_ERROR);
        rejected(valid.replace("\"is_correct\":true", "\"id\":\"60\",\"is_correct\":true"), ErrorCode.VALIDATION_ERROR);
        rejected(valid.replace("\"content\":\"Question\"", "\"generated_by_ai\":true,\"content\":\"Question\""), ErrorCode.VALIDATION_ERROR);
        rejected(valid.replace("\"content\":\"Question\"", "\"content\":\"\\u00a0\""), ErrorCode.VALIDATION_ERROR);
        rejected(valid.replace("\"order_index\":5", "\"order_index\":-1"), ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void enforcesLengthsAndOptionCount() {
        String valid = body("[" + option("A", "true", "0") + "," + option("B", "false", "1") + "]");
        rejected(valid.replace("Question", "q".repeat(4001)), ErrorCode.VALIDATION_ERROR);
        rejected(valid.replace("\"explanation\":null", "\"explanation\":\"" + "e".repeat(4001) + "\""), ErrorCode.VALIDATION_ERROR);
        rejected(valid.replace("\"A\"", "\"" + "a".repeat(1001) + "\""), ErrorCode.VALIDATION_ERROR);
        String options = java.util.stream.IntStream.range(0, 7).mapToObj(i -> option("Option " + i, i == 0 ? "true" : "false", "" + i))
            .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        rejected(body(options), ErrorCode.VALIDATION_ERROR);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "01", "-1", "9223372036854775808"})
    void generationIdMustBePositiveBigint(String id) {
        String valid = body("[" + option("A", "true", "0") + "," + option("B", "false", "1") + "]");
        rejected(valid.replace("\"explanation\":null", "\"explanation\":null,\"generation_id\":\"" + id + "\""), ErrorCode.VALIDATION_ERROR);
    }

    private void rejected(String body, ErrorCode code) {
        assertThatThrownBy(() -> WriteQuestionRequest.fromJson(json.readTree(body)))
            .isInstanceOfSatisfying(ApiException.class, exception -> assertThat(exception.getErrorCode()).isEqualTo(code));
    }
}
