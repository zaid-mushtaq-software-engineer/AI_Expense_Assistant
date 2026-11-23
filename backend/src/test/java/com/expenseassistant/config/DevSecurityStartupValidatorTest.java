package com.expenseassistant.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.expenseassistant.ExpenseAssistantApplication;
import com.expenseassistant.security.DevCurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.NestedExceptionUtils;

class DevSecurityStartupValidatorTest {

    @Test
    void devProviderBeanAbsentWithoutDevProfile() {
        new ApplicationContextRunner()
                .withPropertyValues("spring.profiles.active=prod")
                .withUserConfiguration(DevSecurityStartupValidator.class)
                .run(context -> assertThat(context).doesNotHaveBean(DevCurrentUserProvider.class));
    }

    @Test
    void applicationFailsWhenDevAndProdProfilesActiveTogether() {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        if (dbUrl == null || dbUser == null || dbPassword == null) {
            return;
        }

        Throwable failure = org.assertj.core.api.Assertions.catchThrowable(() -> new SpringApplicationBuilder(
                        ExpenseAssistantApplication.class)
                .profiles("dev", "prod")
                .properties(
                        "spring.datasource.url=" + dbUrl,
                        "spring.datasource.username=" + dbUser,
                        "spring.datasource.password=" + dbPassword)
                .run());
        assertThat(NestedExceptionUtils.getMostSpecificCause(failure))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DevCurrentUserProvider");
    }
}