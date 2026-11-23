package com.expenseassistant.analytics.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.expenseassistant.analytics.dto.CategorySpendingResponse;
import com.expenseassistant.analytics.dto.HighestCategoryResponse;
import com.expenseassistant.analytics.dto.MonthlySummaryResponse;
import com.expenseassistant.analytics.dto.TotalSpendingResponse;
import com.expenseassistant.analytics.service.AnalyticsService;
import com.expenseassistant.common.exception.GlobalExceptionHandler;
import com.expenseassistant.common.exception.ValidationFailedException;
import com.expenseassistant.expense.entity.ExpenseCategory;
import com.expenseassistant.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AnalyticsController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc
class AnalyticsControllerTest {

    private static final String ANALYTICS = "/api/v1/analytics";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsService analyticsService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void total_returns200() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(1L);
        when(analyticsService.total(eq(1L), eq(LocalDate.of(2026, 11, 1)), eq(LocalDate.of(2026, 11, 30))))
                .thenReturn(new TotalSpendingResponse(
                        LocalDate.of(2026, 11, 1),
                        LocalDate.of(2026, 11, 30),
                        new BigDecimal("1250.50"),
                        3));

        mockMvc.perform(get(ANALYTICS + "/total")
                        .queryParam("from", "2026-11-01")
                        .queryParam("to", "2026-11-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(1250.50))
                .andExpect(jsonPath("$.expenseCount").value(3));
    }

    @Test
    void byCategory_withOptionalFilter() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(1L);
        when(analyticsService.byCategory(
                        eq(1L), eq(LocalDate.of(2026, 11, 1)), eq(LocalDate.of(2026, 11, 30)), eq(ExpenseCategory.FOOD)))
                .thenReturn(new CategorySpendingResponse(
                        LocalDate.of(2026, 11, 1),
                        LocalDate.of(2026, 11, 30),
                        new BigDecimal("500.00"),
                        List.of()));

        mockMvc.perform(get(ANALYTICS + "/by-category")
                        .queryParam("from", "2026-11-01")
                        .queryParam("to", "2026-11-30")
                        .queryParam("category", "FOOD"))
                .andExpect(status().isOk());
    }

    @Test
    void highestCategory_empty() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(1L);
        when(analyticsService.highestCategory(eq(1L), eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 1, 31))))
                .thenReturn(new HighestCategoryResponse(
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), null, null));

        mockMvc.perform(get(ANALYTICS + "/highest-category")
                        .queryParam("from", "2026-01-01")
                        .queryParam("to", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").doesNotExist())
                .andExpect(jsonPath("$.amount").doesNotExist());
    }

    @Test
    void monthly_returns200() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(1L);
        when(analyticsService.monthlySummary(1L, 2026, 11))
                .thenReturn(new MonthlySummaryResponse(
                        2026,
                        11,
                        new BigDecimal("100.00"),
                        2L,
                        ExpenseCategory.FOOD,
                        new BigDecimal("80.00"),
                        new BigDecimal("20.00"),
                        List.of()));

        mockMvc.perform(get(ANALYTICS + "/monthly").queryParam("year", "2026").queryParam("month", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changeFromPreviousMonth").value(20.00));
    }

    @Test
    void total_invalidRange_returns400() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(1L);
        when(analyticsService.total(eq(1L), eq(LocalDate.of(2026, 2, 1)), eq(LocalDate.of(2026, 1, 1))))
                .thenThrow(new ValidationFailedException("from must be on or before to"));

        mockMvc.perform(get(ANALYTICS + "/total")
                        .queryParam("from", "2026-02-01")
                        .queryParam("to", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}