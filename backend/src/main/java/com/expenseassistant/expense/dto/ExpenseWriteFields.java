package com.expenseassistant.expense.dto;

import com.expenseassistant.expense.entity.ExpenseCategory;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Shared shape and validation surface for create and full-replacement update. */
public interface ExpenseWriteFields {

    BigDecimal amount();

    ExpenseCategory category();

    String description();

    LocalDate expenseDate();
}