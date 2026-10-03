package com.wen.ojbackendgateway.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.Arrays;
import java.util.List;

//处理跨域
@Configuration
@Data
@AllArgsConstructor
@ConfigurationProperties(prefix = "cors")
public class CrosConfig {

    //存放前端的路径 如: http://localhost:9191
    private List<String> allowedOrigins;

    @Bean
    public CorsWebFilter corsFilter() {

        CorsConfiguration config = new CorsConfiguration();

        config.addAllowedMethod("*");

        config.setAllowCredentials(true);

        config.setAllowedOriginPatterns(allowedOrigins);

        config.addAllowedHeader("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(new PathPatternParser());

        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);

    }
}
