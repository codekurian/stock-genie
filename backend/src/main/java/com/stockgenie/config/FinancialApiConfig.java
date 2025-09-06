package com.stockgenie.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "financial.api")
public class FinancialApiConfig {
    
    private String provider;
    private AlphaVantage alphaVantage = new AlphaVantage();
    private Eodhd eodhd = new Eodhd();
    
    @Data
    public static class AlphaVantage {
        private String baseUrl;
        private String apiKey;
        private RateLimit rateLimit = new RateLimit();
    }
    
    @Data
    public static class Eodhd {
        private String baseUrl;
        private String apiKey;
        private RateLimit rateLimit = new RateLimit();
    }
    
    @Data
    public static class RateLimit {
        private int callsPerMinute;
        private int callsPerDay;
    }
}
