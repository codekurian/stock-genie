package com.stockgenie.controller;

import com.stockgenie.service.DataCleanupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/database")
public class DatabaseController {

    @Autowired
    private DataCleanupService dataCleanupService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDatabaseStats() {
        try {
            Map<String, Object> stats = dataCleanupService.getDatabaseStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to get database statistics",
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/retention-policy")
    public ResponseEntity<Map<String, Object>> getRetentionPolicy() {
        try {
            Map<String, Object> policy = dataCleanupService.getRetentionPolicy();
            return ResponseEntity.ok(policy);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to get retention policy",
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/cleanup-history")
    public ResponseEntity<Map<String, Object>> getCleanupHistory() {
        try {
            Map<String, Object> history = dataCleanupService.getCleanupHistory();
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to get cleanup history",
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> performCleanup(@RequestParam(defaultValue = "365") int retentionDays) {
        try {
            Map<String, Long> cleanupStats = dataCleanupService.cleanupOldData(retentionDays);
            return ResponseEntity.ok(Map.of(
                "message", "Data cleanup completed successfully",
                "retentionDays", retentionDays,
                "cleanupStats", cleanupStats
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to perform data cleanup",
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/cleanup/force")
    public ResponseEntity<Map<String, Object>> forceCleanup() {
        try {
            Map<String, Long> cleanupStats = dataCleanupService.forceCleanup();
            return ResponseEntity.ok(Map.of(
                "message", "Forced data cleanup completed successfully",
                "cleanupStats", cleanupStats
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to perform forced cleanup",
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/optimize")
    public ResponseEntity<Map<String, Object>> optimizeDatabase() {
        try {
            String result = dataCleanupService.optimizeDatabase();
            return ResponseEntity.ok(Map.of(
                "message", "Database optimization completed",
                "result", result
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to optimize database",
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getDatabaseHealth() {
        try {
            Map<String, Object> stats = dataCleanupService.getDatabaseStats();
            Map<String, Object> policy = dataCleanupService.getRetentionPolicy();
            
            // Calculate health metrics
            long totalRecords = 0;
            for (Map.Entry<String, Object> entry : stats.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> tableStats = (Map<String, Object>) entry.getValue();
                    totalRecords += (Long) tableStats.get("recordCount");
                }
            }
            
            Map<String, Object> health = Map.of(
                "status", "HEALTHY",
                "totalRecords", totalRecords,
                "retentionPolicy", policy,
                "databaseStats", stats,
                "message", "Database is healthy and operational"
            );
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "UNHEALTHY",
                "error", "Database health check failed",
                "message", e.getMessage()
            ));
        }
    }
}
