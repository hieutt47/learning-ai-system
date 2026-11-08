package vn.edu.learningai.support;

import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Base for tests that run against a real MySQL database with the real Flyway
 * migrations. Requires DBMS_TEST_CONNECTION/DBMS_TEST_USERNAME/DBMS_TEST_PASSWORD
 * (see application-test.yaml); there is no H2 substitute because the schema is
 * MySQL-specific and ddl-auto is validate.
 *
 * <p>Tagged so plain {@code ./mvnw test} stays runnable without a database; run
 * these with {@code ./mvnw test -Dsurefire.excludedGroups=}.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
@Sql(scripts = "/test-fixtures.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public abstract class IntegrationTestBase {
    /** Password of every user in test-fixtures.sql. */
    protected static final String FIXTURE_PASSWORD = "DemoPass123!";

    @Autowired
    protected MockMvc mockMvc;

    /** Tokens per user id for the current test; cleared by a fresh context per class. */
    private final Map<Long, String> tokens = new HashMap<>();

    /**
     * Access token for a fixture user, obtained by logging in through the real
     * auth route.
     *
     * <p>Logging in rather than forging a token means the test exercises the
     * same path production uses: the password hash, the filter, and the
     * authority mapping all have to be right for any assertion to pass. It also
     * keeps tests honest about inactive accounts, which cannot log in at all.</p>
     */
    protected String tokenFor(long userId) throws Exception {
        String cached = tokens.get(userId);
        if (cached != null) {
            return cached;
        }

        String body = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}""".formatted(emailOf(userId), FIXTURE_PASSWORD)))
            .andReturn().getResponse().getContentAsString();

        String token = com.jayway.jsonpath.JsonPath.read(body, "$.access_token");
        tokens.put(userId, token);
        return token;
    }

    /** Adds {@code Authorization: Bearer <token>} for a fixture user. */
    protected MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request, long userId)
        throws Exception {
        return request.header("Authorization", "Bearer " + tokenFor(userId));
    }

    /** Fixture users, matching test-fixtures.sql. */
    protected static final long ADMIN = 1;
    protected static final long TEACHER_A = 2;
    protected static final long TEACHER_B = 3;
    protected static final long STUDENT_A = 4;
    protected static final long INACTIVE_TEACHER = 5;
    protected static final long STUDENT_B = 6;

    private static String emailOf(long userId) {
        return switch ((int) userId) {
            case 1 -> "admin@test.local";
            case 2 -> "teacher.a@test.local";
            case 3 -> "teacher.b@test.local";
            case 4 -> "student.a@test.local";
            case 5 -> "inactive@test.local";
            case 6 -> "student.b@test.local";
            default -> throw new IllegalArgumentException("Unknown fixture user " + userId);
        };
    }
}
