package com.expenseassistant.analytics.dto;

import com.expenseassistant.expense.entity.ExpenseCategory;
import java.math.BigDecimal;
import java.util.List;

public record MonthlySummaryResponse(
        int year,
        int month,
        BigDecimal totalAmount,
        long expenseCount,
        ExpenseCategory highestCategory,
        BigDecimal previousMonthTotal,
        BigDecimal changeFromPreviousMonth,
        List<CategoryTotal> categories) {}