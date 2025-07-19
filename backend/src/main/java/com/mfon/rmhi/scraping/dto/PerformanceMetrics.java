package com.mfon.rmhi.scraping.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Performance metrics for monitoring system performance
 */
@Data
@Builder
public class PerformanceMetrics {
    
    private String operationType;
    private String operationId;
    private LocalDateTime timestamp;
    
    // Timing metrics
    private long durationMs;
    private long startTimeMs;
    private long endTimeMs;
    
    // Memory metrics
    private long memoryUsedMB;
    private long memoryAvailableMB;
    private long memoryTotalMB;
    private double memoryUsagePercent;
    
    // Processing metrics
    private int itemsProcessed;
    private double itemsPerSecond;
    private int batchSize;
    private int concurrentOperations;
    
    // Network metrics (for scraping operations)
    private Integer httpRequestCount;
    private Long totalHttpResponseTimeMs;
    private Double averageHttpResponseTimeMs;
    private Integer httpErrorCount;
    
    // Database metrics (for staging/migration operations)
    private Integer databaseQueryCount;
    private Long totalDatabaseTimeMs;
    private Double averageDatabaseTimeMs;
    private Integer databaseErrorCount;
    
    // AI service metrics (for transformation operations)
    private Integer aiRequestCount;
    private Long totalAiResponseTimeMs;
    private Double averageAiResponseTimeMs;
    private Integer aiErrorCount;
    private Double aiSuccessRate;
    
    // Custom metrics
    private Map<String, Object> customMetrics;
    
    // Status indicators
    private boolean successful;
    private String errorMessage;
    private String context;
}