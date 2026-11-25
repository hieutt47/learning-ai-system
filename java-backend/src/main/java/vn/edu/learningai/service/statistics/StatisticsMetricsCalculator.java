package vn.edu.learningai.service.statistics;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Nghiệp vụ tính toán thuần cho các chỉ số thống kê (không phụ thuộc DB/HTTP).
 * Quy tắc: tỷ lệ phần trăm và điểm làm tròn 2 chữ số thập phân (HALF_UP); mẫu số 0 trả 0.
 */
@Component
public class StatisticsMetricsCalculator {
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final int DISPLAY_SCALE = 2;

    public BigDecimal completionRate(long completedStudents, long enrolledStudents, long publishedQuizCount) {
        if (enrolledStudents <= 0 || publishedQuizCount <= 0 || completedStudents <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(completedStudents)
            .multiply(ONE_HUNDRED)
            .divide(BigDecimal.valueOf(enrolledStudents), DISPLAY_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal wrongRate(long wrongCount, long submittedAttempts) {
        if (submittedAttempts <= 0 || wrongCount <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(wrongCount)
            .multiply(ONE_HUNDRED)
            .divide(BigDecimal.valueOf(submittedAttempts), DISPLAY_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal roundScore(BigDecimal rawScore) {
        return rawScore == null ? null : rawScore.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);
    }
}
