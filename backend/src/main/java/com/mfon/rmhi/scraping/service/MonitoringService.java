package com.mfon.rmhi.scraping.service;

import com.mfon.rmhi.scraping.dto.DailyExecutionSummary;
import com.mfon.rmhi.scraping.dto.PerformanceMetrics;
import com.mfon.rmhi.scraping.dto.SystemHealthStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for monitoring and metrics collection
 */
public interface MonitoringService {
    
    /**
     * Records the start of a pipeline operation
     */
    void recordOperationStart(String operationType, String operationId, String context);
    
    /**
     * Records the completion of a pipeline operation
     */
    void recordOperationComplete(String operationType, String operationId, long durationMs, boolean successful);
    
    /**
     * Records an error with full context and stack trace
     */
    void recordError(String operationType, String operationId, String errorMessage, Throwable throwable, String context);
    
    /**
     * Records performance metrics for an operation
     */
    void recordPerformanceMetrics(String operationType, PerformanceMetrics metrics);
    
    /**
     * Generates daily execution summary report
     */
    DailyExecutionSummary generateDailyReport(LocalDate date);
    
    /**
     * Gets current system health status
     */
    SystemHealthStatus getSystemHealth();
    
    /**
     * Gets performance metrics for a specific operation type
     */
    List<PerformanceMetrics> getPerformanceMetrics(String operationType, LocalDate startDate, LocalDate endDate);
    
    /**
     * Cleans up old monitoring data based on retention policy
     */
    void cleanupOldData(int retentionDays);
}