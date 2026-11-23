package com.expenseassistant.expense.dto;

import com.expenseassistant.expense.entity.ExpenseCategory;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ExpenseResponse(
        Long id,
        BigDecimal amount,
        ExpenseCategory category,
        String description,
        LocalDate expenseDate,
        Instant createdAt,
        Instant updatedAt) {}