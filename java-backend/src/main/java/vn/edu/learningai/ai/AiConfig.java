package vn.edu.learningai.ai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** Chọn cài đặt AIClient theo app.ai.provider. */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    @Bean
    public AIClient aiClient(AiProperties properties, JsonMapper jsonMapper) {
        String provider = properties.provider() == null ? "mock" : properties.provider().trim().toLowerCase();
        return switch (provider) {
            case "mock" -> new MockAIClient(jsonMapper);
            case "llm" -> new LlmAIClient(properties, jsonMapper);
            default -> throw new IllegalStateException(
                "app.ai.provider (AI_PROVIDER) phải là 'mock' hoặc 'llm', đang là: " + properties.provider());
        };
    }
}
