package com.expenseassistant.expense.dto;

import com.expenseassistant.expense.entity.ExpenseCategory;
import java.time.LocalDate;

public record ExpenseFilter(LocalDate from, LocalDate to, ExpenseCategory category, String q) {}