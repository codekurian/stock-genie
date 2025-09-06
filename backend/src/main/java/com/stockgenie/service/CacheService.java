package com.stockgenie.service;

import com.stockgenie.config.AppConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final AppConfig appConfig;
    
    /**
     * Cache stock data with default TTL
     */
    public void cacheStockData(String key, Object data) {
        cacheData(key, data, appConfig.getCache().getStockDataTtl());
    }
    
    /**
     * Cache technical analysis with default TTL
     */
    public void cacheTechnicalAnalysis(String key, Object data) {
        cacheData(key, data, appConfig.getCache().getTechnicalAnalysisTtl());
    }
    
    /**
     * Cache LLM analysis with default TTL
     */
    public void cacheLLMAnalysis(String key, Object data) {
        cacheData(key, data, appConfig.getCache().getLlmAnalysisTtl());
    }
    
    /**
     * Cache data with custom TTL
     */
    public void cacheData(String key, Object data, int ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(key, data, Duration.ofSeconds(ttlSeconds));
            log.debug("Cached data with key: {} for {} seconds", key, ttlSeconds);
        } catch (Exception e) {
            log.error("Error caching data with key {}: {}", key, e.getMessage());
        }
    }
    
    /**
     * Get cached data
     */
    public Object getCachedData(String key) {
        try {
            Object data = redisTemplate.opsForValue().get(key);
            if (data != null) {
                log.debug("Cache hit for key: {}", key);
            } else {
                log.debug("Cache miss for key: {}", key);
            }
            return data;
        } catch (Exception e) {
            log.error("Error getting cached data with key {}: {}", key, e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if data exists in cache
     */
    public boolean hasCachedData(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return exists != null && exists;
        } catch (Exception e) {
            log.error("Error checking cache for key {}: {}", key, e.getMessage());
            return false;
        }
    }
    
    /**
     * Delete cached data
     */
    public void deleteCachedData(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Deleted cached data with key: {}", key);
        } catch (Exception e) {
            log.error("Error deleting cached data with key {}: {}", key, e.getMessage());
        }
    }
    
    /**
     * Delete cached data by pattern
     */
    public void deleteCachedDataByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Deleted {} cached entries matching pattern: {}", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.error("Error deleting cached data by pattern {}: {}", pattern, e.getMessage());
        }
    }
    
    /**
     * Get TTL for a key
     */
    public long getTTL(String key) {
        try {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return ttl != null ? ttl : -1;
        } catch (Exception e) {
            log.error("Error getting TTL for key {}: {}", key, e.getMessage());
            return -1;
        }
    }
    
    /**
     * Extend TTL for a key
     */
    public void extendTTL(String key, int additionalSeconds) {
        try {
            long currentTTL = getTTL(key);
            if (currentTTL > 0) {
                redisTemplate.expire(key, Duration.ofSeconds(currentTTL + additionalSeconds));
                log.debug("Extended TTL for key: {} by {} seconds", key, additionalSeconds);
            }
        } catch (Exception e) {
            log.error("Error extending TTL for key {}: {}", key, e.getMessage());
        }
    }
    
    /**
     * Clear all cache
     */
    public void clearAllCache() {
        try {
            Set<String> keys = redisTemplate.keys("*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cleared {} cache entries", keys.size());
            }
        } catch (Exception e) {
            log.error("Error clearing all cache: {}", e.getMessage());
        }
    }
    
    /**
     * Get cache statistics
     */
    public CacheStats getCacheStats() {
        try {
            Set<String> allKeys = redisTemplate.keys("*");
            int totalKeys = allKeys != null ? allKeys.size() : 0;
            
            int stockDataKeys = 0;
            int technicalAnalysisKeys = 0;
            int llmAnalysisKeys = 0;
            
            if (allKeys != null) {
                for (String key : allKeys) {
                    if (key.startsWith("stockData")) {
                        stockDataKeys++;
                    } else if (key.startsWith("technicalAnalysis")) {
                        technicalAnalysisKeys++;
                    } else if (key.startsWith("llmAnalysis")) {
                        llmAnalysisKeys++;
                    }
                }
            }
            
            return CacheStats.builder()
                    .totalKeys(totalKeys)
                    .stockDataKeys(stockDataKeys)
                    .technicalAnalysisKeys(technicalAnalysisKeys)
                    .llmAnalysisKeys(llmAnalysisKeys)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error getting cache stats: {}", e.getMessage());
            return CacheStats.builder()
                    .totalKeys(0)
                    .stockDataKeys(0)
                    .technicalAnalysisKeys(0)
                    .llmAnalysisKeys(0)
                    .build();
        }
    }
    
    /**
     * Cache statistics DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CacheStats {
        private int totalKeys;
        private int stockDataKeys;
        private int technicalAnalysisKeys;
        private int llmAnalysisKeys;
    }
}
