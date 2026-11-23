package com.expenseassistant.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expenseassistant.analytics.dto.CategorySpendingResponse;
import com.expenseassistant.analytics.dto.HighestCategoryResponse;
import com.expenseassistant.analytics.dto.MonthlySummaryResponse;
import com.expenseassistant.analytics.dto.TotalSpendingResponse;
import com.expenseassistant.analytics.repository.ExpenseAnalyticsRepository;
import com.expenseassistant.analytics.repository.ExpenseAnalyticsRepository.CategoryAggregateRow;
import com.expenseassistant.analytics.repository.ExpenseAnalyticsRepository.TotalAggregateRow;
import com.expenseassistant.expense.entity.ExpenseCategory;
import com.expenseassistant.expense.mapper.ExpenseMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    private static final long USER_ID = 1L;
    private static final LocalDate FROM = LocalDate.of(2026, 11, 1);
    private static final LocalDate TO = LocalDate.of(2026, 11, 30);

    @Mock
    private ExpenseAnalyticsRepository expenseAnalyticsRepository;

    @Mock
    private ExpenseMapper expenseMapper;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void total_usesZeroWhenNoRows() {
        when(expenseAnalyticsRepository.sumTotal(USER_ID, FROM, TO))
                .thenReturn(new TotalAggregateRow(BigDecimal.ZERO, 0));

        TotalSpendingResponse response = analyticsService.total(USER_ID, FROM, TO);

        assertThat(response.totalAmount()).isEqualByComparingTo("0.00");
        assertThat(response.expenseCount()).isZero();
    }

    @Test
    void byCategory_computesPercentagesAgainstPeriodTotal() {
        when(expenseAnalyticsRepository.sumTotal(USER_ID, FROM, TO))
                .thenReturn(new TotalAggregateRow(new BigDecimal("1000.00"), 4));
        when(expenseAnalyticsRepository.sumByCategory(USER_ID, FROM, TO, null))
                .thenReturn(List.of(
                        new CategoryAggregateRow(ExpenseCategory.BILLS, new BigDecimal("750.00"), 2),
                        new CategoryAggregateRow(ExpenseCategory.FOOD, new BigDecimal("250.00"), 2)));

        CategorySpendingResponse response = analyticsService.byCategory(USER_ID, FROM, TO, null);

        assertThat(response.totalAmount()).isEqualByComparingTo("1000.00");
        assertThat(response.categories()).hasSize(2);
        assertThat(response.categories().get(0).category()).isEqualTo(ExpenseCategory.BILLS);
        assertThat(response.categories().get(0).percentage()).isEqualByComparingTo("75.00");
        assertThat(response.categories().get(1).percentage()).isEqualByComparingTo("25.00");
    }

    @Test
    void highestCategory_reusesByCategoryWithoutSecondQuery() {
        when(expenseAnalyticsRepository.sumTotal(USER_ID, FROM, TO))
                .thenReturn(new TotalAggregateRow(new BigDecimal("100.00"), 2));
        when(expenseAnalyticsRepository.sumByCategory(USER_ID, FROM, TO, null))
                .thenReturn(List.of(
                        new CategoryAggregateRow(ExpenseCategory.BILLS, new BigDecimal("60.00"), 1),
                        new CategoryAggregateRow(ExpenseCategory.FOOD, new BigDecimal("40.00"), 1)));

        HighestCategoryResponse response = analyticsService.highestCategory(USER_ID, FROM, TO);

        assertThat(response.category()).isEqualTo(ExpenseCategory.BILLS);
        assertThat(response.amount()).isEqualByComparingTo("60.00");
        verify(expenseAnalyticsRepository).sumByCategory(USER_ID, FROM, TO, null);
    }

    @Test
    void byCategory_passesCategoryFilter() {
        when(expenseAnalyticsRepository.sumTotal(USER_ID, FROM, TO))
                .thenReturn(new TotalAggregateRow(new BigDecimal("500.00"), 1));
        when(expenseAnalyticsRepository.sumByCategory(USER_ID, FROM, TO, ExpenseCategory.FOOD))
                .thenReturn(List.of(
                        new CategoryAggregateRow(ExpenseCategory.FOOD, new BigDecimal("500.00"), 1)));

        analyticsService.byCategory(USER_ID, FROM, TO, ExpenseCategory.FOOD);

        verify(expenseAnalyticsRepository).sumByCategory(USER_ID, FROM, TO, ExpenseCategory.FOOD);
    }

    @Test
    void monthlySummary_rollsPreviousMonthFromJanuary() {
        LocalDate janFrom = LocalDate.of(2026, 1, 1);
        LocalDate janTo = LocalDate.of(2026, 1, 31);
        LocalDate decFrom = LocalDate.of(2025, 12, 1);
        LocalDate decTo = LocalDate.of(2025, 12, 31);

        when(expenseAnalyticsRepository.sumTotal(USER_ID, janFrom, janTo))
                .thenReturn(new TotalAggregateRow(new BigDecimal("100.00"), 1));
        when(expenseAnalyticsRepository.sumTotal(USER_ID, decFrom, decTo))
                .thenReturn(new TotalAggregateRow(new BigDecimal("250.00"), 2));
        when(expenseAnalyticsRepository.sumByCategory(eq(USER_ID), eq(janFrom), eq(janTo), any()))
                .thenReturn(List.of(
                        new CategoryAggregateRow(ExpenseCategory.FOOD, new BigDecimal("100.00"), 1)));

        MonthlySummaryResponse response = analyticsService.monthlySummary(USER_ID, 2026, 1);

        assertThat(response.previousMonthTotal()).isEqualByComparingTo("250.00");
        assertThat(response.changeFromPreviousMonth()).isEqualByComparingTo("-150.00");
        assertThat(response.highestCategory()).isEqualTo(ExpenseCategory.FOOD);
    }

    @Test
    void highestCategory_emptyRangeReturnsNulls() {
        when(expenseAnalyticsRepository.sumTotal(USER_ID, FROM, TO))
                .thenReturn(new TotalAggregateRow(BigDecimal.ZERO, 0));
        when(expenseAnalyticsRepository.sumByCategory(eq(USER_ID), eq(FROM), eq(TO), any()))
                .thenReturn(List.of());

        HighestCategoryResponse response = analyticsService.highestCategory(USER_ID, FROM, TO);

        assertThat(response.category()).isNull();
        assertThat(response.amount()).isNull();
    }
}