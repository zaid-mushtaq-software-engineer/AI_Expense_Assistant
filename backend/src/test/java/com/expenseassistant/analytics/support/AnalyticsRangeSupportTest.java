package com.expenseassistant.analytics.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.expenseassistant.common.exception.ValidationFailedException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class AnalyticsRangeSupportTest {

    @Test
    void validateDateRange_acceptsInclusiveBounds() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = from.plusDays(365);
        AnalyticsRangeSupport.validateDateRange(from, to);
    }

    @Test
    void validateDateRange_rejectsMissingFrom() {
        assertThatThrownBy(() -> AnalyticsRangeSupport.validateDateRange(null, LocalDate.now()))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessage("from is required");
    }

    @Test
    void validateDateRange_rejectsFromAfterTo() {
        LocalDate from = LocalDate.of(2026, 2, 1);
        LocalDate to = LocalDate.of(2026, 1, 1);
        assertThatThrownBy(() -> AnalyticsRangeSupport.validateDateRange(from, to))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessage("from must be on or before to");
    }

    @Test
    void validateDateRange_rejectsMoreThan366Days() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = from.plusDays(366);
        assertThatThrownBy(() -> AnalyticsRangeSupport.validateDateRange(from, to))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessageContaining("366");
    }

    @Test
    void validateYearMonth_andMonthBounds() {
        LocalDate[] bounds = AnalyticsRangeSupport.monthBounds(2024, 2);
        assertThat(bounds[0]).isEqualTo(LocalDate.of(2024, 2, 1));
        assertThat(bounds[1]).isEqualTo(LocalDate.of(2024, 2, 29));

        YearMonth previous = AnalyticsRangeSupport.previousMonth(2026, 1);
        assertThat(previous).isEqualTo(YearMonth.of(2025, 12));
    }

    @Test
    void percentageOf_roundsTwoDecimals() {
        BigDecimal whole = new BigDecimal("1000.00");
        BigDecimal part = new BigDecimal("333.33");
        assertThat(AnalyticsRangeSupport.percentageOf(part, whole)).isEqualByComparingTo("33.33");
        assertThat(AnalyticsRangeSupport.percentageOf(part, BigDecimal.ZERO))
                .isEqualByComparingTo("0.00");
    }
}