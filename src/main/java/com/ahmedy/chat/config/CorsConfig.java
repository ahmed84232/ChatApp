package com.ahmedy.chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Allow credentials and specific origins
        config.setAllowCredentials(false);
//        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedOriginPattern("*");

        // Allow all headers and methods for development
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        // Handle preflight requests
        config.addExposedHeader("Authorization");

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}