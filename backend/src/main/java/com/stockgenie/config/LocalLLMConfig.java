package com.stockgenie.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "local-llm")
public class LocalLLMConfig {
    
    private String provider;
    private String baseUrl;
    private String model;
    private int timeout;
    private int maxTokens;
    private double temperature;
    private double topP;
}
