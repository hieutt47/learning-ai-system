package vn.edu.learningai.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

/**
 * Cấu hình app.ai.* (application.yaml, đọc từ biến môi trường AI_*).
 *
 * @param provider        mock (mặc định, demo offline) hoặc llm (gọi thật)
 * @param baseUrl         gốc API định dạng OpenAI Chat Completions, vd. https://openrouter.ai/api/v1
 * @param model           tên model của provider, vd. một model ":free" trên OpenRouter
 * @param apiKey          API key; không log, không trả ra API
 * @param timeoutSeconds  thời gian chờ tối đa một lần gọi
 * @param maxOutputTokens giới hạn token model được sinh
 * @param maxSourceChars  độ dài nguồn tối đa gửi cho AI (mặc định 20.000 ký tự)
 * @param sourceStrategy  chiến lược xử lý nguồn khi dài: head (cắt đầu) hoặc spread (lấy mẫu rải đều, mặc định)
 * @param jsonMode        có gửi response_format: { type: "json_object" } hay không
 * @param temperature     mức độ ngẫu nhiên khi sinh (mặc định 0.3 để ổn định JSON và bám bài)
 */
@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
    String provider,
    String baseUrl,
    String model,
    String apiKey,
    Integer timeoutSeconds,
    Integer maxOutputTokens,
    Integer maxSourceChars,
    String sourceStrategy,
    Boolean jsonMode,
    Double temperature
) {
    @ConstructorBinding
    public AiProperties {
    }

    public AiProperties(String provider, String baseUrl, String model, String apiKey,
                        Integer timeoutSeconds, Integer maxOutputTokens, Integer maxSourceChars) {
        this(provider, baseUrl, model, apiKey, timeoutSeconds, maxOutputTokens, maxSourceChars,
            "spread", false, 0.3);
    }

    public AiProperties(String provider, String baseUrl, String model, String apiKey,
                        Integer timeoutSeconds, Integer maxOutputTokens, Integer maxSourceChars,
                        String sourceStrategy, Boolean jsonMode) {
        this(provider, baseUrl, model, apiKey, timeoutSeconds, maxOutputTokens, maxSourceChars,
            sourceStrategy, jsonMode, 0.3);
    }

    @Override
    public String sourceStrategy() {
        return sourceStrategy == null || sourceStrategy.isBlank() ? "spread" : sourceStrategy.trim();
    }

    @Override
    public Boolean jsonMode() {
        return Boolean.TRUE.equals(jsonMode);
    }

    @Override
    public Double temperature() {
        return temperature == null ? 0.3 : temperature;
    }
}
