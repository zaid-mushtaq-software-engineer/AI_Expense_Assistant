package com.expenseassistant.expense.dto;

import com.expenseassistant.expense.entity.ExpenseCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateExpenseRequest(
        @NotNull
                @DecimalMin(value = "0.01", message = "must be greater than 0")
                @DecimalMax("9999999.99")
                @Digits(integer = 7, fraction = 2)
                BigDecimal amount,
        @NotNull ExpenseCategory category,
        @Size(max = 255) String description,
        @NotNull LocalDate expenseDate)
        implements ExpenseWriteFields {}