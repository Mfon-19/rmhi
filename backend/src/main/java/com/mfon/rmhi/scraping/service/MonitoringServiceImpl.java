package com.mfon.rmhi.scraping.service;

import com.mfon.rmhi.scraping.dto.DailyExecutionSummary;
import com.mfon.rmhi.scraping.dto.PerformanceMetrics;
import com.mfon.rmhi.scraping.dto.SystemHealthStatus;
import com.mfon.rmhi.scraping.entity.ScrapingExecution;
import com.mfon.rmhi.scraping.repository.ScrapingExecutionRepository;
import com.mfon.rmhi.scraping.repository.ScrapingSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Implementation of monitoring service with comprehensive logging and metrics
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringServiceImpl implements MonitoringService {
    
    private final ScrapingExecutionRepository scrapingExecutionRepository;
    private final ScrapingSourceRepository scrapingSourceRepository;
    
    @Value("${scraping.monitoring.enable-metrics:true}")
    private boolean metricsEnabled;
    
    @Value("${scraping.monitoring.log-level:INFO}")
    private String logLevel;
    
    // In-memory storage for performance metrics (in production, consider using a time-series database)
    private final Map<String, List<PerformanceMetrics>> performanceMetricsStore = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> operationCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> errorCounters = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> operationStartTimes = new ConcurrentHashMap<>();
    private final List<String> recentErrors = Collections.synchronizedList(new ArrayList<>());
    private final List<String> recentWarnings = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger activeOperations = new AtomicInteger(0);
    
    private final LocalDateTime startupTime = LocalDateTime.now();
    
    @Override
    public void recordOperationStart(String operationType, String operationId, String context) {
        if (!metricsEnabled) return;
        
        String key = operationType + ":" + operationId;
        operationStartTimes.put(key, LocalDateTime.now());
        activeOperations.incrementAndGet();
        
        // Structured logging with timestamp and context
        log.info("OPERATION_START | Type: {} | ID: {} | Context: {} | Timestamp: {} | ActiveOps: {}", 
                operationType, operationId, context, LocalDateTime.now(), activeOperations.get());
        
        // Update operation counters
        operationCounters.computeIfAbsent(operationType, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    @Override
    public void recordOperationComplete(String operationType, String operationId, long durationMs, boolean successful) {
        if (!metricsEnabled) return;
        
        String key = operationType + ":" + operationId;
        operationStartTimes.remove(key);
        activeOperations.decrementAndGet();
        
        // Structured logging with performance data
        log.info("OPERATION_COMPLETE | Type: {} | ID: {} | Duration: {}ms | Success: {} | Timestamp: {} | ActiveOps: {}", 
                operationType, operationId, durationMs, successful, LocalDateTime.now(), activeOperations.get());
        
        // Record performance metrics
        PerformanceMetrics metrics = PerformanceMetrics.builder()
                .operationType(operationType)
                .operationId(operationId)
                .timestamp(LocalDateTime.now())
                .durationMs(durationMs)
                .successful(successful)
                .context("Operation completed")
                .build();
        
        recordPerformanceMetrics(operationType, metrics);
        
        // Update error counters if failed
        if (!successful) {
            errorCounters.computeIfAbsent(operationType, k -> new AtomicLong(0)).incrementAndGet();
        }
    }
    
    @Override
    public void recordError(String operationType, String operationId, String errorMessage, Throwable throwable, String context) {
        String key = operationType + ":" + operationId;
        operationStartTimes.remove(key);
        activeOperations.decrementAndGet();
        
        // Get stack trace as string
        String stackTrace = getStackTrace(throwable);
        
        // Structured error logging with full context
        log.error("OPERATION_ERROR | Type: {} | ID: {} | Error: {} | Context: {} | Timestamp: {} | StackTrace: {}", 
                operationType, operationId, errorMessage, context, LocalDateTime.now(), stackTrace);
        
        // Store recent error for health monitoring
        String errorEntry = String.format("[%s] %s:%s - %s", 
                LocalDateTime.now(), operationType, operationId, errorMessage);
        addRecentError(errorEntry);
        
        // Update error counters
        errorCounters.computeIfAbsent(operationType, k -> new AtomicLong(0)).incrementAndGet();
        
        // Record error metrics
        PerformanceMetrics errorMetrics = PerformanceMetrics.builder()
                .operationType(operationType)
                .operationId(operationId)
                .timestamp(LocalDateTime.now())
                .successful(false)
                .errorMessage(errorMessage)
                .context(context)
                .build();
        
        recordPerformanceMetrics(operationType, errorMetrics);
    }
    
    @Override
    public void recordPerformanceMetrics(String operationType, PerformanceMetrics metrics) {
        if (!metricsEnabled) return;
        
        // Add system metrics to the performance metrics
        enrichWithSystemMetrics(metrics);
        
        // Store metrics
        performanceMetricsStore.computeIfAbsent(operationType, k -> new ArrayList<>()).add(metrics);
        
        // Log performance metrics if debug level
        if ("DEBUG".equalsIgnoreCase(logLevel)) {
            log.debug("PERFORMANCE_METRICS | Type: {} | Duration: {}ms | Memory: {}MB | Items: {} | Success: {}", 
                    operationType, metrics.getDurationMs(), metrics.getMemoryUsedMB(), 
                    metrics.getItemsProcessed(), metrics.isSuccessful());
        }
        
        // Clean up old metrics to prevent memory leaks
        cleanupOldMetrics(operationType);
    }
    
    @Override
    public DailyExecutionSummary generateDailyReport(LocalDate date) {
        log.info("Generating daily execution summary for date: {}", date);
        
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        
        // Get executions for the day
        List<ScrapingExecution> executions = scrapingExecutionRepository
                .findByStartedAtBetween(startOfDay, endOfDay);
        
        // Calculate overall statistics
        int totalOperations = executions.size();
        long successfulOperations = executions.stream()
                .filter(e -> e.getStatus() == ScrapingExecution.ExecutionStatus.COMPLETED)
                .count();
        int failedOperations = totalOperations - (int) successfulOperations;
        double successRate = totalOperations > 0 ? (double) successfulOperations / totalOperations * 100 : 0;
        
        // Calculate scraping statistics
        int totalIdeasScraped = executions.stream()
                .mapToInt(ScrapingExecution::getIdeasScraped)
                .sum();
        
        double averageScrapingDuration = executions.stream()
                .filter(e -> e.getCompletedAt() != null)
                .mapToLong(e -> ChronoUnit.MILLIS.between(e.getStartedAt(), e.getCompletedAt()))
                .average()
                .orElse(0.0);
        
        // Get performance metrics for the day
        Map<String, Integer> errorsByType = new HashMap<>();
        List<String> criticalErrors = new ArrayList<>();
        
        for (ScrapingExecution execution : executions) {
            if (execution.getErrorMessage() != null) {
                String errorType = execution.getStatus().name();
                errorsByType.merge(errorType, 1, Integer::sum);
                
                if (execution.getStatus() == ScrapingExecution.ExecutionStatus.FAILED) {
                    criticalErrors.add(String.format("%s: %s", 
                            execution.getSource().getName(), execution.getErrorMessage()));
                }
            }
        }
        
        // Calculate source-specific statistics
        Map<String, DailyExecutionSummary.SourceStatistics> sourceStats = executions.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getSource().getName(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                this::calculateSourceStatistics
                        )
                ));
        
        // Generate health warnings and performance alerts
        List<String> healthWarnings = generateHealthWarnings(executions);
        List<String> performanceAlerts = generatePerformanceAlerts(date);
        
        DailyExecutionSummary summary = DailyExecutionSummary.builder()
                .reportDate(date)
                .generatedAt(LocalDateTime.now())
                .totalOperations(totalOperations)
                .successfulOperations((int) successfulOperations)
                .failedOperations(failedOperations)
                .successRate(successRate)
                .scrapingExecutions(totalOperations)
                .totalIdeasScraped(totalIdeasScraped)
                .averageIdeasPerSource(totalOperations > 0 ? totalIdeasScraped / totalOperations : 0)
                .averageScrapingDurationMs((long) averageScrapingDuration)
                .totalErrors(criticalErrors.size())
                .errorsByType(errorsByType)
                .criticalErrors(criticalErrors)
                .sourceStatistics(sourceStats)
                .healthWarnings(healthWarnings)
                .performanceAlerts(performanceAlerts)
                .build();
        
        log.info("Daily report generated: {} operations, {:.1f}% success rate, {} ideas scraped", 
                totalOperations, successRate, totalIdeasScraped);
        
        return summary;
    }
    
    @Override
    public SystemHealthStatus getSystemHealth() {
        LocalDateTime now = LocalDateTime.now();
        
        // Check component health
        Map<String, SystemHealthStatus.ComponentHealth> componentHealth = new HashMap<>();
        
        // Database health
        componentHealth.put("database", checkDatabaseHealth());
        
        // Scraping sources health
        componentHealth.put("scraping_sources", checkScrapingSourcesHealth());
        
        // AI service health
        componentHealth.put("ai_service", checkAIServiceHealth());
        
        // System metrics
        SystemHealthStatus.SystemMetrics systemMetrics = getSystemMetrics();
        
        // Determine overall health status
        SystemHealthStatus.HealthStatus overallStatus = determineOverallHealth(componentHealth, systemMetrics);
        
        // Get recent issues
        List<String> recentErrorsCopy = new ArrayList<>(recentErrors);
        List<String> recentWarningsCopy = new ArrayList<>(recentWarnings);
        
        // Configuration validation
        boolean configValid = validateConfiguration();
        List<String> configIssues = getConfigurationIssues();
        
        return SystemHealthStatus.builder()
                .timestamp(now)
                .overallStatus(overallStatus)
                .version("1.0.0") // Should be read from application properties
                .uptimeMs(ChronoUnit.MILLIS.between(startupTime, now))
                .componentHealth(componentHealth)
                .systemMetrics(systemMetrics)
                .recentErrors(recentErrorsCopy.subList(Math.max(0, recentErrorsCopy.size() - 10), recentErrorsCopy.size()))
                .recentWarnings(recentWarningsCopy.subList(Math.max(0, recentWarningsCopy.size() - 10), recentWarningsCopy.size()))
                .activeOperations(activeOperations.get())
                .configurationValid(configValid)
                .configurationIssues(configIssues)
                .build();
    }
    
    @Override
    public List<PerformanceMetrics> getPerformanceMetrics(String operationType, LocalDate startDate, LocalDate endDate) {
        List<PerformanceMetrics> metrics = performanceMetricsStore.getOrDefault(operationType, new ArrayList<>());
        
        return metrics.stream()
                .filter(m -> {
                    LocalDate metricDate = m.getTimestamp().toLocalDate();
                    return !metricDate.isBefore(startDate) && !metricDate.isAfter(endDate);
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public void cleanupOldData(int retentionDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        
        log.info("Cleaning up monitoring data older than {} days", retentionDays);
        
        // Clean up performance metrics
        performanceMetricsStore.forEach((operationType, metrics) -> {
            int originalSize = metrics.size();
            metrics.removeIf(m -> m.getTimestamp().isBefore(cutoffDate));
            int removedCount = originalSize - metrics.size();
            if (removedCount > 0) {
                log.debug("Cleaned up {} old performance metrics for operation type: {}", removedCount, operationType);
            }
        });
        
        // Clean up recent errors and warnings
        synchronized (recentErrors) {
            if (recentErrors.size() > 100) {
                recentErrors.subList(0, recentErrors.size() - 100).clear();
            }
        }
        
        synchronized (recentWarnings) {
            if (recentWarnings.size() > 100) {
                recentWarnings.subList(0, recentWarnings.size() - 100).clear();
            }
        }
        
        log.info("Monitoring data cleanup completed");
    }
    
    // Helper methods
    
    private void enrichWithSystemMetrics(PerformanceMetrics metrics) {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
        
        metrics.setMemoryUsedMB(usedMemory / (1024 * 1024));
        metrics.setMemoryTotalMB(maxMemory / (1024 * 1024));
        metrics.setMemoryUsagePercent((double) usedMemory / maxMemory * 100);
        metrics.setConcurrentOperations(activeOperations.get());
    }
    
    private void cleanupOldMetrics(String operationType) {
        List<PerformanceMetrics> metrics = performanceMetricsStore.get(operationType);
        if (metrics != null && metrics.size() > 1000) {
            // Keep only the most recent 1000 metrics per operation type
            metrics.subList(0, metrics.size() - 1000).clear();
        }
    }
    
    private String getStackTrace(Throwable throwable) {
        if (throwable == null) return "";
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
    
    private void addRecentError(String error) {
        synchronized (recentErrors) {
            recentErrors.add(error);
            if (recentErrors.size() > 50) {
                recentErrors.remove(0);
            }
        }
    }
    
    private DailyExecutionSummary.SourceStatistics calculateSourceStatistics(List<ScrapingExecution> executions) {
        if (executions.isEmpty()) {
            return DailyExecutionSummary.SourceStatistics.builder().build();
        }
        
        String sourceName = executions.get(0).getSource().getName();
        int executionCount = executions.size();
        int ideasScraped = executions.stream().mapToInt(ScrapingExecution::getIdeasScraped).sum();
        long successfulExecutions = executions.stream()
                .filter(e -> e.getStatus() == ScrapingExecution.ExecutionStatus.COMPLETED)
                .count();
        double successRate = (double) successfulExecutions / executionCount * 100;
        
        double averageDuration = executions.stream()
                .filter(e -> e.getCompletedAt() != null)
                .mapToLong(e -> ChronoUnit.MILLIS.between(e.getStartedAt(), e.getCompletedAt()))
                .average()
                .orElse(0.0);
        
        List<String> errors = executions.stream()
                .filter(e -> e.getErrorMessage() != null)
                .map(ScrapingExecution::getErrorMessage)
                .collect(Collectors.toList());
        
        return DailyExecutionSummary.SourceStatistics.builder()
                .sourceName(sourceName)
                .executionCount(executionCount)
                .ideasScraped(ideasScraped)
                .successfulExecutions((int) successfulExecutions)
                .successRate(successRate)
                .averageDurationMs((long) averageDuration)
                .errors(errors)
                .build();
    }
    
    private List<String> generateHealthWarnings(List<ScrapingExecution> executions) {
        List<String> warnings = new ArrayList<>();
        
        // Check for high failure rate
        long failedCount = executions.stream()
                .filter(e -> e.getStatus() == ScrapingExecution.ExecutionStatus.FAILED)
                .count();
        
        if (executions.size() > 0 && (double) failedCount / executions.size() > 0.2) {
            warnings.add(String.format("High failure rate: %.1f%% of operations failed", 
                    (double) failedCount / executions.size() * 100));
        }
        
        // Check for no recent activity
        if (executions.isEmpty()) {
            warnings.add("No scraping activity detected for this day");
        }
        
        return warnings;
    }
    
    private List<String> generatePerformanceAlerts(LocalDate date) {
        List<String> alerts = new ArrayList<>();
        
        // Check memory usage
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        double memoryUsage = (double) memoryBean.getHeapMemoryUsage().getUsed() / 
                            memoryBean.getHeapMemoryUsage().getMax() * 100;
        
        if (memoryUsage > 80) {
            alerts.add(String.format("High memory usage: %.1f%%", memoryUsage));
        }
        
        // Check active operations
        if (activeOperations.get() > 10) {
            alerts.add(String.format("High number of active operations: %d", activeOperations.get()));
        }
        
        return alerts;
    }
    
    private SystemHealthStatus.ComponentHealth checkDatabaseHealth() {
        try {
            // Simple database health check by counting scraping sources
            long sourceCount = scrapingSourceRepository.count();
            
            return SystemHealthStatus.ComponentHealth.builder()
                    .componentName("database")
                    .status(SystemHealthStatus.HealthStatus.HEALTHY)
                    .statusMessage("Database connection healthy")
                    .lastChecked(LocalDateTime.now())
                    .metrics(Map.of("scraping_sources_count", sourceCount))
                    .issues(List.of())
                    .build();
                    
        } catch (Exception e) {
            return SystemHealthStatus.ComponentHealth.builder()
                    .componentName("database")
                    .status(SystemHealthStatus.HealthStatus.CRITICAL)
                    .statusMessage("Database connection failed: " + e.getMessage())
                    .lastChecked(LocalDateTime.now())
                    .metrics(Map.of())
                    .issues(List.of("Database connection error"))
                    .build();
        }
    }
    
    private SystemHealthStatus.ComponentHealth checkScrapingSourcesHealth() {
        try {
            long enabledSources = scrapingSourceRepository.countByEnabledTrue();
            long totalSources = scrapingSourceRepository.count();
            
            SystemHealthStatus.HealthStatus status = enabledSources > 0 ? 
                    SystemHealthStatus.HealthStatus.HEALTHY : 
                    SystemHealthStatus.HealthStatus.WARNING;
            
            return SystemHealthStatus.ComponentHealth.builder()
                    .componentName("scraping_sources")
                    .status(status)
                    .statusMessage(String.format("%d of %d sources enabled", enabledSources, totalSources))
                    .lastChecked(LocalDateTime.now())
                    .metrics(Map.of(
                            "enabled_sources", enabledSources,
                            "total_sources", totalSources
                    ))
                    .issues(enabledSources == 0 ? List.of("No enabled scraping sources") : List.of())
                    .build();
                    
        } catch (Exception e) {
            return SystemHealthStatus.ComponentHealth.builder()
                    .componentName("scraping_sources")
                    .status(SystemHealthStatus.HealthStatus.CRITICAL)
                    .statusMessage("Failed to check scraping sources: " + e.getMessage())
                    .lastChecked(LocalDateTime.now())
                    .metrics(Map.of())
                    .issues(List.of("Scraping sources check failed"))
                    .build();
        }
    }
    
    private SystemHealthStatus.ComponentHealth checkAIServiceHealth() {
        // This is a basic health check - in a real implementation, you might want to make a test API call
        return SystemHealthStatus.ComponentHealth.builder()
                .componentName("ai_service")
                .status(SystemHealthStatus.HealthStatus.HEALTHY)
                .statusMessage("AI service configuration appears valid")
                .lastChecked(LocalDateTime.now())
                .metrics(Map.of("recent_errors", errorCounters.getOrDefault("AI_TRANSFORMATION", new AtomicLong(0)).get()))
                .issues(List.of())
                .build();
    }
    
    private SystemHealthStatus.SystemMetrics getSystemMetrics() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long totalMemory = memoryBean.getHeapMemoryUsage().getMax();
        
        // Try to get CPU usage, fallback to 0 if not available
        double cpuUsage = 0.0;
        try {
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean = (com.sun.management.OperatingSystemMXBean) osBean;
                cpuUsage = sunOsBean.getProcessCpuLoad() * 100;
            }
        } catch (Exception e) {
            log.debug("CPU usage not available: {}", e.getMessage());
        }
        
        return SystemHealthStatus.SystemMetrics.builder()
                .cpuUsagePercent(cpuUsage)
                .memoryUsedMB(usedMemory / (1024 * 1024))
                .memoryTotalMB(totalMemory / (1024 * 1024))
                .memoryUsagePercent((double) usedMemory / totalMemory * 100)
                .activeThreads(threadBean.getThreadCount())
                .totalThreads(threadBean.getPeakThreadCount())
                .build();
    }
    
    private SystemHealthStatus.HealthStatus determineOverallHealth(
            Map<String, SystemHealthStatus.ComponentHealth> componentHealth,
            SystemHealthStatus.SystemMetrics systemMetrics) {
        
        // Check if any component is critical
        boolean hasCritical = componentHealth.values().stream()
                .anyMatch(c -> c.getStatus() == SystemHealthStatus.HealthStatus.CRITICAL);
        
        if (hasCritical) {
            return SystemHealthStatus.HealthStatus.CRITICAL;
        }
        
        // Check if any component has warnings
        boolean hasWarnings = componentHealth.values().stream()
                .anyMatch(c -> c.getStatus() == SystemHealthStatus.HealthStatus.WARNING);
        
        // Check system resource usage
        boolean highResourceUsage = systemMetrics.getMemoryUsagePercent() > 80 || 
                                   systemMetrics.getCpuUsagePercent() > 80;
        
        if (hasWarnings || highResourceUsage) {
            return SystemHealthStatus.HealthStatus.WARNING;
        }
        
        return SystemHealthStatus.HealthStatus.HEALTHY;
    }
    
    private boolean validateConfiguration() {
        // Basic configuration validation
        return metricsEnabled && logLevel != null && !logLevel.trim().isEmpty();
    }
    
    private List<String> getConfigurationIssues() {
        List<String> issues = new ArrayList<>();
        
        if (!metricsEnabled) {
            issues.add("Metrics collection is disabled");
        }
        
        if (logLevel == null || logLevel.trim().isEmpty()) {
            issues.add("Log level is not configured");
        }
        
        return issues;
    }
}