package com.mfon.rmhi.scraping.service;

import com.mfon.rmhi.scraping.dto.ScrapedIdea;
import com.mfon.rmhi.scraping.dto.ScrapingConfig;
import com.mfon.rmhi.scraping.dto.ScrapingResult;
import com.mfon.rmhi.scraping.entity.ScrapingExecution;
import com.mfon.rmhi.scraping.entity.ScrapingSource;
import com.mfon.rmhi.scraping.repository.ScrapingExecutionRepository;
import com.mfon.rmhi.scraping.repository.ScrapingSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Orchestrates scraping operations across multiple sources and scrapers
 * Handles concurrent execution, rate limiting, and result aggregation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScrapingOrchestrator {
    
    private final ScrapingSourceRepository scrapingSourceRepository;
    private final ScrapingExecutionRepository scrapingExecutionRepository;
    private final DuplicateDetectionService duplicateDetectionService;
    private final MonitoringService monitoringService;
    private final DevPostScraper devPostScraper;
    
    private static final int MAX_CONCURRENT_SCRAPERS = 3;
    private final ExecutorService executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_SCRAPERS);
    
    /**
     * Executes scraping for all enabled sources
     */
    public List<ScrapingResult> executeAllScraping() {
        String operationId = "orchestration-" + System.currentTimeMillis();
        long startTime = System.currentTimeMillis();
        
        monitoringService.recordOperationStart("ORCHESTRATION", operationId, "Starting orchestrated scraping for all enabled sources");
        
        try {
            List<ScrapingSource> enabledSources = scrapingSourceRepository.findByEnabledTrue();
            if (enabledSources.isEmpty()) {
                log.warn("No enabled scraping sources found");
                monitoringService.recordOperationComplete("ORCHESTRATION", operationId, 
                        System.currentTimeMillis() - startTime, false);
                return List.of();
            }
            
            log.info("Found {} enabled scraping sources", enabledSources.size());
            
            // Create scraping tasks
            List<CompletableFuture<ScrapingResult>> futures = enabledSources.stream()
                    .map(this::createScrapingTask)
                    .collect(Collectors.toList());
            
            // Wait for all tasks to complete
            List<ScrapingResult> results = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
            
            // Aggregate and log results
            logAggregatedResults(results);
            
            // Record performance metrics
            recordOrchestrationMetrics(operationId, results, System.currentTimeMillis() - startTime);
            
            boolean allSuccessful = results.stream().allMatch(ScrapingResult::isSuccessful);
            monitoringService.recordOperationComplete("ORCHESTRATION", operationId, 
                    System.currentTimeMillis() - startTime, allSuccessful);
            
            return results;
            
        } catch (Exception e) {
            monitoringService.recordError("ORCHESTRATION", operationId, e.getMessage(), e, 
                    "Failed during orchestrated scraping execution");
            throw e;
        }
    }
    
    /**
     * Executes scraping for a specific source
     */
    public ScrapingResult executeScraping(ScrapingSource source) {
        log.info("Starting scraping for source: {}", source.getName());
        
        ScrapingExecution execution = createScrapingExecution(source);
        
        try {
            // Convert source to config
            ScrapingConfig config = convertToConfig(source);
            
            // Get the appropriate scraper
            if (!source.getScraperClass().equals(DevPostScraper.class.getName())) {
                throw new IllegalStateException("Only DevPostScraper is supported, but found: " + source.getScraperClass());
            }

            // Execute scraping
            List<ScrapedIdea> scrapedIdeas = devPostScraper.scrape(config);
            
            // Filter duplicates
            List<ScrapedIdea> uniqueIdeas = duplicateDetectionService.filterDuplicates(scrapedIdeas);
            
            // Validate scraped data
            List<ScrapedIdea> validIdeas = uniqueIdeas.stream()
                    .filter(devPostScraper::validateScrapedData)
                    .collect(Collectors.toList());
            
            // Create successful result
            ScrapingResult result = ScrapingResult.builder()
                    .sourceName(source.getName())
                    .startedAt(execution.getStartedAt())
                    .completedAt(LocalDateTime.now())
                    .successful(true)
                    .ideasScraped(validIdeas.size())
                    .scrapedIdeas(validIdeas)
                    .warnings(createWarnings(scrapedIdeas.size(), uniqueIdeas.size(), validIdeas.size()))
                    .build();
            
            // Update execution record
            updateScrapingExecution(execution, result, null);
            
            log.info("Successfully completed scraping for {}: {} ideas", source.getName(), validIdeas.size());
            return result;
            
        } catch (Exception e) {
            log.error("Scraping failed for source: {}", source.getName(), e);
            
            // Create failed result
            ScrapingResult result = ScrapingResult.builder()
                    .sourceName(source.getName())
                    .startedAt(execution.getStartedAt())
                    .completedAt(LocalDateTime.now())
                    .successful(false)
                    .ideasScraped(0)
                    .scrapedIdeas(List.of())
                    .errorMessage(e.getMessage())
                    .build();
            
            // Update execution record
            updateScrapingExecution(execution, result, e.getMessage());
            
            return result;
        }
    }
    
    /**
     * Creates an asynchronous scraping task for a source
     */
    private CompletableFuture<ScrapingResult> createScrapingTask(ScrapingSource source) {
        return CompletableFuture.supplyAsync(() -> executeScraping(source), executorService);
    }
    
    /**
     * Converts ScrapingSource entity to ScrapingConfig DTO
     */
    private ScrapingConfig convertToConfig(ScrapingSource source) {
        return ScrapingConfig.builder()
                .sourceName(source.getName())
                .baseUrl(source.getBaseUrl())
                .scraperClass(source.getScraperClass())
                .enabled(source.getEnabled())
                .cronExpression(source.getCronExpression())
                .rateLimitMs(source.getRateLimitMs())
                .maxPages(source.getMaxPages())
                .configJson(source.getConfigJson())
                .build();
    }
    
    /**
     * Creates a new scraping execution record
     */
    private ScrapingExecution createScrapingExecution(ScrapingSource source) {
        ScrapingExecution execution = new ScrapingExecution();
        execution.setSource(source);
        execution.setStartedAt(LocalDateTime.now());
        execution.setStatus(ScrapingExecution.ExecutionStatus.RUNNING);
        execution.setIdeasScraped(0);
        execution.setIdeasTransformed(0);
        execution.setIdeasStaged(0);
        
        return scrapingExecutionRepository.save(execution);
    }
    
    /**
     * Updates scraping execution record with results
     */
    private void updateScrapingExecution(ScrapingExecution execution, ScrapingResult result, String errorMessage) {
        execution.setCompletedAt(result.getCompletedAt());
        execution.setStatus(result.isSuccessful() ? 
                ScrapingExecution.ExecutionStatus.COMPLETED : 
                ScrapingExecution.ExecutionStatus.FAILED);
        execution.setIdeasScraped(result.getIdeasScraped());
        execution.setErrorMessage(errorMessage);
        
        scrapingExecutionRepository.save(execution);
    }
    
    /**
     * Creates warning messages for data quality issues
     */
    private List<String> createWarnings(int originalCount, int uniqueCount, int validCount) {
        List<String> warnings = new ArrayList<>();
        
        int duplicatesRemoved = originalCount - uniqueCount;
        if (duplicatesRemoved > 0) {
            warnings.add(String.format("Removed %d duplicate ideas", duplicatesRemoved));
        }
        
        int invalidRemoved = uniqueCount - validCount;
        if (invalidRemoved > 0) {
            warnings.add(String.format("Removed %d invalid ideas", invalidRemoved));
        }
        
        return warnings;
    }
    
    /**
     * Logs aggregated results from all scraping operations
     */
    private void logAggregatedResults(List<ScrapingResult> results) {
        int totalIdeas = results.stream().mapToInt(ScrapingResult::getIdeasScraped).sum();
        long successfulSources = results.stream().filter(ScrapingResult::isSuccessful).count();
        long failedSources = results.size() - successfulSources;
        
        log.info("Scraping orchestration completed: {} total ideas from {} sources ({} successful, {} failed)",
                totalIdeas, results.size(), successfulSources, failedSources);
        
        // Log individual source results
        results.forEach(result -> {
            if (result.isSuccessful()) {
                log.info("✓ {}: {} ideas", result.getSourceName(), result.getIdeasScraped());
            } else {
                log.error("✗ {}: {}", result.getSourceName(), result.getErrorMessage());
            }
        });
    }
    
    /**
     * Records performance metrics for orchestration operations
     */
    private void recordOrchestrationMetrics(String operationId, List<ScrapingResult> results, long durationMs) {
        int totalIdeas = results.stream().mapToInt(ScrapingResult::getIdeasScraped).sum();
        long successfulSources = results.stream().filter(ScrapingResult::isSuccessful).count();
        
        com.mfon.rmhi.scraping.dto.PerformanceMetrics metrics = com.mfon.rmhi.scraping.dto.PerformanceMetrics.builder()
                .operationType("ORCHESTRATION")
                .operationId(operationId)
                .timestamp(LocalDateTime.now())
                .durationMs(durationMs)
                .itemsProcessed(totalIdeas)
                .itemsPerSecond(durationMs > 0 ? (double) totalIdeas / (durationMs / 1000.0) : 0)
                .batchSize(results.size())
                .successful(successfulSources == results.size())
                .context(String.format("Processed %d sources, scraped %d ideas", results.size(), totalIdeas))
                .build();
        
        monitoringService.recordPerformanceMetrics("ORCHESTRATION", metrics);
    }
    
    /**
     * Shuts down the executor service gracefully
     */
    public void shutdown() {
        log.info("Shutting down scraping orchestrator");
        executorService.shutdown();
    }
}