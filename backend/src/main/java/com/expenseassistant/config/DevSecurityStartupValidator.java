package com.expenseassistant.config;

import com.expenseassistant.security.DevCurrentUserProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Prevents accidental deployment with dev identity enabled.
 */
@Component
@Profile("prod")
public class DevSecurityStartupValidator {

    public DevSecurityStartupValidator(ApplicationContext applicationContext) {
        if (!applicationContext.getBeansOfType(DevCurrentUserProvider.class).isEmpty()) {
            throw new IllegalStateException(
                    "DevCurrentUserProvider is active while the prod profile is enabled. "
                            + "Remove the dev profile or delete DevCurrentUserProvider (Stage 10D).");
        }
    }
}