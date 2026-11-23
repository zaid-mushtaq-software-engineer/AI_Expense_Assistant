package com.expenseassistant.analytics.controller;

import com.expenseassistant.analytics.dto.CategorySpendingResponse;
import com.expenseassistant.analytics.dto.DateRangeReportResponse;
import com.expenseassistant.analytics.dto.HighestCategoryResponse;
import com.expenseassistant.analytics.dto.MonthlySummaryResponse;
import com.expenseassistant.analytics.dto.TotalSpendingResponse;
import com.expenseassistant.analytics.service.AnalyticsService;
import com.expenseassistant.expense.entity.ExpenseCategory;
import com.expenseassistant.security.CurrentUserProvider;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final CurrentUserProvider currentUserProvider;

    public AnalyticsController(AnalyticsService analyticsService, CurrentUserProvider currentUserProvider) {
        this.analyticsService = analyticsService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/total")
    public TotalSpendingResponse total(
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate to) {
        return analyticsService.total(currentUserProvider.requireUserId(), from, to);
    }

    @GetMapping("/by-category")
    public CategorySpendingResponse byCategory(
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate to,
            @RequestParam(required = false) ExpenseCategory category) {
        return analyticsService.byCategory(currentUserProvider.requireUserId(), from, to, category);
    }

    @GetMapping("/highest-category")
    public HighestCategoryResponse highestCategory(
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate to) {
        return analyticsService.highestCategory(currentUserProvider.requireUserId(), from, to);
    }

    @GetMapping("/monthly")
    public MonthlySummaryResponse monthly(@RequestParam int year, @RequestParam int month) {
        return analyticsService.monthlySummary(currentUserProvider.requireUserId(), year, month);
    }

    @GetMapping("/report")
    public DateRangeReportResponse report(
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate to) {
        return analyticsService.report(currentUserProvider.requireUserId(), from, to);
    }
}