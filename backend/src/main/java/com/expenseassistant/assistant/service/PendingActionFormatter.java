package com.expenseassistant.assistant.service;

import com.expenseassistant.expense.dto.ExpenseResponse;
import com.expenseassistant.expense.dto.UpdateExpenseRequest;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PendingActionFormatter {

    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    public String formatDelete(ExpenseResponse expense) {
        String desc = descriptionLabel(expense.description());
        return "Delete %s %s (%s) on %s?"
                .formatted(
                        formatAmount(expense.amount()),
                        desc,
                        expense.category().name(),
                        DISPLAY_DATE.format(expense.expenseDate()));
    }

    public String formatUpdate(ExpenseResponse before, UpdateExpenseRequest after) {
        List<String> changes = new ArrayList<>();
        if (before.amount().compareTo(after.amount()) != 0) {
            changes.add("Amount: %s → %s".formatted(formatAmount(before.amount()), formatAmount(after.amount())));
        }
        if (before.category() != after.category()) {
            changes.add("Category: %s → %s".formatted(before.category().name(), after.category().name()));
        }
        if (!Objects.equals(normalizeDescription(before.description()), normalizeDescription(after.description()))) {
            changes.add(
                    "Description: %s → %s"
                            .formatted(
                                    labelOrDash(before.description()),
                                    labelOrDash(after.description())));
        }
        if (!before.expenseDate().equals(after.expenseDate())) {
            changes.add(
                    "Date: %s → %s"
                            .formatted(
                                    DISPLAY_DATE.format(before.expenseDate()),
                                    DISPLAY_DATE.format(after.expenseDate())));
        }
        return "Update expense #" + before.id() + ": " + String.join("; ", changes) + "?";
    }

    public String confirmDeleteMessage(String summary) {
        return "Deleted: " + stripTrailingQuestion(summary);
    }

    public String confirmUpdateMessage(String summary) {
        return "Updated: " + stripTrailingQuestion(summary);
    }

    private static String stripTrailingQuestion(String summary) {
        if (summary != null && summary.endsWith("?")) {
            return summary.substring(0, summary.length() - 1);
        }
        return summary;
    }

    private static String formatAmount(BigDecimal amount) {
        return "₹" + amount.stripTrailingZeros().toPlainString();
    }

    private static String descriptionLabel(String description) {
        return StringUtils.hasText(description) ? description.trim() : "Expense";
    }

    private static String labelOrDash(String description) {
        return StringUtils.hasText(description) ? description.trim() : "—";
    }

    private static String normalizeDescription(String description) {
        return StringUtils.hasText(description) ? description.trim() : null;
    }
}