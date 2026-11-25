package com.expenseassistant.assistant.prompt;

import com.expenseassistant.config.AppProperties;
import com.expenseassistant.expense.entity.ExpenseCategory;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "google-genai")
public class SystemPromptProvider {

    private final PromptTemplate template;
    private final Clock clock;
    private final AppProperties appProperties;
    private final String categoriesList;

    public SystemPromptProvider(Clock clock, AppProperties appProperties) {
        this.clock = clock;
        this.appProperties = appProperties;
        this.template = new PromptTemplate(new ClassPathResource("prompts/system.st"));
        this.categoriesList =
                Arrays.stream(ExpenseCategory.values())
                        .map(Enum::name)
                        .collect(Collectors.joining(", "));
    }

    public String render() {
        LocalDate today = LocalDate.now(clock);
        String weekday = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        return template.render(
                Map.of(
                        "today",
                        today.toString(),
                        "weekday",
                        weekday,
                        "timezone",
                        appProperties.getTimezone(),
                        "categories",
                        categoriesList));
    }
}