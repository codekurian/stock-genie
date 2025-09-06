package com.stockgenie.service;

import com.stockgenie.config.FinancialApiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiOptimizationService {
    
    private final WebClient.Builder webClientBuilder;
    private final FinancialApiConfig financialApiConfig;
    private final RateLimitService rateLimitService;
    
    @Value("${financial.api.alpha-vantage.timeout:30000}")
    private int apiTimeout;
    
    @Value("${financial.api.alpha-vantage.retry-attempts:3}")
    private int retryAttempts;
    
    @Value("${financial.api.alpha-vantage.retry-delay:1000}")
    private int retryDelay;
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final Map<String, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();
    
    /**
     * Make API call with retry mechanism
     */
    public String makeApiCallWithRetry(String url, String requestKey) {
        // Check if request is already pending
        if (pendingRequests.containsKey(requestKey)) {
            log.debug("Request already pending for key: {}", requestKey);
            try {
                return pendingRequests.get(requestKey).get();
            } catch (Exception e) {
                log.warn("Error waiting for pending request: {}", e.getMessage());
            }
        }
        
        // Create new request
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            return executeWithRetry(url);
        }, executorService);
        
        pendingRequests.put(requestKey, future);
        
        try {
            String result = future.get();
            return result;
        } catch (Exception e) {
            log.error("Error executing API call: {}", e.getMessage());
            throw new RuntimeException("API call failed", e);
        } finally {
            pendingRequests.remove(requestKey);
        }
    }
    
    /**
     * Execute API call with retry logic
     */
    private String executeWithRetry(String url) {
        int attempts = 0;
        Exception lastException = null;
        
        while (attempts < retryAttempts) {
            try {
                // Check rate limits before making call
                if (!rateLimitService.canMakeApiCall("alpha-vantage")) {
                    log.warn("Rate limit exceeded, waiting before retry");
                    Thread.sleep(retryDelay * 2);
                    continue;
                }
                
                WebClient webClient = webClientBuilder.build();
                String response = webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofMillis(apiTimeout))
                        .block();
                
                // Record successful API call
                rateLimitService.recordApiCall("alpha-vantage");
                
                log.debug("API call successful on attempt {}", attempts + 1);
                return response;
                
            } catch (WebClientResponseException e) {
                lastException = e;
                attempts++;
                log.warn("API call failed on attempt {}: {} - {}", attempts, e.getStatusCode(), e.getResponseBodyAsString());
                
                if (e.getStatusCode().is4xxClientError()) {
                    // Don't retry on client errors
                    break;
                }
                
                if (attempts < retryAttempts) {
                    try {
                        Thread.sleep(retryDelay * attempts); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                
            } catch (Exception e) {
                lastException = e;
                attempts++;
                log.warn("API call failed on attempt {}: {}", attempts, e.getMessage());
                
                if (attempts < retryAttempts) {
                    try {
                        Thread.sleep(retryDelay * attempts);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        log.error("API call failed after {} attempts", retryAttempts);
        throw new RuntimeException("API call failed after " + retryAttempts + " attempts", lastException);
    }
    
    /**
     * Batch multiple API calls
     */
    public List<String> batchApiCalls(List<String> urls, List<String> requestKeys) {
        List<CompletableFuture<String>> futures = new ArrayList<>();
        
        for (int i = 0; i < urls.size(); i++) {
            String url = urls.get(i);
            String requestKey = requestKeys.get(i);
            
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                return makeApiCallWithRetry(url, requestKey);
            }, executorService);
            
            futures.add(future);
        }
        
        List<String> results = new ArrayList<>();
        for (CompletableFuture<String> future : futures) {
            try {
                results.add(future.get());
            } catch (Exception e) {
                log.error("Error in batch API call: {}", e.getMessage());
                results.add(null); // Add null for failed requests
            }
        }
        
        return results;
    }
    
    /**
     * Get API optimization statistics
     */
    public Map<String, Object> getOptimizationStats() {
        Map<String, Object> stats = Map.of(
            "pendingRequests", pendingRequests.size(),
            "retryAttempts", retryAttempts,
            "retryDelay", retryDelay,
            "apiTimeout", apiTimeout,
            "rateLimitStatus", rateLimitService.getRateLimitStatus("alpha-vantage"),
            "threadPoolSize", 10,
            "activeThreads", Thread.activeCount()
        );
        
        return stats;
    }
    
    /**
     * Clear pending requests (for cleanup)
     */
    public void clearPendingRequests() {
        log.info("Clearing {} pending requests", pendingRequests.size());
        pendingRequests.clear();
    }
    
    /**
     * Shutdown executor service
     */
    public void shutdown() {
        log.info("Shutting down API optimization service");
        executorService.shutdown();
    }
}
