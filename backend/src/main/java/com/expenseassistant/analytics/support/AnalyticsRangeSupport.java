package com.expenseassistant.analytics.support;

import com.expenseassistant.common.exception.ValidationFailedException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

public final class AnalyticsRangeSupport {

    static final int MAX_INCLUSIVE_DAYS = 366;
    static final int MIN_YEAR = 2000;
    static final int MAX_YEAR = 2100;

    private AnalyticsRangeSupport() {}

    public static void validateDateRange(LocalDate from, LocalDate to) {
        if (from == null) {
            throw new ValidationFailedException("from is required");
        }
        if (to == null) {
            throw new ValidationFailedException("to is required");
        }
        if (from.isAfter(to)) {
            throw new ValidationFailedException("from must be on or before to");
        }
        long inclusiveDays = inclusiveDayCount(from, to);
        if (inclusiveDays > MAX_INCLUSIVE_DAYS) {
            throw new ValidationFailedException("Date range must not exceed " + MAX_INCLUSIVE_DAYS + " days");
        }
    }

    public static void validateYearMonth(int year, int month) {
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new ValidationFailedException("year must be between " + MIN_YEAR + " and " + MAX_YEAR);
        }
        if (month < 1 || month > 12) {
            throw new ValidationFailedException("month must be between 1 and 12");
        }
    }

    public static LocalDate[] monthBounds(int year, int month) {
        validateYearMonth(year, month);
        YearMonth yearMonth = YearMonth.of(year, month);
        return new LocalDate[] {yearMonth.atDay(1), yearMonth.atEndOfMonth()};
    }

    public static YearMonth previousMonth(int year, int month) {
        validateYearMonth(year, month);
        return YearMonth.of(year, month).minusMonths(1);
    }

    public static long inclusiveDayCount(LocalDate from, LocalDate to) {
        return to.toEpochDay() - from.toEpochDay() + 1;
    }

    public static BigDecimal roundMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal percentageOf(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return roundMoney(part.multiply(BigDecimal.valueOf(100)).divide(whole, 4, RoundingMode.HALF_UP));
    }
}