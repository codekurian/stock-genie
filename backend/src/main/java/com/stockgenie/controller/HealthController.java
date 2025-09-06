package com.stockgenie.controller;

import com.stockgenie.service.CacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @Autowired
    private CacheService cacheService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now(),
            "version", "1.0.0",
            "message", "Stock Genie API is running!"
        );
        return ResponseEntity.ok(health);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        try {
            Map<String, Object> status = Map.of(
                "application", "Stock Genie Backend",
                "status", "UP",
                "timestamp", LocalDateTime.now(),
                "cache", "Cache service - implementation in progress"
            );
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            Map<String, Object> status = Map.of(
                "application", "Stock Genie Backend",
                "status", "UP",
                "timestamp", LocalDateTime.now(),
                "cache", "Cache service unavailable"
            );
            return ResponseEntity.ok(status);
        }
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        Map<String, Object> info = Map.of(
            "name", "Stock Genie Backend API",
            "version", "1.0.0",
            "description", "Stock analysis and AI-powered insights API",
            "endpoints", Map.of(
                "stocks", "/api/v1/stocks",
                "analysis", "/api/v1/analysis", 
                "llm", "/api/v1/llm",
                "health", "/api/v1/health"
            )
        );
        return ResponseEntity.ok(info);
    }
}
