package com.mfon.rmhi.scraping.controller;

import com.mfon.rmhi.scraping.service.ScrapingScheduledService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for managing and monitoring scheduled scraping jobs
 */
@Slf4j
@RestController
@RequestMapping("/api/scraping/scheduling")
@RequiredArgsConstructor
public class SchedulingController {
    
    private final ScrapingScheduledService scheduledService;
    
    /**
     * Get status of all currently running jobs
     */
    @GetMapping("/jobs/running")
    public ResponseEntity<Map<String, ScrapingScheduledService.JobExecutionContext>> getRunningJobs() {
        try {
            Map<String, ScrapingScheduledService.JobExecutionContext> runningJobs = scheduledService.getRunningJobs();
            return ResponseEntity.ok(runningJobs);
        } catch (Exception e) {
            log.error("Error retrieving running jobs", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get scheduling status and configuration
     */
    @GetMapping("/status")
    public ResponseEntity<SchedulingStatus> getSchedulingStatus() {
        try {
            SchedulingStatus status = SchedulingStatus.builder()
                    .enabled(scheduledService.isSchedulingEnabled())
                    .runningJobsCount(scheduledService.getRunningJobs().size())
                    .build();
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error retrieving scheduling status", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Trigger manual execution of daily scraping job
     */
    @PostMapping("/jobs/daily/trigger")
    public ResponseEntity<String> triggerDailyJob() {
        try {
            log.info("Manual trigger requested for daily scraping job");
            scheduledService.triggerDailyJob();
            return ResponseEntity.ok("Daily scraping job has been triggered successfully.");
        } catch (Exception e) {
            log.error("Error triggering daily job", e);
            return ResponseEntity.internalServerError().body("Failed to trigger daily job");
        }
    }
    
    /**
     * DTO for scheduling status response
     */
    public static class SchedulingStatus {
        private boolean enabled;
        private int runningJobsCount;
        
        public static SchedulingStatusBuilder builder() {
            return new SchedulingStatusBuilder();
        }
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getRunningJobsCount() { return runningJobsCount; }
        public void setRunningJobsCount(int runningJobsCount) { this.runningJobsCount = runningJobsCount; }
        
        public static class SchedulingStatusBuilder {
            private boolean enabled;
            private int runningJobsCount;
            
            public SchedulingStatusBuilder enabled(boolean enabled) {
                this.enabled = enabled;
                return this;
            }
            
            public SchedulingStatusBuilder runningJobsCount(int runningJobsCount) {
                this.runningJobsCount = runningJobsCount;
                return this;
            }
            
            public SchedulingStatus build() {
                SchedulingStatus status = new SchedulingStatus();
                status.setEnabled(this.enabled);
                status.setRunningJobsCount(this.runningJobsCount);
                return status;
            }
        }
    }
}