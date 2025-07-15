package com.mfon.rmhi.scraping.service;

import com.mfon.rmhi.scraping.dto.ScrapedIdea;
import com.mfon.rmhi.scraping.dto.ScrapingConfig;
import com.mfon.rmhi.scraping.dto.ScrapingResult;
import com.mfon.rmhi.scraping.entity.ScrapingExecution;
import com.mfon.rmhi.scraping.entity.ScrapingSource;
import com.mfon.rmhi.scraping.repository.ScrapingExecutionRepository;
import com.mfon.rmhi.scraping.repository.ScrapingSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScrapingOrchestratorTest {

    @Mock
    private ScrapingSourceRepository scrapingSourceRepository;

    @Mock
    private ScrapingExecutionRepository scrapingExecutionRepository;

    @Mock
    private DuplicateDetectionService duplicateDetectionService;

    @Mock
    private AbstractWebsiteScraper mockScraper;

    @InjectMocks
    private ScrapingOrchestrator scrapingOrchestrator;

    private ScrapingSource testSource;
    private ScrapingExecution testExecution;
    private List<ScrapedIdea> testIdeas;

    @BeforeEach
    void setUp() {
        testSource = new ScrapingSource();
        testSource.setId(1);
        testSource.setName("test-source");
        testSource.setBaseUrl("https://example.com");
        testSource.setScraperClass("TestScraper");
        testSource.setEnabled(true);
        testSource.setRateLimitMs(100);
        testSource.setMaxPages(10);
        testSource.setCronExpression("0 0 2 * * ?");
        testSource.setConfigJson(Map.of("key", "value"));

        testExecution = new ScrapingExecution();
        testExecution.setId(1L);
        testExecution.setSource(testSource);
        testExecution.setStartedAt(LocalDateTime.now());
        testExecution.setStatus(ScrapingExecution.ExecutionStatus.RUNNING);

        testIdeas = List.of(
                ScrapedIdea.builder()
                        .title("Test Idea 1")
                        .description("Description 1")
                        .sourceUrl("https://example.com/1")
                        .sourceWebsite("example.com")
                        .contentHash("hash1")
                        .build(),
                ScrapedIdea.builder()
                        .title("Test Idea 2")
                        .description("Description 2")
                        .sourceUrl("https://example.com/2")
                        .sourceWebsite("example.com")
                        .contentHash("hash2")
                        .build()
        );
    }

    @Test
    void testRegisterScraper() {
        // When
        scrapingOrchestrator.registerScraper("TestScraper", mockScraper);

        // Then - no exception should be thrown
        // The scraper should be registered internally (tested indirectly in other tests)
    }

    @Test
    void testExecuteAllScrapingWithNoEnabledSources() {
        // Given
        when(scrapingSourceRepository.findByEnabledTrue()).thenReturn(List.of());

        // When
        List<ScrapingResult> results = scrapingOrchestrator.executeAllScraping();

        // Then
        assertTrue(results.isEmpty());
        verify(scrapingSourceRepository).findByEnabledTrue();
    }

    @Test
    void testExecuteAllScrapingWithEnabledSources() {
        // Given
        List<ScrapingSource> enabledSources = List.of(testSource);
        when(scrapingSourceRepository.findByEnabledTrue()).thenReturn(enabledSources);
        when(scrapingExecutionRepository.save(any(ScrapingExecution.class))).thenReturn(testExecution);
        when(mockScraper.scrapeIdeas(any(ScrapingConfig.class))).thenReturn(testIdeas);
        when(duplicateDetectionService.filterDuplicates(testIdeas)).thenReturn(testIdeas);
        when(mockScraper.validateScrapedData(any(ScrapedIdea.class))).thenReturn(true);

        scrapingOrchestrator.registerScraper("TestScraper", mockScraper);

        // When
        List<ScrapingResult> results = scrapingOrchestrator.executeAllScraping();

        // Then
        assertEquals(1, results.size());
        ScrapingResult result = results.get(0);
        assertTrue(result.isSuccessful());
        assertEquals("test-source", result.getSourceName());
        assertEquals(2, result.getIdeasScraped());
    }

    @Test
    void testExecuteScrapingSuccess() {
        // Given
        when(scrapingExecutionRepository.save(any(ScrapingExecution.class))).thenReturn(testExecution);
        when(mockScraper.scrapeIdeas(any(ScrapingConfig.class))).thenReturn(testIdeas);
        when(duplicateDetectionService.filterDuplicates(testIdeas)).thenReturn(testIdeas);
        when(mockScraper.validateScrapedData(any(ScrapedIdea.class))).thenReturn(true);

        scrapingOrchestrator.registerScraper("TestScraper", mockScraper);

        // When
        ScrapingResult result = scrapingOrchestrator.executeScraping(testSource);

        // Then
        assertTrue(result.isSuccessful());
        assertEquals("test-source", result.getSourceName());
        assertEquals(2, result.getIdeasScraped());
        assertEquals(testIdeas, result.getScrapedIdeas());
        assertNull(result.getErrorMessage());

        verify(scrapingExecutionRepository, times(2)).save(any(ScrapingExecution.class));
        verify(mockScraper).scrapeIdeas(any(ScrapingConfig.class));
        verify(duplicateDetectionService).filterDuplicates(testIdeas);
    }

    @Test
    void testExecuteScrapingWithDuplicateFiltering() {
        // Given
        List<ScrapedIdea> filteredIdeas = List.of(testIdeas.get(0)); // Only first idea after filtering
        
        when(scrapingExecutionRepository.save(any(ScrapingExecution.class))).thenReturn(testExecution);
        when(mockScraper.scrapeIdeas(any(ScrapingConfig.class))).thenReturn(testIdeas);
        when(duplicateDetectionService.filterDuplicates(testIdeas)).thenReturn(filteredIdeas);
        when(mockScraper.validateScrapedData(any(ScrapedIdea.class))).thenReturn(true);

        scrapingOrchestrator.registerScraper("TestScraper", mockScraper);

        // When
        ScrapingResult result = scrapingOrchestrator.executeScraping(testSource);

        // Then
        assertTrue(result.isSuccessful());
        assertEquals(1, result.getIdeasScraped());
        assertEquals(filteredIdeas, result.getScrapedIdeas());
        assertTrue(result.getWarnings().contains("Removed 1 duplicate ideas"));
    }

    @Test
    void testExecuteScrapingWithValidationFiltering() {
        // Given
        when(scrapingExecutionRepository.save(any(ScrapingExecution.class))).thenReturn(testExecution);
        when(mockScraper.scrapeIdeas(any(ScrapingConfig.class))).thenReturn(testIdeas);
        when(duplicateDetectionService.filterDuplicates(testIdeas)).thenReturn(testIdeas);
        when(mockScraper.validateScrapedData(testIdeas.get(0))).thenReturn(true);
        when(mockScraper.validateScrapedData(testIdeas.get(1))).thenReturn(false); // Second idea is invalid

        scrapingOrchestrator.registerScraper("TestScraper", mockScraper);

        // When
        ScrapingResult result = scrapingOrchestrator.executeScraping(testSource);

        // Then
        assertTrue(result.isSuccessful());
        assertEquals(1, result.getIdeasScraped());
        assertEquals(1, result.getScrapedIdeas().size());
        assertTrue(result.getWarnings().contains("Removed 1 invalid ideas"));
    }

    @Test
    void testExecuteScrapingWithUnregisteredScraper() {
        // Given
        when(scrapingExecutionRepository.save(any(ScrapingExecution.class))).thenReturn(testExecution);

        // When
        ScrapingResult result = scrapingOrchestrator.executeScraping(testSource);

        // Then
        assertFalse(result.isSuccessful());
        assertEquals("test-source", result.getSourceName());
        assertEquals(0, result.getIdeasScraped());
        assertTrue(result.getErrorMessage().contains("No scraper registered for class: TestScraper"));

        verify(scrapingExecutionRepository, times(2)).save(any(ScrapingExecution.class));
    }

    @Test
    void testExecuteScrapingWithScrapingException() {
        // Given
        when(scrapingExecutionRepository.save(any(ScrapingExecution.class))).thenReturn(testExecution);
        when(mockScraper.scrapeIdeas(any(ScrapingConfig.class))).thenThrow(new RuntimeException("Scraping failed"));

        scrapingOrchestrator.registerScraper("TestScraper", mockScraper);

        // When
        ScrapingResult result = scrapingOrchestrator.executeScraping(testSource);

        // Then
        assertFalse(result.isSuccessful());
        assertEquals("test-source", result.getSourceName());
        assertEquals(0, result.getIdeasScraped());
        assertEquals("Scraping failed", result.getErrorMessage());

        verify(scrapingExecutionRepository, times(2)).save(any(ScrapingExecution.class));
        verify(mockScraper).scrapeIdeas(any(ScrapingConfig.class));
    }

    @Test
    void testConvertToConfig() {
        // This tests the private method indirectly through executeScraping
        when(scrapingExecutionRepository.save(any(ScrapingExecution.class))).thenReturn(testExecution);
        when(mockScraper.scrapeIdeas(any(ScrapingConfig.class))).thenAnswer(invocation -> {
            ScrapingConfig config = invocation.getArgument(0);
            
            // Verify the config was converted correctly
            assertEquals("test-source", config.getSourceName());
            assertEquals("https://example.com", config.getBaseUrl());
            assertEquals("TestScraper", config.getScraperClass());
            assertTrue(config.isEnabled());
            assertEquals("0 0 2 * * ?", config.getCronExpression());
            assertEquals(100, config.getRateLimitMs());
            assertEquals(10, config.getMaxPages());
            assertEquals(Map.of("key", "value"), config.getConfigJson());
            
            return testIdeas;
        });
        when(duplicateDetectionService.filterDuplicates(testIdeas)).thenReturn(testIdeas);
        when(mockScraper.validateScrapedData(any(ScrapedIdea.class))).thenReturn(true);

        scrapingOrchestrator.registerScraper("TestScraper", mockScraper);

        // When
        ScrapingResult result = scrapingOrchestrator.executeScraping(testSource);

        // Then
        assertTrue(result.isSuccessful());
    }

    @Test
    void testScrapingExecutionTracking() {
        // Given
        when(scrapingExecutionRepository.save(any(ScrapingExecution.class))).thenReturn(testExecution);
        when(mockScraper.scrapeIdeas(any(ScrapingConfig.class))).thenReturn(testIdeas);
        when(duplicateDetectionService.filterDuplicates(testIdeas)).thenReturn(testIdeas);
        when(mockScraper.validateScrapedData(any(ScrapedIdea.class))).thenReturn(true);

        scrapingOrchestrator.registerScraper("TestScraper", mockScraper);

        // When
        scrapingOrchestrator.executeScraping(testSource);

        // Then
        verify(scrapingExecutionRepository, times(2)).save(argThat(execution -> {
            if (execution.getCompletedAt() == null) {
                // First save - starting execution
                assertEquals(ScrapingExecution.ExecutionStatus.RUNNING, execution.getStatus());
                assertEquals(testSource, execution.getSource());
                assertNotNull(execution.getStartedAt());
                return true;
            } else {
                // Second save - completed execution
                assertEquals(ScrapingExecution.ExecutionStatus.COMPLETED, execution.getStatus());
                assertNotNull(execution.getCompletedAt());
                assertEquals(2, execution.getIdeasScraped());
                return true;
            }
        }));
    }

    @Test
    void testScrapingExecutionTrackingOnFailure() {
        // Given
        when(scrapingExecutionRepository.save(any(ScrapingExecution.class))).thenReturn(testExecution);
        when(mockScraper.scrapeIdeas(any(ScrapingConfig.class))).thenThrow(new RuntimeException("Test failure"));

        scrapingOrchestrator.registerScraper("TestScraper", mockScraper);

        // When
        scrapingOrchestrator.executeScraping(testSource);

        // Then
        verify(scrapingExecutionRepository, times(2)).save(argThat(execution -> {
            if (execution.getCompletedAt() == null) {
                // First save - starting execution
                assertEquals(ScrapingExecution.ExecutionStatus.RUNNING, execution.getStatus());
                return true;
            } else {
                // Second save - failed execution
                assertEquals(ScrapingExecution.ExecutionStatus.FAILED, execution.getStatus());
                assertEquals("Test failure", execution.getErrorMessage());
                assertEquals(0, execution.getIdeasScraped());
                return true;
            }
        }));
    }
}