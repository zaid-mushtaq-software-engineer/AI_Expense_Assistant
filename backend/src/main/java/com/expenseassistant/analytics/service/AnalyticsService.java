package com.expenseassistant.analytics.service;

import com.expenseassistant.analytics.dto.CategorySpendingResponse;
import com.expenseassistant.analytics.dto.CategoryTotal;
import com.expenseassistant.analytics.dto.DateRangeReportResponse;
import com.expenseassistant.analytics.dto.HighestCategoryResponse;
import com.expenseassistant.analytics.dto.MonthlySummaryResponse;
import com.expenseassistant.analytics.dto.TotalSpendingResponse;
import com.expenseassistant.analytics.repository.ExpenseAnalyticsRepository;
import com.expenseassistant.analytics.repository.ExpenseAnalyticsRepository.CategoryAggregateRow;
import com.expenseassistant.analytics.repository.ExpenseAnalyticsRepository.TotalAggregateRow;
import com.expenseassistant.analytics.support.AnalyticsRangeSupport;
import com.expenseassistant.expense.entity.ExpenseCategory;
import com.expenseassistant.expense.mapper.ExpenseMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spending aggregates for a single user. Tie rule for highest category: highest amount wins; if
 * tied, first category name in ascending alphabetical order.
 */
@Service
public class AnalyticsService {

    private static final int TOP_EXPENSE_LIMIT = 5;

    private final ExpenseAnalyticsRepository expenseAnalyticsRepository;
    private final ExpenseMapper expenseMapper;

    public AnalyticsService(ExpenseAnalyticsRepository expenseAnalyticsRepository, ExpenseMapper expenseMapper) {
        this.expenseAnalyticsRepository = expenseAnalyticsRepository;
        this.expenseMapper = expenseMapper;
    }

    @Transactional(readOnly = true)
    public TotalSpendingResponse total(Long userId, LocalDate from, LocalDate to) {
        AnalyticsRangeSupport.validateDateRange(from, to);
        TotalAggregateRow aggregate = expenseAnalyticsRepository.sumTotal(userId, from, to);
        BigDecimal total = AnalyticsRangeSupport.roundMoney(aggregate.totalAmount());
        return new TotalSpendingResponse(from, to, total, aggregate.expenseCount());
    }

    @Transactional(readOnly = true)
    public CategorySpendingResponse byCategory(Long userId, LocalDate from, LocalDate to, ExpenseCategory category) {
        AnalyticsRangeSupport.validateDateRange(from, to);
        TotalAggregateRow totalAggregate = expenseAnalyticsRepository.sumTotal(userId, from, to);
        BigDecimal periodTotal = AnalyticsRangeSupport.roundMoney(totalAggregate.totalAmount());
        List<CategoryTotal> categories = toCategoryTotals(
                expenseAnalyticsRepository.sumByCategory(userId, from, to, category), periodTotal);
        return new CategorySpendingResponse(from, to, periodTotal, categories);
    }

    @Transactional(readOnly = true)
    public HighestCategoryResponse highestCategory(Long userId, LocalDate from, LocalDate to) {
        CategorySpendingResponse breakdown = byCategory(userId, from, to, null);
        if (breakdown.categories().isEmpty()) {
            return new HighestCategoryResponse(from, to, null, null);
        }
        CategoryTotal top = breakdown.categories().getFirst();
        return new HighestCategoryResponse(from, to, top.category(), top.amount());
    }

    @Transactional(readOnly = true)
    public MonthlySummaryResponse monthlySummary(Long userId, int year, int month) {
        AnalyticsRangeSupport.validateYearMonth(year, month);
        LocalDate[] bounds = AnalyticsRangeSupport.monthBounds(year, month);
        LocalDate from = bounds[0];
        LocalDate to = bounds[1];

        TotalSpendingResponse current = total(userId, from, to);
        CategorySpendingResponse categories = byCategory(userId, from, to, null);

        YearMonth previous = AnalyticsRangeSupport.previousMonth(year, month);
        LocalDate[] previousBounds = AnalyticsRangeSupport.monthBounds(previous.getYear(), previous.getMonthValue());
        TotalSpendingResponse previousMonth = total(userId, previousBounds[0], previousBounds[1]);

        ExpenseCategory highest = categories.categories().isEmpty() ? null : categories.categories().getFirst().category();
        BigDecimal change = AnalyticsRangeSupport.roundMoney(
                current.totalAmount().subtract(previousMonth.totalAmount()));

        return new MonthlySummaryResponse(
                year,
                month,
                current.totalAmount(),
                current.expenseCount(),
                highest,
                previousMonth.totalAmount(),
                change,
                categories.categories());
    }

    @Transactional(readOnly = true)
    public DateRangeReportResponse report(Long userId, LocalDate from, LocalDate to) {
        AnalyticsRangeSupport.validateDateRange(from, to);
        TotalSpendingResponse total = total(userId, from, to);
        CategorySpendingResponse categories = byCategory(userId, from, to, null);
        HighestCategoryResponse highest = highestCategory(userId, from, to);

        long days = AnalyticsRangeSupport.inclusiveDayCount(from, to);
        BigDecimal averagePerDay = AnalyticsRangeSupport.roundMoney(
                total.totalAmount().divide(BigDecimal.valueOf(days), 4, java.math.RoundingMode.HALF_UP));

        var topExpenses = expenseAnalyticsRepository.findTopExpensesByAmount(userId, from, to, TOP_EXPENSE_LIMIT).stream()
                .map(expenseMapper::toResponse)
                .toList();

        return new DateRangeReportResponse(
                from,
                to,
                total.totalAmount(),
                total.expenseCount(),
                averagePerDay,
                highest.category(),
                categories.categories(),
                topExpenses);
    }

    private List<CategoryTotal> toCategoryTotals(List<CategoryAggregateRow> rows, BigDecimal periodTotal) {
        return rows.stream()
                .map(row -> {
                    BigDecimal amount = AnalyticsRangeSupport.roundMoney(row.amount());
                    BigDecimal percentage = AnalyticsRangeSupport.percentageOf(amount, periodTotal);
                    return new CategoryTotal(row.category(), amount, percentage, row.expenseCount());
                })
                .toList();
    }
}