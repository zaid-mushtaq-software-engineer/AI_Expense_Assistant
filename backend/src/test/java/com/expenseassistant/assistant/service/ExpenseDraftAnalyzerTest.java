package com.expenseassistant.assistant.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.expenseassistant.assistant.dto.ExpenseDraft;
import com.expenseassistant.assistant.dto.ExpenseExtractionResponse;
import com.expenseassistant.expense.entity.ExpenseCategory;
import com.expenseassistant.expense.service.ExpenseWriteValidator;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExpenseDraftAnalyzerTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 25);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-25T06:30:00Z"), ZONE);

    private ExpenseDraftAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        ExpenseWriteValidator validator =
                new ExpenseWriteValidator(Validation.buildDefaultValidatorFactory().getValidator(), CLOCK);
        analyzer = new ExpenseDraftAnalyzer(validator);
    }

    @Test
    void analyze_completeDraft_noMissing() {
        ExpenseDraft sanitized =
                new ExpenseDraft(
                        new BigDecimal("500.00"), ExpenseCategory.FOOD, "Dinner", LocalDate.of(2026, 11, 24));

        ExpenseExtractionResponse response = analyzer.analyze(sanitized);

        assertThat(response.missing()).isEmpty();
        assertThat(response.question()).isNull();
        assertThat(response.draft().amount()).isEqualByComparingTo("500.00");
        assertThat(response.draft().category()).isEqualTo(ExpenseCategory.FOOD);
    }

    @Test
    void analyze_missingAmount_asksHowMuch() {
        ExpenseDraft sanitized = new ExpenseDraft(null, ExpenseCategory.FOOD, "coffee", null);

        ExpenseExtractionResponse response = analyzer.analyze(sanitized);

        assertThat(response.missing()).containsExactly("amount");
        assertThat(response.question()).isEqualTo("How much was it?");
        assertThat(response.draft().expenseDate()).isEqualTo(TODAY);
    }

    @Test
    void analyze_missingCategory_asksCategory() {
        ExpenseDraft sanitized = new ExpenseDraft(new BigDecimal("500"), null, "stuff", null);

        ExpenseExtractionResponse response = analyzer.analyze(sanitized);

        assertThat(response.missing()).containsExactly("category");
        assertThat(response.question()).isEqualTo("Which category does it belong to?");
    }

    @Test
    void analyze_missingAmountAndCategory_combinedQuestion() {
        ExpenseDraft sanitized = new ExpenseDraft(null, null, "coffee", null);

        ExpenseExtractionResponse response = analyzer.analyze(sanitized);

        assertThat(response.missing()).containsExactly("amount", "category");
        assertThat(response.question()).isEqualTo("How much was it, and which category does it belong to?");
        assertThat(response.draft().expenseDate()).isEqualTo(TODAY);
    }
}