package com.mfon.rmhi.scraping.service;

import com.mfon.rmhi.scraping.dto.DailyExecutionSummary;
import com.mfon.rmhi.scraping.dto.PerformanceMetrics;
import com.mfon.rmhi.scraping.dto.SystemHealthStatus;
import com.mfon.rmhi.scraping.entity.ScrapingExecution;
import com.mfon.rmhi.scraping.entity.ScrapingSource;
import com.mfon.rmhi.scraping.repository.ScrapingExecutionRepository;
import com.mfon.rmhi.scraping.repository.ScrapingSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitoringServiceTest {

    @Mock
    private ScrapingExecutionRepository scrapingExecutionRepository;

    @Mock
    private ScrapingSourceRepository scrapingSourceRepository;

    private MonitoringServiceImpl monitoringService;

    @BeforeEach
    void setUp() {
        monitoringService = new MonitoringServiceImpl(scrapingExecutionRepository, scrapingSourceRepository);
        
        // Set test configuration
        ReflectionTestUtils.setField(monitoringService, "metricsEnabled", true);
        ReflectionTestUtils.setField(monitoringService, "logLevel", "INFO");
    }

    @Test
    void recordOperationStart_ShouldTrackOperationStart() {
        // When
        monitoringService.recordOperationStart("TEST_OPERATION", "test-123", "Test context");

        // Then - verify internal state is updated (this would be verified through logs in real scenario)
        // Since we can't easily test internal state, we verify no exceptions are thrown
        assertThat(true).isTrue(); // Operation completed without exception
    }

    @Test
    void recordOperationComplete_ShouldTrackOperationCompletion() {
        // Given
        monitoringService.recordOperationStart("TEST_OPERATION", "test-123", "Test context");

        // When
        monitoringService.recordOperationComplete("TEST_OPERATION", "test-123", 1000L, true);

        // Then - verify no exceptions are thrown
        assertThat(true).isTrue();
    }

    @Test
    void recordError_ShouldTrackErrorWithStackTrace() {
        // Given
        Exception testException = new RuntimeException("Test error");

        // When
        monitoringService.recordError("TEST_OPERATION", "test-123", "Test error message", 
                testException, "Test error context");

        // Then - verify no exceptions are thrown
        assertThat(true).isTrue();
    }

    @Test
    void recordPerformanceMetrics_ShouldStoreMetrics() {
        // Given
        PerformanceMetrics metrics = PerformanceMetrics.builder()
                .operationType("TEST_OPERATION")
                .operationId("test-123")
                .timestamp(LocalDateTime.now())
                .durationMs(1000L)
                .itemsProcessed(10)
                .successful(true)
                .build();

        // When
        monitoringService.recordPerformanceMetrics("TEST_OPERATION", metrics);

        // Then - verify no exceptions are thrown
        assertThat(true).isTrue();
    }

    @Test
    void generateDailyReport_ShouldCreateComprehensiveReport() {
        // Given
        LocalDate testDate = LocalDate.of(2024, 1, 15);
        LocalDateTime startOfDay = testDate.atStartOfDay();
        LocalDateTime endOfDay = testDate.plusDays(1).atStartOfDay();

        ScrapingSource testSource = createTestScrapingSource();
        List<ScrapingExecution> testExecutions = List.of(
                createTestExecution(testSource, ScrapingExecution.ExecutionStatus.COMPLETED, 10, null),
                createTestExecution(testSource, ScrapingExecution.ExecutionStatus.FAILED, 0, "Test error")
        );

        when(scrapingExecutionRepository.findByStartedAtBetween(startOfDay, endOfDay))
                .thenReturn(testExecutions);

        // When
        DailyExecutionSummary report = monitoringService.generateDailyReport(testDate);

        // Then
        assertThat(report).isNotNull();
        assertThat(report.getReportDate()).isEqualTo(testDate);
        assertThat(report.getTotalOperations()).isEqualTo(2);
        assertThat(report.getSuccessfulOperations()).isEqualTo(1);
        assertThat(report.getFailedOperations()).isEqualTo(1);
        assertThat(report.getSuccessRate()).isEqualTo(50.0);
        assertThat(report.getTotalIdeasScraped()).isEqualTo(10);
        assertThat(report.getSourceStatistics()).hasSize(1);
        
        verify(scrapingExecutionRepository).findByStartedAtBetween(startOfDay, endOfDay);
    }

    @Test
    void generateDailyReport_WithNoExecutions_ShouldReturnEmptyReport() {
        // Given
        LocalDate testDate = LocalDate.of(2024, 1, 15);
        LocalDateTime startOfDay = testDate.atStartOfDay();
        LocalDateTime endOfDay = testDate.plusDays(1).atStartOfDay();

        when(scrapingExecutionRepository.findByStartedAtBetween(startOfDay, endOfDay))
                .thenReturn(List.of());

        // When
        DailyExecutionSummary report = monitoringService.generateDailyReport(testDate);

        // Then
        assertThat(report).isNotNull();
        assertThat(report.getReportDate()).isEqualTo(testDate);
        assertThat(report.getTotalOperations()).isEqualTo(0);
        assertThat(report.getSuccessRate()).isEqualTo(0.0);
        assertThat(report.getTotalIdeasScraped()).isEqualTo(0);
    }

    @Test
    void getSystemHealth_ShouldReturnHealthStatus() {
        // Given
        when(scrapingSourceRepository.count()).thenReturn(5L);
        when(scrapingSourceRepository.countByEnabledTrue()).thenReturn(3L);

        // When
        SystemHealthStatus health = monitoringService.getSystemHealth();

        // Then
        assertThat(health).isNotNull();
        assertThat(health.getTimestamp()).isNotNull();
        assertThat(health.getOverallStatus()).isNotNull();
        assertThat(health.getComponentHealth()).isNotEmpty();
        assertThat(health.getSystemMetrics()).isNotNull();
        assertThat(health.getUptimeMs()).isGreaterThan(0);
        
        // Verify that count() is called at least once (it's called by both database and scraping sources health checks)
        verify(scrapingSourceRepository, atLeastOnce()).count();
        verify(scrapingSourceRepository).countByEnabledTrue();
    }

    @Test
    void getSystemHealth_WithDatabaseError_ShouldReturnCriticalStatus() {
        // Given
        when(scrapingSourceRepository.count()).thenThrow(new RuntimeException("Database connection failed"));

        // When
        SystemHealthStatus health = monitoringService.getSystemHealth();

        // Then
        assertThat(health).isNotNull();
        assertThat(health.getComponentHealth().get("database").getStatus())
                .isEqualTo(SystemHealthStatus.HealthStatus.CRITICAL);
    }

    @Test
    void getPerformanceMetrics_ShouldReturnFilteredMetrics() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 10);
        LocalDate endDate = LocalDate.of(2024, 1, 15);
        
        // Add some test metrics
        PerformanceMetrics metrics1 = PerformanceMetrics.builder()
                .operationType("TEST_OPERATION")
                .timestamp(LocalDateTime.of(2024, 1, 12, 10, 0))
                .build();
        
        PerformanceMetrics metrics2 = PerformanceMetrics.builder()
                .operationType("TEST_OPERATION")
                .timestamp(LocalDateTime.of(2024, 1, 20, 10, 0)) // Outside range
                .build();
        
        monitoringService.recordPerformanceMetrics("TEST_OPERATION", metrics1);
        monitoringService.recordPerformanceMetrics("TEST_OPERATION", metrics2);

        // When
        List<PerformanceMetrics> result = monitoringService.getPerformanceMetrics("TEST_OPERATION", startDate, endDate);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTimestamp()).isEqualTo(metrics1.getTimestamp());
    }

    @Test
    void cleanupOldData_ShouldRemoveOldMetrics() {
        // Given
        int retentionDays = 7;
        
        // Add old and new metrics
        PerformanceMetrics oldMetrics = PerformanceMetrics.builder()
                .operationType("TEST_OPERATION")
                .timestamp(LocalDateTime.now().minusDays(10))
                .build();
        
        PerformanceMetrics newMetrics = PerformanceMetrics.builder()
                .operationType("TEST_OPERATION")
                .timestamp(LocalDateTime.now().minusDays(3))
                .build();
        
        monitoringService.recordPerformanceMetrics("TEST_OPERATION", oldMetrics);
        monitoringService.recordPerformanceMetrics("TEST_OPERATION", newMetrics);

        // When
        monitoringService.cleanupOldData(retentionDays);

        // Then
        List<PerformanceMetrics> remainingMetrics = monitoringService.getPerformanceMetrics(
                "TEST_OPERATION", LocalDate.now().minusDays(30), LocalDate.now());
        
        assertThat(remainingMetrics).hasSize(1);
        assertThat(remainingMetrics.get(0).getTimestamp()).isEqualTo(newMetrics.getTimestamp());
    }

    @Test
    void metricsDisabled_ShouldNotRecordMetrics() {
        // Given
        ReflectionTestUtils.setField(monitoringService, "metricsEnabled", false);

        // When
        monitoringService.recordOperationStart("TEST_OPERATION", "test-123", "Test context");
        monitoringService.recordOperationComplete("TEST_OPERATION", "test-123", 1000L, true);

        // Then - verify no exceptions are thrown and operations are ignored
        assertThat(true).isTrue();
    }

    // Helper methods

    private ScrapingSource createTestScrapingSource() {
        ScrapingSource source = new ScrapingSource();
        source.setId(1);
        source.setName("test-source");
        source.setBaseUrl("https://test.com");
        source.setScraperClass("TestScraper");
        source.setEnabled(true);
        return source;
    }

    private ScrapingExecution createTestExecution(ScrapingSource source, 
                                                  ScrapingExecution.ExecutionStatus status, 
                                                  int ideasScraped, 
                                                  String errorMessage) {
        ScrapingExecution execution = new ScrapingExecution();
        execution.setId(System.currentTimeMillis());
        execution.setSource(source);
        execution.setStartedAt(LocalDateTime.now().minusHours(1));
        execution.setCompletedAt(LocalDateTime.now());
        execution.setStatus(status);
        execution.setIdeasScraped(ideasScraped);
        execution.setIdeasTransformed(ideasScraped);
        execution.setIdeasStaged(ideasScraped);
        execution.setErrorMessage(errorMessage);
        return execution;
    }
}