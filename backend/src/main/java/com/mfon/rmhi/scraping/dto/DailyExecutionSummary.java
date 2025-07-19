package com.mfon.rmhi.scraping.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Daily execution summary report containing all pipeline operations
 */
@Data
@Builder
public class DailyExecutionSummary {
    
    private LocalDate reportDate;
    private LocalDateTime generatedAt;
    
    // Overall statistics
    private int totalOperations;
    private int successfulOperations;
    private int failedOperations;
    private double successRate;
    
    // Scraping statistics
    private int scrapingExecutions;
    private int totalIdeasScraped;
    private int averageIdeasPerSource;
    private long averageScrapingDurationMs;
    
    // Transformation statistics
    private int transformationExecutions;
    private int totalIdeasTransformed;
    private double transformationSuccessRate;
    private long averageTransformationDurationMs;
    
    // Staging statistics
    private int stagingOperations;
    private int totalIdeasStaged;
    private int duplicatesDetected;
    private long averageStagingDurationMs;
    
    // Migration statistics
    private int migrationExecutions;
    private int totalIdeasMigrated;
    private double migrationSuccessRate;
    private long averageMigrationDurationMs;
    
    // Error statistics
    private int totalErrors;
    private Map<String, Integer> errorsByType;
    private List<String> criticalErrors;
    
    // Performance metrics
    private long totalExecutionTimeMs;
    private double averageMemoryUsageMB;
    private double peakMemoryUsageMB;
    private int concurrentOperationsPeak;
    
    // Source-specific statistics
    private Map<String, SourceStatistics> sourceStatistics;
    
    // Health indicators
    private List<String> healthWarnings;
    private List<String> performanceAlerts;
    
    @Data
    @Builder
    public static class SourceStatistics {
        private String sourceName;
        private int executionCount;
        private int ideasScraped;
        private int successfulExecutions;
        private double successRate;
        private long averageDurationMs;
        private List<String> errors;
    }
}