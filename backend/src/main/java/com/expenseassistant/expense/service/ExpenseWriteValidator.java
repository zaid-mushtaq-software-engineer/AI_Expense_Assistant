package com.expenseassistant.expense.service;

import com.expenseassistant.common.exception.ValidationFailedException;
import com.expenseassistant.common.response.ErrorResponse.FieldError;
import com.expenseassistant.expense.dto.CreateExpenseRequest;
import com.expenseassistant.expense.dto.ExpenseWriteFields;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ExpenseWriteValidator {

    private final Validator validator;
    private final Clock clock;

    public ExpenseWriteValidator(Validator validator, Clock clock) {
        this.validator = validator;
        this.clock = clock;
    }

    public LocalDate defaultDateWhenUnmentioned() {
        return LocalDate.now(clock);
    }

    public void validate(ExpenseWriteFields fields) {
        validateExpenseDate(fields.expenseDate());
        CreateExpenseRequest probe = new CreateExpenseRequest(
                fields.amount(), fields.category(), fields.description(), fields.expenseDate());
        var violations = validator.validate(probe);
        if (!violations.isEmpty()) {
            List<FieldError> fieldErrors = violations.stream()
                    .map(ExpenseWriteValidator::toFieldError)
                    .toList();
            throw new ValidationFailedException("Request validation failed", fieldErrors);
        }
    }

    public boolean isExpenseDateAllowed(LocalDate expenseDate) {
        if (expenseDate == null) {
            return false;
        }
        LocalDate maxAllowed = LocalDate.now(clock).plusDays(1);
        return !expenseDate.isAfter(maxAllowed);
    }

    private void validateExpenseDate(LocalDate expenseDate) {
        if (expenseDate == null) {
            throw new ValidationFailedException("expenseDate is required");
        }
        if (!isExpenseDateAllowed(expenseDate)) {
            throw new ValidationFailedException("Expense date cannot be more than one day in the future");
        }
    }

    private static FieldError toFieldError(ConstraintViolation<CreateExpenseRequest> violation) {
        String field = violation.getPropertyPath().toString();
        return new FieldError(field, violation.getMessage());
    }
}