package com.mfon.rmhi.scraping.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mfon.rmhi.scraping.dto.DailyExecutionSummary;
import com.mfon.rmhi.scraping.dto.PerformanceMetrics;
import com.mfon.rmhi.scraping.dto.SystemHealthStatus;
import com.mfon.rmhi.scraping.service.MonitoringService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MonitoringController.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestSecurityConfig.class)
class MonitoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MonitoringService monitoringService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getSystemHealth_WithHealthySystem_ShouldReturn200() throws Exception {
        // Given
        SystemHealthStatus healthyStatus = SystemHealthStatus.builder()
                .timestamp(LocalDateTime.now())
                .overallStatus(SystemHealthStatus.HealthStatus.HEALTHY)
                .version("1.0.0")
                .uptimeMs(3600000L)
                .componentHealth(Map.of())
                .systemMetrics(SystemHealthStatus.SystemMetrics.builder()
                        .memoryUsagePercent(50.0)
                        .cpuUsagePercent(30.0)
                        .build())
                .recentErrors(List.of())
                .recentWarnings(List.of())
                .activeOperations(2)
                .configurationValid(true)
                .configurationIssues(List.of())
                .build();

        when(monitoringService.getSystemHealth()).thenReturn(healthyStatus);

        // When & Then
        mockMvc.perform(get("/api/monitoring/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.overallStatus").value("HEALTHY"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.activeOperations").value(2))
                .andExpect(jsonPath("$.configurationValid").value(true));

        verify(monitoringService).getSystemHealth();
    }

    @Test
    void getSystemHealth_WithCriticalSystem_ShouldReturn503() throws Exception {
        // Given
        SystemHealthStatus criticalStatus = SystemHealthStatus.builder()
                .timestamp(LocalDateTime.now())
                .overallStatus(SystemHealthStatus.HealthStatus.CRITICAL)
                .version("1.0.0")
                .uptimeMs(3600000L)
                .componentHealth(Map.of())
                .systemMetrics(SystemHealthStatus.SystemMetrics.builder().build())
                .recentErrors(List.of("Database connection failed"))
                .recentWarnings(List.of())
                .activeOperations(0)
                .configurationValid(false)
                .configurationIssues(List.of("Database configuration invalid"))
                .build();

        when(monitoringService.getSystemHealth()).thenReturn(criticalStatus);

        // When & Then
        mockMvc.perform(get("/api/monitoring/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.overallStatus").value("CRITICAL"))
                .andExpect(jsonPath("$.recentErrors[0]").value("Database connection failed"))
                .andExpect(jsonPath("$.configurationValid").value(false));

        verify(monitoringService).getSystemHealth();
    }

    @Test
    void getSystemHealth_WithException_ShouldReturn503() throws Exception {
        // Given
        when(monitoringService.getSystemHealth()).thenThrow(new RuntimeException("Service unavailable"));

        // When & Then
        mockMvc.perform(get("/api/monitoring/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.overallStatus").value("CRITICAL"))
                .andExpect(jsonPath("$.recentErrors[0]").value("Failed to retrieve system health: Service unavailable"));
    }

    @Test
    void getSimpleHealth_WithHealthySystem_ShouldReturn200() throws Exception {
        // Given
        SystemHealthStatus healthyStatus = SystemHealthStatus.builder()
                .timestamp(LocalDateTime.now())
                .overallStatus(SystemHealthStatus.HealthStatus.HEALTHY)
                .build();

        when(monitoringService.getSystemHealth()).thenReturn(healthyStatus);

        // When & Then
        mockMvc.perform(get("/api/monitoring/health/simple"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("HEALTHY"));

        verify(monitoringService).getSystemHealth();
    }

    @Test
    void getDailyReport_WithValidDate_ShouldReturnReport() throws Exception {
        // Given
        LocalDate testDate = LocalDate.of(2024, 1, 15);
        DailyExecutionSummary report = DailyExecutionSummary.builder()
                .reportDate(testDate)
                .generatedAt(LocalDateTime.now())
                .totalOperations(10)
                .successfulOperations(8)
                .failedOperations(2)
                .successRate(80.0)
                .totalIdeasScraped(100)
                .totalErrors(2)
                .errorsByType(Map.of("SCRAPING_ERROR", 2))
                .criticalErrors(List.of("Critical error 1"))
                .sourceStatistics(Map.of())
                .healthWarnings(List.of())
                .performanceAlerts(List.of())
                .build();

        when(monitoringService.generateDailyReport(testDate)).thenReturn(report);

        // When & Then
        mockMvc.perform(get("/api/monitoring/reports/daily")
                        .param("date", "2024-01-15"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.reportDate").value("2024-01-15"))
                .andExpect(jsonPath("$.totalOperations").value(10))
                .andExpect(jsonPath("$.successRate").value(80.0))
                .andExpect(jsonPath("$.totalIdeasScraped").value(100));

        verify(monitoringService).generateDailyReport(testDate);
    }

    @Test
    void getDailyReport_WithoutDate_ShouldUseYesterday() throws Exception {
        // Given
        LocalDate yesterday = LocalDate.now().minusDays(1);
        DailyExecutionSummary report = DailyExecutionSummary.builder()
                .reportDate(yesterday)
                .generatedAt(LocalDateTime.now())
                .totalOperations(5)
                .successfulOperations(5)
                .failedOperations(0)
                .successRate(100.0)
                .build();

        when(monitoringService.generateDailyReport(yesterday)).thenReturn(report);

        // When & Then
        mockMvc.perform(get("/api/monitoring/reports/daily"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.successRate").value(100.0));

        verify(monitoringService).generateDailyReport(yesterday);
    }

    @Test
    void getPerformanceMetrics_WithValidParameters_ShouldReturnMetrics() throws Exception {
        // Given
        String operationType = "SCRAPING";
        LocalDate startDate = LocalDate.of(2024, 1, 10);
        LocalDate endDate = LocalDate.of(2024, 1, 15);
        
        List<PerformanceMetrics> metrics = List.of(
                PerformanceMetrics.builder()
                        .operationType(operationType)
                        .operationId("test-1")
                        .timestamp(LocalDateTime.of(2024, 1, 12, 10, 0))
                        .durationMs(1000L)
                        .itemsProcessed(10)
                        .successful(true)
                        .build()
        );

        when(monitoringService.getPerformanceMetrics(operationType, startDate, endDate))
                .thenReturn(metrics);

        // When & Then
        mockMvc.perform(get("/api/monitoring/metrics/{operationType}", operationType)
                        .param("startDate", "2024-01-10")
                        .param("endDate", "2024-01-15"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].operationType").value(operationType))
                .andExpect(jsonPath("$[0].durationMs").value(1000))
                .andExpect(jsonPath("$[0].itemsProcessed").value(10));

        verify(monitoringService).getPerformanceMetrics(operationType, startDate, endDate);
    }

    @Test
    void getPerformanceMetrics_WithoutDates_ShouldUseDefaults() throws Exception {
        // Given
        String operationType = "SCRAPING";
        LocalDate expectedStart = LocalDate.now().minusDays(7);
        LocalDate expectedEnd = LocalDate.now();

        when(monitoringService.getPerformanceMetrics(eq(operationType), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/monitoring/metrics/{operationType}", operationType))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(monitoringService).getPerformanceMetrics(eq(operationType), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void getAvailableMetrics_ShouldReturnMetricTypes() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/monitoring/metrics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.SCRAPING").value("Web scraping operations"))
                .andExpect(jsonPath("$.AI_TRANSFORMATION").value("AI transformation operations"))
                .andExpect(jsonPath("$.STAGING").value("Staging operations"))
                .andExpect(jsonPath("$.MIGRATION").value("Migration operations"))
                .andExpect(jsonPath("$.ORCHESTRATION").value("Overall orchestration operations"));
    }

    @Test
    void cleanupOldData_WithValidRetention_ShouldReturnSuccess() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/monitoring/cleanup")
                        .param("retentionDays", "30"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Cleanup completed for data older than 30 days"));

        verify(monitoringService).cleanupOldData(30);
    }

    @Test
    void cleanupOldData_WithDefaultRetention_ShouldUseDefault() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/monitoring/cleanup"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Cleanup completed for data older than 30 days"));

        verify(monitoringService).cleanupOldData(30);
    }

    @Test
    void getMonitoringStats_ShouldReturnSummaryStats() throws Exception {
        // Given
        SystemHealthStatus healthStatus = SystemHealthStatus.builder()
                .overallStatus(SystemHealthStatus.HealthStatus.HEALTHY)
                .activeOperations(3)
                .uptimeMs(7200000L) // 2 hours
                .systemMetrics(SystemHealthStatus.SystemMetrics.builder()
                        .memoryUsagePercent(65.5)
                        .build())
                .build();

        DailyExecutionSummary todayReport = DailyExecutionSummary.builder()
                .totalOperations(15)
                .successRate(90.0)
                .totalIdeasScraped(150)
                .build();

        when(monitoringService.getSystemHealth()).thenReturn(healthStatus);
        when(monitoringService.generateDailyReport(LocalDate.now())).thenReturn(todayReport);

        // When & Then
        mockMvc.perform(get("/api/monitoring/stats"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.system_health").value("HEALTHY"))
                .andExpect(jsonPath("$.active_operations").value(3))
                .andExpect(jsonPath("$.uptime_hours").value(2))
                .andExpect(jsonPath("$.today_operations").value(15))
                .andExpect(jsonPath("$.today_success_rate").value(90.0))
                .andExpect(jsonPath("$.today_ideas_scraped").value(150))
                .andExpect(jsonPath("$.memory_usage_percent").value(65.5));

        verify(monitoringService).getSystemHealth();
        verify(monitoringService).generateDailyReport(LocalDate.now());
    }
}