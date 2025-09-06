package com.stockgenie.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    
    private Cache cache = new Cache();
    private Analysis analysis = new Analysis();
    
    @Data
    public static class Cache {
        private int stockDataTtl;
        private int technicalAnalysisTtl;
        private int llmAnalysisTtl;
    }
    
    @Data
    public static class Analysis {
        private int defaultPeriod;
        private int maxPeriod;
        private int batchSize;
    }
}
