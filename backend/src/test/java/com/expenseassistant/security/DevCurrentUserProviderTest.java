package com.expenseassistant.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.expenseassistant.config.AppProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class DevCurrentUserProviderTest {

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void headerOverridesConfiguredDevUserId() {
        AppProperties properties = new AppProperties();
        properties.setDevUserId(1L);
        DevCurrentUserProvider provider = new DevCurrentUserProvider(properties);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(DevCurrentUserProvider.DEV_USER_HEADER, "2");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThat(provider.requireUserId()).isEqualTo(2L);
    }

    @Test
    void usesConfiguredDevUserIdWhenHeaderAbsent() {
        AppProperties properties = new AppProperties();
        properties.setDevUserId(1L);
        DevCurrentUserProvider provider = new DevCurrentUserProvider(properties);

        assertThat(provider.requireUserId()).isEqualTo(1L);
    }

    @Test
    void invalidHeaderValueFails() {
        AppProperties properties = new AppProperties();
        properties.setDevUserId(1L);
        DevCurrentUserProvider provider = new DevCurrentUserProvider(properties);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(DevCurrentUserProvider.DEV_USER_HEADER, "not-a-number");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThatThrownBy(provider::requireUserId).isInstanceOf(NumberFormatException.class);
    }
}