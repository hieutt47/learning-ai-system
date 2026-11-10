package vn.edu.learningai.dto.request;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.learningai.common.enums.QuizStatus;
import vn.edu.learningai.exception.ApiException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuizPublicationRequestTest {
    private final JsonMapper json = JsonMapper.builder().build();

    @ParameterizedTest
    @ValueSource(strings = {"PUBLISHED", "CLOSED"})
    void acceptsOnlyPublicationTargets(String status) {
        assertThat(json.readValue("{\"status\":\"" + status + "\"}", QuizPublicationRequest.class).status())
            .isEqualTo(QuizStatus.valueOf(status));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "[]", "true", "{\"status\":null}", "{\"status\":1}",
        "{\"status\":true}", "{\"status\":\"DRAFT\"}", "{\"status\":\"published\"}",
        "{\"status\":\"PUBLISHED\",\"published_at\":null}"})
    void rejectsInvalidTargetAndServerFields(String body) {
        assertThatThrownBy(() -> json.readValue(body, QuizPublicationRequest.class)).hasRootCauseInstanceOf(ApiException.class);
    }
}
