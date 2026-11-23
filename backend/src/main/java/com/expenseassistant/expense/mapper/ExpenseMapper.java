package com.expenseassistant.expense.mapper;

import com.expenseassistant.expense.dto.ExpenseResponse;
import com.expenseassistant.expense.entity.Expense;
import org.springframework.stereotype.Component;

@Component
public class ExpenseMapper {

    public ExpenseResponse toResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getDescription(),
                expense.getExpenseDate(),
                expense.getCreatedAt(),
                expense.getUpdatedAt());
    }
}