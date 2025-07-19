package com.mfon.rmhi.scraping.service;

import com.mfon.rmhi.scraping.dto.DailyExecutionSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Scheduled service for automated monitoring tasks
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "scraping.monitoring.enable-metrics", havingValue = "true", matchIfMissing = true)
public class MonitoringScheduledService {
    
    private final MonitoringService monitoringService;
    
    @Value("${scraping.monitoring.retention-days:30}")
    private int retentionDays;
    
    @Value("${scraping.monitoring.enable-daily-reports:true}")
    private boolean enableDailyReports;
    
    /**
     * Generate daily execution summary report every day at 1 AM
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void generateDailyReport() {
        if (!enableDailyReports) {
            log.debug("Daily reports are disabled, skipping report generation");
            return;
        }
        
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            
            log.info("Generating automated daily report for {}", yesterday);
            
            DailyExecutionSummary report = monitoringService.generateDailyReport(yesterday);
            
            // Log summary of the report
            logReportSummary(report);
            
            // In a production environment, you might want to:
            // - Send the report via email
            // - Store it in a database
            // - Send alerts if there are critical issues
            // - Push metrics to external monitoring systems
            
        } catch (Exception e) {
            log.error("Failed to generate automated daily report", e);
        }
    }
    
    /**
     * Clean up old monitoring data every Sunday at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * SUN")
    public void cleanupOldData() {
        try {
            log.info("Starting automated cleanup of monitoring data older than {} days", retentionDays);
            
            monitoringService.cleanupOldData(retentionDays);
            
            log.info("Automated monitoring data cleanup completed successfully");
            
        } catch (Exception e) {
            log.error("Failed to cleanup old monitoring data", e);
        }
    }
    
    /**
     * Perform health checks every 5 minutes and log warnings/errors
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void performHealthCheck() {
        try {
            var healthStatus = monitoringService.getSystemHealth();
            
            // Log warnings and errors
            if (!healthStatus.getRecentErrors().isEmpty()) {
                log.warn("Recent errors detected: {}", healthStatus.getRecentErrors().size());
                healthStatus.getRecentErrors().forEach(error -> log.warn("Error: {}", error));
            }
            
            if (!healthStatus.getRecentWarnings().isEmpty()) {
                log.info("Recent warnings detected: {}", healthStatus.getRecentWarnings().size());
                healthStatus.getRecentWarnings().forEach(warning -> log.info("Warning: {}", warning));
            }
            
            // Log critical health issues
            if (healthStatus.getOverallStatus() == com.mfon.rmhi.scraping.dto.SystemHealthStatus.HealthStatus.CRITICAL) {
                log.error("CRITICAL: System health is in critical state!");
                
                healthStatus.getComponentHealth().forEach((component, health) -> {
                    if (health.getStatus() == com.mfon.rmhi.scraping.dto.SystemHealthStatus.HealthStatus.CRITICAL) {
                        log.error("CRITICAL: Component {} is in critical state: {}", component, health.getStatusMessage());
                    }
                });
            }
            
            // Log high resource usage
            if (healthStatus.getSystemMetrics() != null) {
                var metrics = healthStatus.getSystemMetrics();
                if (metrics.getMemoryUsagePercent() > 85) {
                    log.warn("HIGH MEMORY USAGE: {:.1f}% memory usage detected", metrics.getMemoryUsagePercent());
                }
                if (metrics.getCpuUsagePercent() > 85) {
                    log.warn("HIGH CPU USAGE: {:.1f}% CPU usage detected", metrics.getCpuUsagePercent());
                }
            }
            
        } catch (Exception e) {
            log.error("Health check failed", e);
        }
    }
    
    /**
     * Log a summary of the daily report
     */
    private void logReportSummary(DailyExecutionSummary report) {
        log.info("=== DAILY EXECUTION SUMMARY for {} ===", report.getReportDate());
        log.info("Total Operations: {} (Success Rate: {:.1f}%)", 
                report.getTotalOperations(), report.getSuccessRate());
        log.info("Ideas Scraped: {}, Transformed: {}, Staged: {}, Migrated: {}", 
                report.getTotalIdeasScraped(), 
                report.getTotalIdeasTransformed(),
                report.getTotalIdeasStaged(),
                report.getTotalIdeasMigrated());
        
        if (report.getTotalErrors() > 0) {
            log.warn("Total Errors: {}", report.getTotalErrors());
            if (report.getCriticalErrors() != null && !report.getCriticalErrors().isEmpty()) {
                log.error("Critical Errors:");
                report.getCriticalErrors().forEach(error -> log.error("  - {}", error));
            }
        }
        
        if (report.getHealthWarnings() != null && !report.getHealthWarnings().isEmpty()) {
            log.warn("Health Warnings:");
            report.getHealthWarnings().forEach(warning -> log.warn("  - {}", warning));
        }
        
        if (report.getPerformanceAlerts() != null && !report.getPerformanceAlerts().isEmpty()) {
            log.warn("Performance Alerts:");
            report.getPerformanceAlerts().forEach(alert -> log.warn("  - {}", alert));
        }
        
        // Log source-specific statistics
        if (report.getSourceStatistics() != null && !report.getSourceStatistics().isEmpty()) {
            log.info("Source Statistics:");
            report.getSourceStatistics().forEach((sourceName, stats) -> {
                log.info("  - {}: {} executions, {} ideas, {:.1f}% success rate", 
                        sourceName, stats.getExecutionCount(), stats.getIdeasScraped(), stats.getSuccessRate());
            });
        }
        
        log.info("=== END DAILY SUMMARY ===");
    }
}