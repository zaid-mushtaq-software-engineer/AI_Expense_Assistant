package com.expenseassistant.assistant.tools;

import com.expenseassistant.analytics.dto.CategorySpendingResponse;
import com.expenseassistant.analytics.dto.CategoryTotal;
import com.expenseassistant.analytics.dto.DateRangeReportResponse;
import com.expenseassistant.analytics.dto.HighestCategoryResponse;
import com.expenseassistant.analytics.dto.MonthlySummaryResponse;
import com.expenseassistant.analytics.dto.TotalSpendingResponse;
import com.expenseassistant.analytics.service.AnalyticsService;
import com.expenseassistant.assistant.tools.ToolResults.CategoryLine;
import com.expenseassistant.assistant.tools.ToolResults.CategorySpendingResult;
import com.expenseassistant.assistant.tools.ToolResults.DateRangeReportResult;
import com.expenseassistant.assistant.tools.ToolResults.HighestCategoryResult;
import com.expenseassistant.assistant.tools.ToolResults.MonthlySummaryResult;
import com.expenseassistant.assistant.tools.ToolResults.ToolOutcome;
import com.expenseassistant.assistant.tools.ToolResults.TopExpenseLine;
import com.expenseassistant.assistant.tools.ToolResults.TotalSpendingResult;
import com.expenseassistant.expense.dto.ExpenseResponse;
import com.expenseassistant.expense.entity.ExpenseCategory;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "google-genai")
public class AnalyticsTools {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsTools.class);

    private final AnalyticsService analyticsService;
    private final AssistantToolSupport toolSupport;

    public AnalyticsTools(AnalyticsService analyticsService, AssistantToolSupport toolSupport) {
        this.analyticsService = analyticsService;
        this.toolSupport = toolSupport;
    }

    @Tool(
            description =
                    "Total spending between two dates for the current user. Dates are inclusive ISO yyyy-MM-dd.")
    public ToolOutcome<TotalSpendingResult> getTotalSpending(
            @ToolParam(description = "Start date (yyyy-MM-dd inclusive)") String from,
            @ToolParam(description = "End date (yyyy-MM-dd inclusive)") String to,
            ToolContext toolContext) {
        log.debug("getTotalSpending from={} to={}", from, to);
        return toolSupport.runTool(
                "getTotalSpending",
                toolContext,
                userId -> {
                    LocalDate fromDate = AssistantToolSupport.parseIsoDate(from);
                    LocalDate toDate = AssistantToolSupport.parseIsoDate(to);
                    return analyticsService.total(userId, fromDate, toDate);
                },
                this::toTotalSpendingResult,
                "Could not complete the analytics request.");
    }

    @Tool(
            description =
                    "Category spending breakdown between two dates (inclusive ISO yyyy-MM-dd). "
                            + "Use optional category to filter one category (e.g. FOOD). Omit category for full breakdown.")
    public ToolOutcome<CategorySpendingResult> getCategorySpending(
            @ToolParam(description = "Start date (yyyy-MM-dd inclusive)") String from,
            @ToolParam(description = "End date (yyyy-MM-dd inclusive)") String to,
            @ToolParam(description = "Optional category filter", required = false) String category,
            ToolContext toolContext) {
        log.debug("getCategorySpending from={} to={} category={}", from, to, category);
        return toolSupport.runTool(
                "getCategorySpending",
                toolContext,
                userId -> {
                    LocalDate fromDate = AssistantToolSupport.parseIsoDate(from);
                    LocalDate toDate = AssistantToolSupport.parseIsoDate(to);
                    ExpenseCategory categoryFilter = AssistantToolSupport.parseCategoryOptional(category);
                    return analyticsService.byCategory(userId, fromDate, toDate, categoryFilter);
                },
                this::toCategorySpendingResult,
                "Could not complete the analytics request.");
    }

    @Tool(
            description =
                    "Which expense category had the highest spending between two dates (inclusive ISO yyyy-MM-dd).")
    public ToolOutcome<HighestCategoryResult> getHighestSpendingCategory(
            @ToolParam(description = "Start date (yyyy-MM-dd inclusive)") String from,
            @ToolParam(description = "End date (yyyy-MM-dd inclusive)") String to,
            ToolContext toolContext) {
        log.debug("getHighestSpendingCategory from={} to={}", from, to);
        return toolSupport.runTool(
                "getHighestSpendingCategory",
                toolContext,
                userId -> {
                    LocalDate fromDate = AssistantToolSupport.parseIsoDate(from);
                    LocalDate toDate = AssistantToolSupport.parseIsoDate(to);
                    return analyticsService.highestCategory(userId, fromDate, toDate);
                },
                this::toHighestCategoryResult,
                "Could not complete the analytics request.");
    }

    @Tool(
            description =
                    "Monthly spending summary for a calendar month: total, count, category breakdown, "
                            + "previous month total, and change. Use for 'this month' or a named month.")
    public ToolOutcome<MonthlySummaryResult> getMonthlySummary(
            @ToolParam(description = "Four-digit year, e.g. 2026") int year,
            @ToolParam(description = "Month 1-12") int month,
            ToolContext toolContext) {
        log.debug("getMonthlySummary year={} month={}", year, month);
        return toolSupport.runTool(
                "getMonthlySummary",
                toolContext,
                userId -> analyticsService.monthlySummary(userId, year, month),
                this::toMonthlySummaryResult,
                "Could not complete the analytics request.");
    }

    @Tool(
            description =
                    "Detailed date-range report: total, count, average per day, category breakdown, "
                            + "highest category, and top five expenses. Dates inclusive ISO yyyy-MM-dd.")
    public ToolOutcome<DateRangeReportResult> getDateRangeReport(
            @ToolParam(description = "Start date (yyyy-MM-dd inclusive)") String from,
            @ToolParam(description = "End date (yyyy-MM-dd inclusive)") String to,
            ToolContext toolContext) {
        log.debug("getDateRangeReport from={} to={}", from, to);
        return toolSupport.runTool(
                "getDateRangeReport",
                toolContext,
                userId -> {
                    LocalDate fromDate = AssistantToolSupport.parseIsoDate(from);
                    LocalDate toDate = AssistantToolSupport.parseIsoDate(to);
                    return analyticsService.report(userId, fromDate, toDate);
                },
                this::toDateRangeReportResult,
                "Could not complete the analytics request.");
    }

    private TotalSpendingResult toTotalSpendingResult(TotalSpendingResponse response) {
        return new TotalSpendingResult(
                response.from().toString(),
                response.to().toString(),
                response.totalAmount(),
                response.expenseCount());
    }

    private CategorySpendingResult toCategorySpendingResult(CategorySpendingResponse response) {
        return new CategorySpendingResult(
                response.from().toString(),
                response.to().toString(),
                response.totalAmount(),
                toCategoryLines(response.categories()));
    }

    private HighestCategoryResult toHighestCategoryResult(HighestCategoryResponse response) {
        String category = response.category() != null ? response.category().name() : null;
        return new HighestCategoryResult(
                response.from().toString(), response.to().toString(), category, response.amount());
    }

    private MonthlySummaryResult toMonthlySummaryResult(MonthlySummaryResponse response) {
        String highest = response.highestCategory() != null ? response.highestCategory().name() : null;
        return new MonthlySummaryResult(
                response.year(),
                response.month(),
                response.totalAmount(),
                response.expenseCount(),
                highest,
                response.previousMonthTotal(),
                response.changeFromPreviousMonth(),
                toCategoryLines(response.categories()));
    }

    private DateRangeReportResult toDateRangeReportResult(DateRangeReportResponse response) {
        String highest = response.highestCategory() != null ? response.highestCategory().name() : null;
        List<TopExpenseLine> top =
                response.topExpenses().stream().map(this::toTopExpenseLine).toList();
        return new DateRangeReportResult(
                response.from().toString(),
                response.to().toString(),
                response.totalAmount(),
                response.expenseCount(),
                response.averagePerDay(),
                highest,
                toCategoryLines(response.categories()),
                top);
    }

    private TopExpenseLine toTopExpenseLine(ExpenseResponse expense) {
        return new TopExpenseLine(
                expense.id(),
                expense.amount(),
                expense.category().name(),
                expense.description(),
                expense.expenseDate().toString());
    }

    private List<CategoryLine> toCategoryLines(List<CategoryTotal> categories) {
        return categories.stream()
                .map(row -> new CategoryLine(
                        row.category().name(), row.amount(), row.percentage(), row.expenseCount()))
                .toList();
    }
}