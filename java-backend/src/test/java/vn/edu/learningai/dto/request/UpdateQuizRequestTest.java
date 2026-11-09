package vn.edu.learningai.dto.request;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpdateQuizRequestTest {
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void absentFieldsAreUnchanged() {
        assertThat(json.readValue("{}", UpdateQuizRequest.class)).isEqualTo(new UpdateQuizRequest(null, null));
    }

    @Test
    void explicitNullGenerationMeansRetainProvenance() {
        var request = json.readValue("{\"title\":\"Java\",\"generation_id\":null}", UpdateQuizRequest.class);
        assertThat(request).isEqualTo(new UpdateQuizRequest("Java", null));
    }

    @Test
    void generationOnlyPatchPreservesBigintPrecision() {
        var request = json.readValue("{\"generation_id\":\"9223372036854775807\"}", UpdateQuizRequest.class);
        assertThat(request).isEqualTo(new UpdateQuizRequest(null, Long.MAX_VALUE));
    }

    @Test
    void titleLengthUsesUnicodeCodePoints() {
        String title = "\uD83D\uDE00".repeat(200);
        assertThat(json.readValue("{\"title\":\"" + title + "\"}", UpdateQuizRequest.class).title()).isEqualTo(title);
        assertThatThrownBy(() -> json.readValue("{\"title\":\"" + title + "x\"}", UpdateQuizRequest.class))
            .hasRootCauseInstanceOf(vn.edu.learningai.exception.ApiException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "1", "true", "{\"title\":null}", "{\"title\":1}", "{\"title\":true}",
        "{\"title\":[]}", "{\"title\":{}}", "{\"title\":\"\"}", "{\"title\":\"  \\t\"}",
        "{\"status\":\"DRAFT\"}", "{\"created_by\":\"7\"}", "{\"generationId\":\"40\"}",
        "{\"generation_id\":1}", "{\"generation_id\":true}", "{\"generation_id\":[]}", "{\"generation_id\":{}}",
        "{\"generation_id\":\"0\"}", "{\"generation_id\":\"01\"}", "{\"generation_id\":\"-1\"}",
        "{\"generation_id\":\"1.0\"}", "{\"generation_id\":\"9223372036854775808\"}"})
    void invalidPatchCannotBeCoerced(String body) {
        assertThatThrownBy(() -> json.readValue(body, UpdateQuizRequest.class))
            .hasRootCauseInstanceOf(vn.edu.learningai.exception.ApiException.class);
    }
}
