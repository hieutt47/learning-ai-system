package vn.edu.learningai.ai;

import lombok.Getter;

/** Lỗi khi gọi Hệ thống AI; service đổi sang AI_TIMEOUT / AI_PROVIDER_ERROR / AI_OUTPUT_INVALID. */
@Getter
public class AIClientException extends RuntimeException {

    public enum Reason {
        TIMEOUT,
        PROVIDER_ERROR,
        INVALID_OUTPUT
    }

    private final Reason reason;

    public AIClientException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public AIClientException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }
}
