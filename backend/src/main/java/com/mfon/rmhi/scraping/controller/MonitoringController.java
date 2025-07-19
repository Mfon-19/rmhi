package com.mfon.rmhi.scraping.controller;

import com.mfon.rmhi.scraping.dto.DailyExecutionSummary;
import com.mfon.rmhi.scraping.dto.PerformanceMetrics;
import com.mfon.rmhi.scraping.dto.SystemHealthStatus;
import com.mfon.rmhi.scraping.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST controller for monitoring and health check endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MonitoringController {
    
    private final MonitoringService monitoringService;
    
    /**
     * Health check endpoint for system status
     */
    @GetMapping("/health")
    public ResponseEntity<SystemHealthStatus> getSystemHealth() {
        log.debug("Health check requested");
        
        try {
            SystemHealthStatus health = monitoringService.getSystemHealth();
            
            // Return appropriate HTTP status based on health
            switch (health.getOverallStatus()) {
                case HEALTHY:
                    return ResponseEntity.ok(health);
                case WARNING:
                    return ResponseEntity.status(200).body(health); // Still OK but with warnings
                case CRITICAL:
                    return ResponseEntity.status(503).body(health); // Service Unavailable
                case DOWN:
                    return ResponseEntity.status(503).body(health); // Service Unavailable
                default:
                    return ResponseEntity.status(500).body(health); // Internal Server Error
            }
            
        } catch (Exception e) {
            log.error("Failed to get system health", e);
            
            SystemHealthStatus errorHealth = SystemHealthStatus.builder()
                    .overallStatus(SystemHealthStatus.HealthStatus.CRITICAL)
                    .recentErrors(List.of("Failed to retrieve system health: " + e.getMessage()))
                    .build();
            
            return ResponseEntity.status(503).body(errorHealth);
        }
    }
    
    /**
     * Simple health check endpoint for load balancers
     */
    @GetMapping("/health/simple")
    public ResponseEntity<Map<String, String>> getSimpleHealth() {
        try {
            SystemHealthStatus health = monitoringService.getSystemHealth();
            
            Map<String, String> response = Map.of(
                    "status", health.getOverallStatus().name(),
                    "timestamp", health.getTimestamp().toString()
            );
            
            return health.getOverallStatus() == SystemHealthStatus.HealthStatus.HEALTHY ?
                    ResponseEntity.ok(response) :
                    ResponseEntity.status(503).body(response);
                    
        } catch (Exception e) {
            log.error("Simple health check failed", e);
            return ResponseEntity.status(503).body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Get daily execution summary report
     */
    @GetMapping("/reports/daily")
    public ResponseEntity<DailyExecutionSummary> getDailyReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDate reportDate = date != null ? date : LocalDate.now().minusDays(1);
        
        log.info("Daily report requested for date: {}", reportDate);
        
        try {
            DailyExecutionSummary report = monitoringService.generateDailyReport(reportDate);
            return ResponseEntity.ok(report);
            
        } catch (Exception e) {
            log.error("Failed to generate daily report for date: {}", reportDate, e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * Get performance metrics for a specific operation type
     */
    @GetMapping("/metrics/{operationType}")
    public ResponseEntity<List<PerformanceMetrics>> getPerformanceMetrics(
            @PathVariable String operationType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(7);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        
        log.debug("Performance metrics requested for operation: {} from {} to {}", operationType, start, end);
        
        try {
            List<PerformanceMetrics> metrics = monitoringService.getPerformanceMetrics(operationType, start, end);
            return ResponseEntity.ok(metrics);
            
        } catch (Exception e) {
            log.error("Failed to get performance metrics for operation: {}", operationType, e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * Get available operation types for metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, String>> getAvailableMetrics() {
        Map<String, String> availableMetrics = Map.of(
                "SCRAPING", "Web scraping operations",
                "AI_TRANSFORMATION", "AI transformation operations", 
                "STAGING", "Staging operations",
                "MIGRATION", "Migration operations",
                "ORCHESTRATION", "Overall orchestration operations"
        );
        
        return ResponseEntity.ok(availableMetrics);
    }
    
    /**
     * Trigger cleanup of old monitoring data
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, String>> cleanupOldData(
            @RequestParam(defaultValue = "30") int retentionDays) {
        
        log.info("Manual cleanup requested with retention: {} days", retentionDays);
        
        try {
            monitoringService.cleanupOldData(retentionDays);
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", String.format("Cleanup completed for data older than %d days", retentionDays)
            ));
            
        } catch (Exception e) {
            log.error("Failed to cleanup old monitoring data", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Cleanup failed: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get monitoring statistics summary
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getMonitoringStats() {
        try {
            SystemHealthStatus health = monitoringService.getSystemHealth();
            DailyExecutionSummary todayReport = monitoringService.generateDailyReport(LocalDate.now());
            
            Map<String, Object> stats = Map.of(
                    "system_health", health.getOverallStatus().name(),
                    "active_operations", health.getActiveOperations(),
                    "uptime_hours", health.getUptimeMs() / (1000 * 60 * 60),
                    "today_operations", todayReport.getTotalOperations(),
                    "today_success_rate", todayReport.getSuccessRate(),
                    "today_ideas_scraped", todayReport.getTotalIdeasScraped(),
                    "memory_usage_percent", health.getSystemMetrics() != null ? 
                            health.getSystemMetrics().getMemoryUsagePercent() : 0
            );
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Failed to get monitoring statistics", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to retrieve statistics: " + e.getMessage()
            ));
        }
    }
}