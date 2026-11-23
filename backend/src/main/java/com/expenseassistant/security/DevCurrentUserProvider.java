package com.expenseassistant.security;

import com.expenseassistant.config.AppProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Dev-only identity stand-in until JWT auth (Stage 10).
 * <p><strong>REMOVE IN STAGE 10D</strong></p>
 */
@Component
@Profile("dev")
public class DevCurrentUserProvider implements CurrentUserProvider {

    public static final String DEV_USER_HEADER = "X-Dev-User-Id";

    private final AppProperties appProperties;

    public DevCurrentUserProvider(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public Long requireUserId() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            String header = attributes.getRequest().getHeader(DEV_USER_HEADER);
            if (header != null && !header.isBlank()) {
                return Long.parseLong(header.trim());
            }
        }
        return appProperties.getDevUserId();
    }
}