package com.expenseassistant.assistant.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.expenseassistant.assistant.dto.ExpenseDraft;
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

class ExpenseDraftPostProcessorTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-25T06:30:00Z"), ZONE);

    private ExpenseDraftPostProcessor processor;

    @BeforeEach
    void setUp() {
        ExpenseWriteValidator validator =
                new ExpenseWriteValidator(Validation.buildDefaultValidatorFactory().getValidator(), CLOCK);
        processor = new ExpenseDraftPostProcessor(validator);
    }

    @Test
    void sanitize_keepsValidDraft() {
        LocalDate yesterday = LocalDate.of(2026, 11, 24);
        ExpenseDraft raw =
                new ExpenseDraft(new BigDecimal("500"), ExpenseCategory.FOOD, "  Dinner  ", yesterday);

        ExpenseDraft result = processor.sanitize(raw);

        assertThat(result.amount()).isEqualByComparingTo("500.00");
        assertThat(result.category()).isEqualTo(ExpenseCategory.FOOD);
        assertThat(result.description()).isEqualTo("Dinner");
        assertThat(result.expenseDate()).isEqualTo(yesterday);
    }

    @Test
    void sanitize_negativeAmountBecomesNull() {
        ExpenseDraft raw = new ExpenseDraft(new BigDecimal("-40"), ExpenseCategory.TRANSPORTATION, "Bus", null);

        assertThat(processor.sanitize(raw).amount()).isNull();
    }

    @Test
    void sanitize_futureDateBeyondOneDayBecomesNull() {
        LocalDate tooFar = LocalDate.of(2026, 11, 27);
        ExpenseDraft raw = new ExpenseDraft(new BigDecimal("10"), ExpenseCategory.FOOD, null, tooFar);

        assertThat(processor.sanitize(raw).expenseDate()).isNull();
    }

    @Test
    void sanitize_nullRawReturnsEmptyDraft() {
        ExpenseDraft result = processor.sanitize(null);

        assertThat(result.amount()).isNull();
        assertThat(result.category()).isNull();
        assertThat(result.description()).isNull();
        assertThat(result.expenseDate()).isNull();
    }
}