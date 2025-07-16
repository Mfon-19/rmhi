package com.mfon.rmhi.scraping.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO representing a staging summary report with statistics
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StagingSummaryReport {
    
    // Report metadata
    private LocalDateTime generatedAt;
    private String reportPeriod;
    
    // Review status counts
    private long pendingCount;
    private long approvedCount;
    private long rejectedCount;
    private long totalIdeas;
    
    // Migration status counts
    private long notMigratedCount;
    private long migratedCount;
    private long migrationFailedCount;
    
    // Source website breakdown
    private Map<String, Long> ideasBySource;
    
    // Time-based statistics
    private long ideasScrapedToday;
    private long ideasScrapedThisWeek;
    private long ideasScrapedThisMonth;
    
    // Quality metrics
    private Double averageRating;
    private long highQualityIdeas; // Rating >= 7
    private long mediumQualityIdeas; // Rating 4-6
    private long lowQualityIdeas; // Rating <= 3
    
    // Processing statistics
    private long duplicatesDetected;
    private long transformationFailures;
    
    // Recent activity
    private LocalDateTime lastScrapingTime;
    private LocalDateTime lastReviewTime;
    private LocalDateTime lastMigrationTime;
    
    /**
     * Calculate the approval rate as a percentage
     * @return Approval rate (0-100)
     */
    public double getApprovalRate() {
        long reviewedTotal = approvedCount + rejectedCount;
        if (reviewedTotal == 0) {
            return 0.0;
        }
        return (double) approvedCount / reviewedTotal * 100.0;
    }
    
    /**
     * Calculate the migration success rate as a percentage
     * @return Migration success rate (0-100)
     */
    public double getMigrationSuccessRate() {
        long migrationTotal = migratedCount + migrationFailedCount;
        if (migrationTotal == 0) {
            return 0.0;
        }
        return (double) migratedCount / migrationTotal * 100.0;
    }
    
    /**
     * Get the percentage of high-quality ideas
     * @return High quality percentage (0-100)
     */
    public double getHighQualityPercentage() {
        if (totalIdeas == 0) {
            return 0.0;
        }
        return (double) highQualityIdeas / totalIdeas * 100.0;
    }
}