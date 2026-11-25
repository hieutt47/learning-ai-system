package vn.edu.learningai.service;

import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;
import org.springframework.jdbc.core.simple.JdbcClient;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.security.SecurityHelper;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.withSettings;

class AdminAiServiceTest {

    @Test
    void invalidPaginationOrMissingLessonIdIsRejectedBeforeQuerying() {
        var jdbc = mock(JdbcClient.class);
        var security = mock(SecurityHelper.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        var service = new AdminAiService(jdbc, security);

        assertThatThrownBy(() -> service.listLessonAiGenerations(10L, null, null, 0, 20))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.VALIDATION_ERROR);

        assertThatThrownBy(() -> service.listLessonAiGenerations(10L, null, null, 1, 101))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.VALIDATION_ERROR);

        assertThatThrownBy(() -> service.listLessonAiGenerations(null, null, null, 1, 20))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.VALIDATION_ERROR);

        verifyNoInteractions(jdbc, security);
    }
}
