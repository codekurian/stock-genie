package com.stockgenie.controller;

import com.stockgenie.service.RateLimitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rate-limit")
public class RateLimitController {

    @Autowired
    private RateLimitService rateLimitService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getRateLimitStatus() {
        try {
            Map<String, Object> status = Map.of(
                "alpha-vantage", rateLimitService.getRateLimitStatus("alpha-vantage"),
                "eodhd", rateLimitService.getRateLimitStatus("eodhd"),
                "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            Map<String, Object> status = Map.of(
                "error", "Rate limit service unavailable",
                "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.ok(status);
        }
    }

    @GetMapping("/status/{provider}")
    public ResponseEntity<Map<String, Object>> getRateLimitStatusForProvider(@PathVariable String provider) {
        try {
            Map<String, Object> status = Map.of(
                "provider", provider,
                "status", rateLimitService.getRateLimitStatus(provider),
                "canMakeCall", rateLimitService.canMakeApiCall(provider),
                "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            Map<String, Object> status = Map.of(
                "provider", provider,
                "error", "Rate limit service unavailable",
                "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.ok(status);
        }
    }
}
