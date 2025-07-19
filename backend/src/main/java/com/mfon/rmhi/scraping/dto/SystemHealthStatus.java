package com.mfon.rmhi.scraping.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * System health status for monitoring endpoints
 */
@Data
@Builder
public class SystemHealthStatus {
    
    private LocalDateTime timestamp;
    private HealthStatus overallStatus;
    private String version;
    private long uptimeMs;
    
    // Component health
    private Map<String, ComponentHealth> componentHealth;
    
    // System metrics
    private SystemMetrics systemMetrics;
    
    // Recent activity
    private List<String> recentErrors;
    private List<String> recentWarnings;
    private int activeOperations;
    
    // Configuration status
    private boolean configurationValid;
    private List<String> configurationIssues;
    
    public enum HealthStatus {
        HEALTHY,
        WARNING,
        CRITICAL,
        DOWN
    }
    
    @Data
    @Builder
    public static class ComponentHealth {
        private String componentName;
        private HealthStatus status;
        private String statusMessage;
        private LocalDateTime lastChecked;
        private Map<String, Object> metrics;
        private List<String> issues;
    }
    
    @Data
    @Builder
    public static class SystemMetrics {
        private double cpuUsagePercent;
        private long memoryUsedMB;
        private long memoryTotalMB;
        private double memoryUsagePercent;
        private long diskUsedMB;
        private long diskTotalMB;
        private double diskUsagePercent;
        private int activeThreads;
        private int totalThreads;
        private long networkBytesIn;
        private long networkBytesOut;
    }
}