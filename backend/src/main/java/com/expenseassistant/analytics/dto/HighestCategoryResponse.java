package com.expenseassistant.analytics.dto;

import com.expenseassistant.expense.entity.ExpenseCategory;
import java.math.BigDecimal;
import java.time.LocalDate;

public record HighestCategoryResponse(
        LocalDate from, LocalDate to, ExpenseCategory category, BigDecimal amount) {}