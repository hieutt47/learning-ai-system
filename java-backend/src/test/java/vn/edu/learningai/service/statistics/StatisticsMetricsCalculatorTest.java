package vn.edu.learningai.service.statistics;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class StatisticsMetricsCalculatorTest {
    private final StatisticsMetricsCalculator calculator = new StatisticsMetricsCalculator();

    @Test
    void completionRateReturnsZeroWhenDenominatorOrPublishedQuizzesIsZero() {
        assertThat(calculator.completionRate(3, 0, 2)).isEqualByComparingTo("0");
        assertThat(calculator.completionRate(3, 10, 0)).isEqualByComparingTo("0");
        assertThat(calculator.completionRate(0, 10, 2)).isEqualByComparingTo("0");
    }

    @Test
    void completionRateRoundsHalfUpToTwoDecimals() {
        assertThat(calculator.completionRate(1, 2, 2)).isEqualByComparingTo("50.00");
        assertThat(calculator.completionRate(1, 3, 1)).isEqualByComparingTo("33.33");
        assertThat(calculator.completionRate(2, 3, 1)).isEqualByComparingTo("66.67");
    }

    @Test
    void wrongRateComputesPercentageAndHandlesZeroAttempts() {
        assertThat(calculator.wrongRate(2, 0)).isEqualByComparingTo("0");
        assertThat(calculator.wrongRate(0, 5)).isEqualByComparingTo("0");
        assertThat(calculator.wrongRate(2, 3)).isEqualByComparingTo("66.67");
    }

    @Test
    void roundScorePreservesNullAndRoundsHalfUp() {
        assertThat(calculator.roundScore(null)).isNull();
        assertThat(calculator.roundScore(new BigDecimal("33.335"))).isEqualByComparingTo("33.34");
        assertThat(calculator.roundScore(new BigDecimal("85"))).isEqualByComparingTo("85.00");
    }
}
