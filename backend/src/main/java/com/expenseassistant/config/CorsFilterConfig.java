package com.expenseassistant.config;

import com.expenseassistant.assistant.service.PendingActionProperties;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableConfigurationProperties({CorsProperties.class, AppProperties.class, PendingActionProperties.class})
public class CorsFilterConfig {

    private final CorsProperties corsProperties;

    public CorsFilterConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Bean
    public CorsFilter corsFilter() {
        var allowedOrigins = corsProperties.getAllowedOrigins();
        var source = new UrlBasedCorsConfigurationSource();
        if (!allowedOrigins.isEmpty()) {
            var config = new CorsConfiguration();
            config.setAllowedOrigins(allowedOrigins);
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
            config.setAllowedHeaders(List.of("Content-Type", "Authorization", "X-Dev-User-Id"));
            config.setMaxAge(3600L);
            source.registerCorsConfiguration("/**", config);
        }
        return new CorsFilter(source);
    }
}