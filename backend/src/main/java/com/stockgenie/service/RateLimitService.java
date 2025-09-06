package com.stockgenie.service;

import com.stockgenie.config.FinancialApiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {
    
    private final FinancialApiConfig financialApiConfig;
    
    // Track API calls per minute and per day
    private final ConcurrentHashMap<String, AtomicInteger> callsPerMinute = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> callsPerDay = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> lastMinuteReset = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> lastDayReset = new ConcurrentHashMap<>();
    
    /**
     * Check if we can make an API call based on rate limits
     */
    public boolean canMakeApiCall(String apiProvider) {
        String minuteKey = apiProvider + "_minute";
        String dayKey = apiProvider + "_day";
        
        LocalDateTime now = LocalDateTime.now();
        
        // Check minute rate limit
        if (!canMakeCallInTimeWindow(minuteKey, now, ChronoUnit.MINUTES, 
                getCallsPerMinute(apiProvider))) {
            log.warn("Rate limit exceeded for {} - calls per minute", apiProvider);
            return false;
        }
        
        // Check day rate limit
        if (!canMakeCallInTimeWindow(dayKey, now, ChronoUnit.DAYS, 
                getCallsPerDay(apiProvider))) {
            log.warn("Rate limit exceeded for {} - calls per day", apiProvider);
            return false;
        }
        
        return true;
    }
    
    /**
     * Record an API call
     */
    public void recordApiCall(String apiProvider) {
        String minuteKey = apiProvider + "_minute";
        String dayKey = apiProvider + "_day";
        
        callsPerMinute.computeIfAbsent(minuteKey, k -> new AtomicInteger(0)).incrementAndGet();
        callsPerDay.computeIfAbsent(dayKey, k -> new AtomicInteger(0)).incrementAndGet();
        
        log.debug("Recorded API call for {}. Minute: {}, Day: {}", 
                apiProvider, 
                callsPerMinute.get(minuteKey).get(),
                callsPerDay.get(dayKey).get());
    }
    
    /**
     * Check if we can make a call within a time window
     */
    private boolean canMakeCallInTimeWindow(String key, LocalDateTime now, 
                                          ChronoUnit timeUnit, int maxCalls) {
        LocalDateTime lastReset = getLastReset(key, now, timeUnit);
        AtomicInteger callCount = callsPerMinute.computeIfAbsent(key, k -> new AtomicInteger(0));
        
        // Reset counter if time window has passed
        if (lastReset.isBefore(now.minus(1, timeUnit))) {
            callCount.set(0);
            lastMinuteReset.put(key, now);
        }
        
        return callCount.get() < maxCalls;
    }
    
    /**
     * Get the last reset time for a key
     */
    private LocalDateTime getLastReset(String key, LocalDateTime now, ChronoUnit timeUnit) {
        return lastMinuteReset.computeIfAbsent(key, k -> now.minus(1, timeUnit));
    }
    
    /**
     * Get calls per minute limit for API provider
     */
    private int getCallsPerMinute(String apiProvider) {
        if ("alpha-vantage".equals(apiProvider)) {
            return financialApiConfig.getAlphaVantage().getRateLimit().getCallsPerMinute();
        } else if ("eodhd".equals(apiProvider)) {
            return 60; // Default for EODHD
        }
        return 5; // Default conservative limit
    }
    
    /**
     * Get calls per day limit for API provider
     */
    private int getCallsPerDay(String apiProvider) {
        if ("alpha-vantage".equals(apiProvider)) {
            return financialApiConfig.getAlphaVantage().getRateLimit().getCallsPerDay();
        } else if ("eodhd".equals(apiProvider)) {
            return financialApiConfig.getEodhd().getRateLimit().getCallsPerDay();
        }
        return 25; // Default conservative limit
    }
    
    /**
     * Get current rate limit status
     */
    public String getRateLimitStatus(String apiProvider) {
        String minuteKey = apiProvider + "_minute";
        String dayKey = apiProvider + "_day";
        
        int minuteCalls = callsPerMinute.getOrDefault(minuteKey, new AtomicInteger(0)).get();
        int dayCalls = callsPerDay.getOrDefault(dayKey, new AtomicInteger(0)).get();
        int minuteLimit = getCallsPerMinute(apiProvider);
        int dayLimit = getCallsPerDay(apiProvider);
        
        return String.format("API: %s, Minute: %d/%d, Day: %d/%d", 
                apiProvider, minuteCalls, minuteLimit, dayCalls, dayLimit);
    }
}
