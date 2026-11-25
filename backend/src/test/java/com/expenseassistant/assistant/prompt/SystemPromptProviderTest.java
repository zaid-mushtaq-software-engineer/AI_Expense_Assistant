package com.expenseassistant.assistant.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.expenseassistant.config.AppProperties;
import com.expenseassistant.expense.entity.ExpenseCategory;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class SystemPromptProviderTest {

    @Test
    void render_includesDateWeekdayTimezoneAndAllCategories() {
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        LocalDate fixedDate = LocalDate.of(2026, 11, 25);
        Clock clock = Clock.fixed(fixedDate.atStartOfDay(zone).toInstant(), zone);

        AppProperties appProperties = new AppProperties();
        appProperties.setTimezone("Asia/Kolkata");

        SystemPromptProvider provider = new SystemPromptProvider(clock, appProperties);
        String rendered = provider.render();

        assertThat(rendered).contains("2026-11-25").contains("Wednesday").contains("Asia/Kolkata");
        for (ExpenseCategory category : ExpenseCategory.values()) {
            assertThat(rendered).contains(category.name());
        }
        assertThat(rendered).contains("₹");
        assertThat(rendered).contains("getTotalSpending");
        assertThat(rendered).contains("getCategorySpending");
        assertThat(rendered).contains("getMonthlySummary");
        assertThat(rendered).contains("searchExpenses");
        assertThat(rendered).contains("addExpense");
        assertThat(rendered).contains("proposeExpenseDeletion");
        assertThat(rendered).contains("proposeExpenseUpdate");
        assertThat(rendered).contains("Missing amount");
        assertThat(rendered).doesNotContain("tools are not connected");
        assertThat(rendered).doesNotContain("Listing, adding");
    }
}