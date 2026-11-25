package com.expenseassistant.assistant.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expenseassistant.analytics.dto.CategorySpendingResponse;
import com.expenseassistant.analytics.dto.CategoryTotal;
import com.expenseassistant.analytics.dto.DateRangeReportResponse;
import com.expenseassistant.analytics.dto.HighestCategoryResponse;
import com.expenseassistant.analytics.dto.MonthlySummaryResponse;
import com.expenseassistant.analytics.dto.TotalSpendingResponse;
import com.expenseassistant.analytics.service.AnalyticsService;
import com.expenseassistant.assistant.service.ChatTurnContext;
import com.expenseassistant.assistant.tools.ToolResults.ToolOutcome;
import com.expenseassistant.common.exception.ValidationFailedException;
import com.expenseassistant.expense.dto.ExpenseResponse;
import com.expenseassistant.expense.entity.ExpenseCategory;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.ToolParam;

@ExtendWith(MockitoExtension.class)
class AnalyticsToolsTest {

    private static final ToolContext USER1 = new ToolContext(Map.of(AssistantToolContextKeys.USER_ID, 1L));

    @Mock
    private AnalyticsService analyticsService;

    private final ChatTurnContext chatTurnContext = new ChatTurnContext();
    private final AssistantToolSupport toolSupport =
            AssistantToolSupportTestHelper.toolSupport(chatTurnContext);

    private AnalyticsTools analyticsTools;

    @BeforeEach
    void setUp() {
        analyticsTools = new AnalyticsTools(analyticsService, toolSupport);
    }

    @Test
    void getTotalSpending_returnsServiceTotals() {
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 25);
        when(analyticsService.total(eq(1L), eq(from), eq(to)))
                .thenReturn(new TotalSpendingResponse(from, to, new BigDecimal("1250.50"), 7));

        ToolOutcome<ToolResults.TotalSpendingResult> outcome =
                analyticsTools.getTotalSpending("2026-11-01", "2026-11-25", USER1);

        assertThat(outcome.error()).isNull();
        assertThat(outcome.success().totalAmount()).isEqualByComparingTo("1250.50");
        assertThat(outcome.success().expenseCount()).isEqualTo(7);
        verify(analyticsService).total(1L, from, to);
    }

    @Test
    void getTotalSpending_usesUserIdFromContext() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);
        when(analyticsService.total(eq(2L), eq(from), eq(to)))
                .thenReturn(new TotalSpendingResponse(from, to, BigDecimal.ZERO, 0));

        analyticsTools.getTotalSpending(
                "2026-10-01", "2026-10-31", new ToolContext(Map.of(AssistantToolContextKeys.USER_ID, 2L)));

        verify(analyticsService).total(2L, from, to);
    }

    @Test
    void getCategorySpending_mapsBreakdown() {
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 25);
        List<CategoryTotal> categories =
                List.of(new CategoryTotal(ExpenseCategory.FOOD, new BigDecimal("500.00"), new BigDecimal("100.00"), 3));
        when(analyticsService.byCategory(eq(1L), eq(from), eq(to), isNull()))
                .thenReturn(new CategorySpendingResponse(from, to, new BigDecimal("500.00"), categories));

        ToolOutcome<ToolResults.CategorySpendingResult> outcome =
                analyticsTools.getCategorySpending("2026-11-01", "2026-11-25", null, USER1);

        assertThat(outcome.error()).isNull();
        assertThat(outcome.success().categories()).hasSize(1);
        assertThat(outcome.success().categories().getFirst().category()).isEqualTo("FOOD");
    }

    @Test
    void getCategorySpending_passesCategoryFilter() {
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 25);
        when(analyticsService.byCategory(eq(1L), eq(from), eq(to), eq(ExpenseCategory.FOOD)))
                .thenReturn(new CategorySpendingResponse(from, to, BigDecimal.TEN, List.of()));

        analyticsTools.getCategorySpending("2026-11-01", "2026-11-25", "FOOD", USER1);

        verify(analyticsService).byCategory(1L, from, to, ExpenseCategory.FOOD);
    }

    @Test
    void getCategorySpending_invalidCategory_returnsError() {
        ToolOutcome<ToolResults.CategorySpendingResult> outcome =
                analyticsTools.getCategorySpending("2026-11-01", "2026-11-25", "NOT_A_CAT", USER1);

        assertThat(outcome.error().code()).isEqualTo("INVALID_CATEGORY");
    }

    @Test
    void getHighestSpendingCategory_mapsNullWhenEmpty() {
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 25);
        when(analyticsService.highestCategory(eq(1L), eq(from), eq(to)))
                .thenReturn(new HighestCategoryResponse(from, to, null, null));

        ToolOutcome<ToolResults.HighestCategoryResult> outcome =
                analyticsTools.getHighestSpendingCategory("2026-11-01", "2026-11-25", USER1);

        assertThat(outcome.success().category()).isNull();
        assertThat(outcome.success().amount()).isNull();
    }

    @Test
    void getMonthlySummary_mapsChange() {
        MonthlySummaryResponse service =
                new MonthlySummaryResponse(
                        2026,
                        11,
                        new BigDecimal("1000.00"),
                        5,
                        ExpenseCategory.FOOD,
                        new BigDecimal("800.00"),
                        new BigDecimal("200.00"),
                        List.of());
        when(analyticsService.monthlySummary(1L, 2026, 11)).thenReturn(service);

        ToolOutcome<ToolResults.MonthlySummaryResult> outcome = analyticsTools.getMonthlySummary(2026, 11, USER1);

        assertThat(outcome.success().changeFromPreviousMonth()).isEqualByComparingTo("200.00");
        assertThat(outcome.success().highestCategory()).isEqualTo("FOOD");
    }

    @Test
    void getDateRangeReport_mapsTopExpensesWithoutAuditFields() {
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 10);
        ExpenseResponse top = new ExpenseResponse(
                99L,
                new BigDecimal("500.00"),
                ExpenseCategory.FOOD,
                "Dinner",
                LocalDate.of(2026, 11, 5),
                Instant.parse("2026-11-05T10:00:00Z"),
                Instant.parse("2026-11-05T10:00:00Z"));
        DateRangeReportResponse service = new DateRangeReportResponse(
                from,
                to,
                new BigDecimal("500.00"),
                1,
                new BigDecimal("50.00"),
                ExpenseCategory.FOOD,
                List.of(),
                List.of(top));
        when(analyticsService.report(eq(1L), eq(from), eq(to))).thenReturn(service);

        ToolOutcome<ToolResults.DateRangeReportResult> outcome =
                analyticsTools.getDateRangeReport("2026-11-01", "2026-11-10", USER1);

        assertThat(outcome.success().topExpenses()).hasSize(1);
        assertThat(outcome.success().topExpenses().getFirst().id()).isEqualTo(99L);
        assertThat(outcome.success().topExpenses().getFirst().expenseDate()).isEqualTo("2026-11-05");
    }

    @Test
    void getTotalSpending_invalidDate_returnsError() {
        ToolOutcome<ToolResults.TotalSpendingResult> outcome =
                analyticsTools.getTotalSpending("not-a-date", "2026-11-25", USER1);

        assertThat(outcome.success()).isNull();
        assertThat(outcome.error().code()).isEqualTo("INVALID_DATE");
    }

    @Test
    void getTotalSpending_validationError_returnsErrorResult() {
        when(analyticsService.total(eq(1L), eq(LocalDate.of(2026, 11, 1)), eq(LocalDate.of(2026, 10, 1))))
                .thenThrow(new ValidationFailedException("from must be on or before to"));

        ToolOutcome<ToolResults.TotalSpendingResult> outcome =
                analyticsTools.getTotalSpending("2026-11-01", "2026-10-01", USER1);

        assertThat(outcome.error().code()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void getTotalSpending_missingUserId_returnsInternalError() {
        ToolOutcome<ToolResults.TotalSpendingResult> outcome =
                analyticsTools.getTotalSpending("2026-11-01", "2026-11-25", new ToolContext(Map.of()));

        assertThat(outcome.error().code()).isEqualTo("INTERNAL_ERROR");
    }

    @Test
    void getTotalSpending_exposedParamsExcludeUserId() throws NoSuchMethodException {
        assertToolParamCount("getTotalSpending", 2, String.class, String.class, ToolContext.class);
    }

    @Test
    void getCategorySpending_exposedParamsExcludeUserId() throws NoSuchMethodException {
        assertToolParamCount("getCategorySpending", 3, String.class, String.class, String.class, ToolContext.class);
    }

    @Test
    void getMonthlySummary_exposedParamsExcludeUserId() throws NoSuchMethodException {
        assertToolParamCount("getMonthlySummary", 2, int.class, int.class, ToolContext.class);
    }

    private void assertToolParamCount(String methodName, int expectedToolParams, Class<?>... paramTypes)
            throws NoSuchMethodException {
        Method method = AnalyticsTools.class.getMethod(methodName, paramTypes);
        long toolParamCount =
                Arrays.stream(method.getParameters()).filter(p -> p.isAnnotationPresent(ToolParam.class)).count();
        assertThat(toolParamCount).isEqualTo(expectedToolParams);
        assertThat(Arrays.stream(method.getParameters()).map(Parameter::getType).anyMatch(ToolContext.class::equals))
                .isTrue();
    }
}