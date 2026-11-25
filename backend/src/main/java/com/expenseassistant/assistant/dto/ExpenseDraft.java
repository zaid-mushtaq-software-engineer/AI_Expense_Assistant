package com.expenseassistant.assistant.dto;

import com.expenseassistant.expense.dto.ExpenseWriteFields;
import com.expenseassistant.expense.entity.ExpenseCategory;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Nullable fields from LLM extraction; null means not stated or invalid after post-processing. */
public record ExpenseDraft(BigDecimal amount, ExpenseCategory category, String description, LocalDate expenseDate)
        implements ExpenseWriteFields {}