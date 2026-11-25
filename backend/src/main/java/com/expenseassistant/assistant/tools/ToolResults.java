package com.expenseassistant.assistant.tools;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;

public final class ToolResults {

    private ToolResults() {}

    public record ErrorResult(String code, String message) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ToolOutcome<T>(T success, ErrorResult error) {

        public static <T> ToolOutcome<T> ok(T success) {
            return new ToolOutcome<>(success, null);
        }

        public static <T> ToolOutcome<T> err(ErrorResult error) {
            return new ToolOutcome<>(null, error);
        }
    }

    public record TotalSpendingResult(String from, String to, BigDecimal totalAmount, long expenseCount) {}

    public record CategoryLine(String category, BigDecimal amount, BigDecimal percentage, long expenseCount) {}

    public record CategorySpendingResult(
            String from, String to, BigDecimal totalAmount, List<CategoryLine> categories) {}

    public record HighestCategoryResult(String from, String to, String category, BigDecimal amount) {}

    public record MonthlySummaryResult(
            int year,
            int month,
            BigDecimal totalAmount,
            long expenseCount,
            String highestCategory,
            BigDecimal previousMonthTotal,
            BigDecimal changeFromPreviousMonth,
            List<CategoryLine> categories) {}

    public record TopExpenseLine(
            Long id,
            BigDecimal amount,
            String category,
            String description,
            String expenseDate) {}

    public record DateRangeReportResult(
            String from,
            String to,
            BigDecimal totalAmount,
            long expenseCount,
            BigDecimal averagePerDay,
            String highestCategory,
            List<CategoryLine> categories,
            List<TopExpenseLine> topExpenses) {}

    public record ExpenseLine(
            Long id,
            BigDecimal amount,
            String category,
            String description,
            String expenseDate) {}

    public record ExpenseSearchResult(
            List<ExpenseLine> items, long totalElements, int returnedCount) {}

    public record ExpenseResult(
            Long id, BigDecimal amount, String category, String description, String expenseDate) {}

    public record PendingResult(String actionId, String type, String summary, String expiresAt) {}
}