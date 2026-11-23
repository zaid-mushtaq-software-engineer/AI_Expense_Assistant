package com.expenseassistant.analytics.dto;

import com.expenseassistant.expense.entity.ExpenseCategory;
import java.math.BigDecimal;

public record CategoryTotal(
        ExpenseCategory category, BigDecimal amount, BigDecimal percentage, long expenseCount) {}