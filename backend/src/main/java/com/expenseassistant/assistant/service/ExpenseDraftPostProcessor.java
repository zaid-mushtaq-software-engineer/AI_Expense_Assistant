package com.expenseassistant.assistant.service;

import com.expenseassistant.assistant.dto.ExpenseDraft;
import com.expenseassistant.expense.service.ExpenseWriteValidator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ExpenseDraftPostProcessor {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999.99");

    private final ExpenseWriteValidator expenseWriteValidator;

    public ExpenseDraftPostProcessor(ExpenseWriteValidator expenseWriteValidator) {
        this.expenseWriteValidator = expenseWriteValidator;
    }

    public ExpenseDraft sanitize(ExpenseDraft raw) {
        if (raw == null) {
            return new ExpenseDraft(null, null, null, null);
        }
        return new ExpenseDraft(
                sanitizeAmount(raw.amount()),
                raw.category(),
                sanitizeDescription(raw.description()),
                sanitizeDate(raw.expenseDate()));
    }

    private BigDecimal sanitizeAmount(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        if (amount.scale() > 2) {
            return null;
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            return null;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String sanitizeDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return null;
        }
        String trimmed = description.trim();
        return trimmed.length() > 255 ? null : trimmed;
    }

    private LocalDate sanitizeDate(LocalDate expenseDate) {
        if (expenseDate == null) {
            return null;
        }
        if (!expenseWriteValidator.isExpenseDateAllowed(expenseDate)) {
            return null;
        }
        return expenseDate;
    }
}