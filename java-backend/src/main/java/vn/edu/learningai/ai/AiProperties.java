package vn.edu.learningai.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình app.ai.* (application.yaml, đọc từ biến môi trường AI_*).
 *
 * @param provider        mock (mặc định, demo offline) hoặc llm (gọi thật)
 * @param baseUrl         gốc API định dạng OpenAI Chat Completions, vd. https://openrouter.ai/api/v1
 * @param model           tên model của provider, vd. một model ":free" trên OpenRouter
 * @param apiKey          API key; không log, không trả ra API
 * @param timeoutSeconds  thời gian chờ tối đa một lần gọi
 * @param maxOutputTokens giới hạn token model được sinh
 * @param maxSourceChars  độ dài nguồn tối đa gửi cho AI (thỏa thuận với người 3: 20.000 ký tự)
 */
@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
    String provider,
    String baseUrl,
    String model,
    String apiKey,
    Integer timeoutSeconds,
    Integer maxOutputTokens,
    Integer maxSourceChars
) {
}
