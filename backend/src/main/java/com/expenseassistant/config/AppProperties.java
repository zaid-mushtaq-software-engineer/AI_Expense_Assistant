package com.expenseassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /**
     * Default user id when {@code X-Dev-User-Id} is not sent (dev profile only).
     */
    private long devUserId = 1L;

    private String timezone = "Asia/Kolkata";

    public long getDevUserId() {
        return devUserId;
    }

    public void setDevUserId(long devUserId) {
        this.devUserId = devUserId;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}