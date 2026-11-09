package vn.edu.learningai.ai;

/**
 * Cổng gọi Hệ thống AI (báo cáo 3.3.4). Nghiệp vụ chỉ phụ thuộc interface này nên có thể đổi
 * nhà cung cấp hoặc dùng bản giả lập khi demo mà không sửa service.
 */
public interface AIClient {

    /**
     * Gửi prompt và trả nguyên văn text model sinh ra (kỳ vọng là JSON, service sẽ validate).
     *
     * @throws AIClientException khi quá thời gian, provider lỗi hoặc output bị cắt
     */
    String generate(String prompt);

    /** Tên provider ghi vào ai_generations.provider. */
    String provider();

    /** Tên model ghi vào ai_generations.model. */
    String model();
}
