package vn.edu.learningai.dto.request;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateQuizRequestTest {
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void omittedGenerationCreatesManualRequest() {
        var request = json.readValue("{\"title\":\"Java cơ bản\"}", CreateQuizRequest.class);
        assertThat(request.title()).isEqualTo("Java cơ bản");
        assertThat(request.generationId()).isNull();
    }

    @Test
    void explicitNullGenerationCreatesManualRequest() {
        var request = json.readValue("{\"title\":\"Java\",\"generation_id\":null}", CreateQuizRequest.class);
        assertThat(request.generationId()).isNull();
    }

    @Test
    void generationIdRetainsBigintPrecision() {
        var request = json.readValue("{\"title\":\"Java\",\"generation_id\":\"9223372036854775807\"}", CreateQuizRequest.class);
        assertThat(request.generationId()).isEqualTo(Long.MAX_VALUE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "[]", "\"Java\"", "1", "true", "{}",
        "{\"title\":null}", "{\"title\":42}", "{\"title\":true}",
        "{\"title\":[]}", "{\"title\":{}}", "{\"title\":\"\"}", "{\"title\":\"  \\n \\t\"}"})
    void invalidBodyAndTitleAreRejected(String body) {
        assertInvalid(body);
    }

    @ParameterizedTest
    @ValueSource(strings = {"status", "created_by", "generated_by_ai", "lesson_id", "questions", "generationId"})
    void unknownAndServerOwnedFieldsAreRejected(String field) {
        assertInvalid("{\"title\":\"Java\",\"" + field + "\":null}");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "true", "[]", "{}", "\"0\"", "\"-1\"", "\"01\"", "\"+1\"",
        "\"1.0\"", "\"1e2\"", "\" 1\"", "\"1 \"", "\"\"", "\"9223372036854775808\"", "\"10000000000000000000\""})
    void invalidGenerationIdIsRejectedWithoutCoercion(String generation) {
        assertInvalid("{\"title\":\"Java\",\"generation_id\":" + generation + "}");
    }

    @Test
    void titleLengthUsesUnicodeCharacters() {
        String title = "\uD83D\uDE00".repeat(200);
        var request = json.readValue("{\"title\":\"" + title + "\"}", CreateQuizRequest.class);
        assertThat(request.title()).isEqualTo(title);
        assertInvalid("{\"title\":\"" + title + "x\"}");
    }

    @Test
    void invalidDeserializationFailsAtTheHttpBindingBoundary() {
        assertThatThrownBy(() -> json.readValue("{\"title\":\"Java\",\"generation_id\":123}", CreateQuizRequest.class))
            .hasRootCauseInstanceOf(ApiException.class);
    }

    private void assertInvalid(String body) {
        assertThatThrownBy(() -> CreateQuizRequest.fromJson(json.readTree(body)))
            .isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }
}
