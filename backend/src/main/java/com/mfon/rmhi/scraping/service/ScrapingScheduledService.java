package com.mfon.rmhi.scraping.service;

import com.mfon.rmhi.scraping.dto.ScrapingResult;
import com.mfon.rmhi.scraping.dto.TransformedIdea;
import com.mfon.rmhi.scraping.entity.ScrapingExecution;
import com.mfon.rmhi.scraping.entity.ScrapingSource;
import com.mfon.rmhi.scraping.repository.ScrapingExecutionRepository;
import com.mfon.rmhi.scraping.repository.ScrapingSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduled service for automated scraping job orchestration
 * Handles cron-based scheduling, job tracking, and failure notifications
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "scraping.enabled", havingValue = "true", matchIfMissing = true)
public class ScrapingScheduledService {
    
    private final ScrapingOrchestrator scrapingOrchestrator;
    private final ScrapingSourceRepository scrapingSourceRepository;
    private final ScrapingExecutionRepository scrapingExecutionRepository;
    private final MonitoringService monitoringService;
    private final IdeaScrapingService ideaScrapingService;
    private final GeminiTransformationService transformationService;
    private final StagingService stagingService;
    
    @Value("${scraping.scheduling.enabled:true}")
    private boolean schedulingEnabled;
    
    @Value("${scraping.scheduling.max-concurrent-jobs:1}")
    private int maxConcurrentJobs;
    
    @Value("${scraping.scheduling.failure-notification-enabled:true}")
    private boolean failureNotificationEnabled;
    
    @Value("${scraping.scheduling.retry-failed-jobs:true}")
    private boolean retryFailedJobs;
    
    @Value("${scraping.scheduling.max-retry-attempts:3}")
    private int maxRetryAttempts;
    
    // Track running jobs to prevent concurrent execution
    private final Map<String, JobExecutionContext> runningJobs = new ConcurrentHashMap<>();
    private final AtomicBoolean globalJobRunning = new AtomicBoolean(false);
    
    /**
     * Main scheduled job that runs every hour to check for sources that need scraping
     * This allows for flexible per-source cron expressions
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void checkAndExecuteScheduledScraping() {
        if (!schedulingEnabled) {
            log.debug("Scraping scheduling is disabled, skipping scheduled execution check");
            return;
        }
        
        try {
            log.debug("Checking for sources that need scheduled scraping execution");
            
            List<ScrapingSource> enabledSources = scrapingSourceRepository.findByEnabledTrue();
            LocalDateTime now = LocalDateTime.now();
            
            for (ScrapingSource source : enabledSources) {
                if (shouldExecuteForSource(source, now)) {
                    executeScheduledScrapingForSource(source);
                }
            }
            
        } catch (Exception e) {
            log.error("Error during scheduled scraping check", e);
            monitoringService.recordError("SCHEDULING", "scheduled-check", 
                    e.getMessage(), e, "Failed during scheduled scraping check");
        }
    }
    
    /**
     * Daily comprehensive scraping job that runs at 2 AM
     * This is a fallback to ensure all sources are scraped at least once per day
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void executeDailyScrapingJob() {
        if (!schedulingEnabled) {
            log.debug("Scraping scheduling is disabled, skipping daily scraping job");
            return;
        }
        
        // Prevent concurrent execution of global jobs
        if (!globalJobRunning.compareAndSet(false, true)) {
            log.warn("Daily scraping job is already running, skipping this execution");
            return;
        }
        
        String jobId = "daily-scraping-" + System.currentTimeMillis();
        
        try {
            log.info("Starting daily comprehensive scraping job: {}", jobId);
            
            JobExecutionContext context = new JobExecutionContext(jobId, "DAILY_COMPREHENSIVE", LocalDateTime.now());
            runningJobs.put(jobId, context);
            
            // Execute comprehensive scraping pipeline
            List<ScrapingResult> scrapingResults = scrapingOrchestrator.executeAllScraping();
            
            // Process results through transformation and staging
            processScrapingResults(scrapingResults, context);
            
            // Mark job as completed
            context.markCompleted();
            log.info("Daily scraping job completed successfully: {} sources processed", scrapingResults.size());
            
        } catch (Exception e) {
            log.error("Daily scraping job failed: {}", jobId, e);
            handleJobFailure(jobId, "DAILY_COMPREHENSIVE", e);
            
        } finally {
            runningJobs.remove(jobId);
            globalJobRunning.set(false);
        }
    }
    
    /**
     * Retry failed jobs every 4 hours
     */
    @Scheduled(fixedRate = 14400000) // Every 4 hours
    public void retryFailedJobs() {
        if (!schedulingEnabled || !retryFailedJobs) {
            return;
        }
        
        try {
            log.debug("Checking for failed jobs to retry");
            
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
            List<ScrapingExecution> failedExecutions = scrapingExecutionRepository
                    .findByStatusAndStartedAtAfter(ScrapingExecution.ExecutionStatus.FAILED, cutoffTime);
            
            for (ScrapingExecution execution : failedExecutions) {
                if (shouldRetryExecution(execution)) {
                    retryFailedExecution(execution);
                }
            }
            
        } catch (Exception e) {
            log.error("Error during failed job retry process", e);
        }
    }
    
    /**
     * Determines if a source should be executed based on its cron expression
     */
    private boolean shouldExecuteForSource(ScrapingSource source, LocalDateTime now) {
        try {
            String cronExpression = source.getCronExpression();
            if (cronExpression == null || cronExpression.trim().isEmpty()) {
                return false;
            }
            
            // Check if there's already a recent execution for this source
            LocalDateTime oneHourAgo = now.minusHours(1);
            boolean hasRecentExecution = scrapingExecutionRepository
                    .existsBySourceAndStartedAtAfter(source, oneHourAgo);
            
            if (hasRecentExecution) {
                return false;
            }
            
            // Parse cron expression and check if it should run now
            CronExpression cron = CronExpression.parse(cronExpression);
            LocalDateTime lastExecution = getLastExecutionTime(source);
            LocalDateTime nextExecution = cron.next(lastExecution != null ? lastExecution : now.minusHours(1));
            
            return nextExecution != null && !nextExecution.isAfter(now);
            
        } catch (Exception e) {
            log.error("Error evaluating cron expression for source {}: {}", source.getName(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Executes scheduled scraping for a specific source
     */
    private void executeScheduledScrapingForSource(ScrapingSource source) {
        String jobId = "source-" + source.getName() + "-" + System.currentTimeMillis();
        
        // Check if this source is already being processed
        if (runningJobs.containsKey("source-" + source.getName())) {
            log.debug("Source {} is already being processed, skipping", source.getName());
            return;
        }
        
        try {
            log.info("Starting scheduled scraping for source: {} (job: {})", source.getName(), jobId);
            
            JobExecutionContext context = new JobExecutionContext(jobId, "SOURCE_SPECIFIC", LocalDateTime.now());
            context.setSourceName(source.getName());
            runningJobs.put(jobId, context);
            
            // Execute scraping for this specific source
            ScrapingResult result = scrapingOrchestrator.executeScraping(source);
            
            // Process the result through transformation and staging
            processScrapingResults(List.of(result), context);
            
            context.markCompleted();
            log.info("Scheduled scraping completed for source: {}", source.getName());
            
        } catch (Exception e) {
            log.error("Scheduled scraping failed for source: {}", source.getName(), e);
            handleJobFailure(jobId, "SOURCE_SPECIFIC", e);
            
        } finally {
            runningJobs.remove(jobId);
        }
    }
    
    /**
     * Processes scraping results through transformation and staging pipeline
     */
    private void processScrapingResults(List<ScrapingResult> scrapingResults, JobExecutionContext context) {
        for (ScrapingResult result : scrapingResults) {
            if (!result.isSuccessful() || result.getScrapedIdeas().isEmpty()) {
                continue;
            }
            
            try {
                // Transform scraped ideas using AI
                List<TransformedIdea> transformedIdeas = transformationService.batchTransform(result.getScrapedIdeas());
                context.incrementTransformed(transformedIdeas.size());
                
                // Stage transformed ideas
                for (TransformedIdea transformedIdea : transformedIdeas) {
                    stagingService.storeIdea(transformedIdea);
                    context.incrementStaged(1);
                }
                
                log.info("Processed {} ideas from source {}: {} transformed, {} staged", 
                        result.getIdeasScraped(), result.getSourceName(),
                        transformedIdeas.size(), transformedIdeas.size());
                
            } catch (Exception e) {
                log.error("Error processing results from source {}: {}", result.getSourceName(), e.getMessage());
                context.addError("Processing failed for " + result.getSourceName() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Handles job failure with notification and tracking
     */
    private void handleJobFailure(String jobId, String jobType, Exception error) {
        try {
            // Record the failure in monitoring
            monitoringService.recordError("SCHEDULING", jobId, error.getMessage(), error,
                    String.format("Scheduled job failed: %s (%s)", jobId, jobType));
            
            // Send failure notification if enabled
            if (failureNotificationEnabled) {
                sendFailureNotification(jobId, jobType, error);
            }
            
            // Update job context
            JobExecutionContext context = runningJobs.get(jobId);
            if (context != null) {
                context.markFailed(error.getMessage());
            }
            
        } catch (Exception e) {
            log.error("Error handling job failure for {}: {}", jobId, e.getMessage());
        }
    }
    
    /**
     * Sends failure notification (placeholder for actual notification implementation)
     */
    private void sendFailureNotification(String jobId, String jobType, Exception error) {
        // In a production environment, this would:
        // - Send email notifications
        // - Post to Slack/Teams
        // - Create alerts in monitoring systems
        // - Update dashboards
        
        log.error("FAILURE NOTIFICATION: Job {} ({}) failed with error: {}", 
                jobId, jobType, error.getMessage());
        
        // For now, just log the notification
        // Future implementation could integrate with:
        // - Spring Boot Actuator for health checks
        // - External notification services
        // - Webhook endpoints
    }
    
    /**
     * Gets the last execution time for a source
     */
    private LocalDateTime getLastExecutionTime(ScrapingSource source) {
        return scrapingExecutionRepository
                .findTopBySourceOrderByStartedAtDesc(source)
                .map(ScrapingExecution::getStartedAt)
                .orElse(null);
    }
    
    /**
     * Determines if a failed execution should be retried
     */
    private boolean shouldRetryExecution(ScrapingExecution execution) {
        // Check retry count in metadata
        Map<String, Object> metadata = execution.getExecutionMetadata();
        if (metadata != null) {
            Integer retryCount = (Integer) metadata.get("retryCount");
            if (retryCount != null && retryCount >= maxRetryAttempts) {
                return false;
            }
        }
        
        // Don't retry if too recent
        LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);
        return execution.getStartedAt().isBefore(twoHoursAgo);
    }
    
    /**
     * Retries a failed execution
     */
    private void retryFailedExecution(ScrapingExecution failedExecution) {
        try {
            log.info("Retrying failed execution for source: {}", failedExecution.getSource().getName());
            
            // Update retry count
            Map<String, Object> metadata = failedExecution.getExecutionMetadata();
            if (metadata == null) {
                metadata = new ConcurrentHashMap<>();
            }
            Integer retryCount = (Integer) metadata.getOrDefault("retryCount", 0);
            metadata.put("retryCount", retryCount + 1);
            metadata.put("lastRetryAt", LocalDateTime.now().toString());
            failedExecution.setExecutionMetadata(metadata);
            scrapingExecutionRepository.save(failedExecution);
            
            // Execute the retry
            executeScheduledScrapingForSource(failedExecution.getSource());
            
        } catch (Exception e) {
            log.error("Failed to retry execution for source: {}", 
                    failedExecution.getSource().getName(), e);
        }
    }
    
    /**
     * Gets current job execution status
     */
    public Map<String, JobExecutionContext> getRunningJobs() {
        return Map.copyOf(runningJobs);
    }
    
    /**
     * Checks if scheduling is currently enabled
     */
    public boolean isSchedulingEnabled() {
        return schedulingEnabled;
    }
    
    /**
     * Context class to track job execution details
     */
    public static class JobExecutionContext {
        private final String jobId;
        private final String jobType;
        private final LocalDateTime startTime;
        private LocalDateTime endTime;
        private String status = "RUNNING";
        private String sourceName;
        private int ideasScraped = 0;
        private int ideasTransformed = 0;
        private int ideasStaged = 0;
        private final List<String> errors = new java.util.ArrayList<>();
        
        public JobExecutionContext(String jobId, String jobType, LocalDateTime startTime) {
            this.jobId = jobId;
            this.jobType = jobType;
            this.startTime = startTime;
        }
        
        public void markCompleted() {
            this.status = "COMPLETED";
            this.endTime = LocalDateTime.now();
        }
        
        public void markFailed(String errorMessage) {
            this.status = "FAILED";
            this.endTime = LocalDateTime.now();
            this.errors.add(errorMessage);
        }
        
        public void incrementScraped(int count) {
            this.ideasScraped += count;
        }
        
        public void incrementTransformed(int count) {
            this.ideasTransformed += count;
        }
        
        public void incrementStaged(int count) {
            this.ideasStaged += count;
        }
        
        public void addError(String error) {
            this.errors.add(error);
        }
        
        // Getters
        public String getJobId() { return jobId; }
        public String getJobType() { return jobType; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public String getStatus() { return status; }
        public String getSourceName() { return sourceName; }
        public void setSourceName(String sourceName) { this.sourceName = sourceName; }
        public int getIdeasScraped() { return ideasScraped; }
        public int getIdeasTransformed() { return ideasTransformed; }
        public int getIdeasStaged() { return ideasStaged; }
        public List<String> getErrors() { return List.copyOf(errors); }
        
        public long getDurationMs() {
            LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
            return java.time.Duration.between(startTime, end).toMillis();
        }
    }
}