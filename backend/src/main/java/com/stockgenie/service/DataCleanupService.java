package com.stockgenie.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataCleanupService {
    
    private final DataSource dataSource;
    
    @Value("${app.data.retention-days:365}")
    private int retentionDays;
    
    @Value("${app.data.cleanup-enabled:true}")
    private boolean cleanupEnabled;
    
    /**
     * Scheduled cleanup task - runs daily at 2 AM
     */
    @Scheduled(cron = "${app.data.cleanup-schedule:0 0 2 * * ?}")
    @Transactional
    public void performScheduledCleanup() {
        if (!cleanupEnabled) {
            log.info("Data cleanup is disabled");
            return;
        }
        
        log.info("Starting scheduled data cleanup with retention period of {} days", retentionDays);
        
        try {
            Map<String, Long> cleanupStats = cleanupOldData(retentionDays);
            
            log.info("Data cleanup completed successfully: {}", cleanupStats);
            
            // Optimize database after cleanup
            optimizeDatabase();
            
        } catch (Exception e) {
            log.error("Error during scheduled data cleanup", e);
        }
    }
    
    /**
     * Manual cleanup with custom retention period
     */
    @Transactional
    public Map<String, Long> cleanupOldData(int retentionDays) {
        Map<String, Long> stats = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            // Call the cleanup function
            String sql = "SELECT * FROM cleanup_old_data(?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, retentionDays);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String tableName = rs.getString("table_name");
                        long deletedCount = rs.getLong("deleted_count");
                        stats.put(tableName, deletedCount);
                        log.info("Cleaned up {} records from {}", deletedCount, tableName);
                    }
                }
            }
            
        } catch (SQLException e) {
            log.error("Error during data cleanup", e);
            throw new RuntimeException("Failed to cleanup old data", e);
        }
        
        return stats;
    }
    
    /**
     * Get database statistics
     */
    public Map<String, Object> getDatabaseStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            String sql = "SELECT * FROM get_database_stats()";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String tableName = rs.getString("table_name");
                        Map<String, Object> tableStats = new HashMap<>();
                        tableStats.put("recordCount", rs.getLong("record_count"));
                        tableStats.put("oldestRecord", rs.getDate("oldest_record"));
                        tableStats.put("newestRecord", rs.getDate("newest_record"));
                        tableStats.put("tableSize", rs.getString("table_size"));
                        stats.put(tableName, tableStats);
                    }
                }
            }
            
        } catch (SQLException e) {
            log.error("Error getting database statistics", e);
            throw new RuntimeException("Failed to get database statistics", e);
        }
        
        return stats;
    }
    
    /**
     * Optimize database performance
     */
    @Transactional
    public String optimizeDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            String sql = "SELECT optimize_database()";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String result = rs.getString(1);
                        log.info("Database optimization: {}", result);
                        return result;
                    }
                }
            }
            
        } catch (SQLException e) {
            log.error("Error optimizing database", e);
            throw new RuntimeException("Failed to optimize database", e);
        }
        
        return "Database optimization completed";
    }
    
    /**
     * Get data retention policy information
     */
    public Map<String, Object> getRetentionPolicy() {
        Map<String, Object> policy = new HashMap<>();
        policy.put("retentionDays", retentionDays);
        policy.put("cleanupEnabled", cleanupEnabled);
        policy.put("cutoffDate", LocalDate.now().minusDays(retentionDays));
        policy.put("nextCleanup", "Daily at 2 AM");
        return policy;
    }
    
    /**
     * Force immediate cleanup
     */
    @Transactional
    public Map<String, Long> forceCleanup() {
        log.info("Forcing immediate data cleanup");
        return cleanupOldData(retentionDays);
    }
    
    /**
     * Get cleanup history (simplified version)
     */
    public Map<String, Object> getCleanupHistory() {
        Map<String, Object> history = new HashMap<>();
        history.put("lastCleanup", "Scheduled daily at 2 AM");
        history.put("retentionPolicy", getRetentionPolicy());
        history.put("databaseStats", getDatabaseStats());
        return history;
    }
}
