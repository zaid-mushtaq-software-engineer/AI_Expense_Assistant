package com.expenseassistant.analytics.dto;

import com.expenseassistant.expense.dto.ExpenseResponse;
import com.expenseassistant.expense.entity.ExpenseCategory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DateRangeReportResponse(
        LocalDate from,
        LocalDate to,
        BigDecimal totalAmount,
        long expenseCount,
        BigDecimal averagePerDay,
        ExpenseCategory highestCategory,
        List<CategoryTotal> categories,
        List<ExpenseResponse> topExpenses) {}