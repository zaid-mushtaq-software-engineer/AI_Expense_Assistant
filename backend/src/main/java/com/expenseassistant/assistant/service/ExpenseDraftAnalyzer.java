package com.expenseassistant.assistant.service;

import com.expenseassistant.assistant.dto.ExpenseDraft;
import com.expenseassistant.assistant.dto.ExpenseExtractionResponse;
import com.expenseassistant.expense.service.ExpenseWriteValidator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ExpenseDraftAnalyzer {

    private final ExpenseWriteValidator expenseWriteValidator;

    public ExpenseDraftAnalyzer(ExpenseWriteValidator expenseWriteValidator) {
        this.expenseWriteValidator = expenseWriteValidator;
    }

    public ExpenseExtractionResponse analyze(ExpenseDraft sanitized) {
        LocalDate expenseDate = sanitized.expenseDate();
        if (expenseDate == null) {
            expenseDate = expenseWriteValidator.defaultDateWhenUnmentioned();
        }
        ExpenseDraft draft =
                new ExpenseDraft(sanitized.amount(), sanitized.category(), sanitized.description(), expenseDate);

        List<String> missing = new ArrayList<>();
        if (draft.amount() == null) {
            missing.add("amount");
        }
        if (draft.category() == null) {
            missing.add("category");
        }

        if (missing.isEmpty()) {
            expenseWriteValidator.validate(draft);
        }

        String question = missing.isEmpty() ? null : buildQuestion(missing);
        return new ExpenseExtractionResponse(draft, List.copyOf(missing), question);
    }

    private static String buildQuestion(List<String> missing) {
        boolean needAmount = missing.contains("amount");
        boolean needCategory = missing.contains("category");
        if (needAmount && needCategory) {
            return "How much was it, and which category does it belong to?";
        }
        if (needAmount) {
            return "How much was it?";
        }
        if (needCategory) {
            return "Which category does it belong to?";
        }
        return null;
    }
}