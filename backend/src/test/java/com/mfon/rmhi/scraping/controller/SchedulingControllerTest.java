package com.mfon.rmhi.scraping.controller;

import com.mfon.rmhi.scraping.service.ScrapingScheduledService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulingControllerTest {

    @Mock
    private ScrapingScheduledService scheduledService;

    @InjectMocks
    private SchedulingController schedulingController;

    private ScrapingScheduledService.JobExecutionContext testJobContext;

    @BeforeEach
    void setUp() {
        testJobContext = new ScrapingScheduledService.JobExecutionContext(
                "test-job-1", "DAILY_COMPREHENSIVE", LocalDateTime.now());
    }

    @Test
    void testGetRunningJobs_Success() {
        // Given
        Map<String, ScrapingScheduledService.JobExecutionContext> runningJobs = 
                Map.of("test-job-1", testJobContext);
        when(scheduledService.getRunningJobs()).thenReturn(runningJobs);

        // When
        ResponseEntity<Map<String, ScrapingScheduledService.JobExecutionContext>> response = 
                schedulingController.getRunningJobs();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertTrue(response.getBody().containsKey("test-job-1"));
        verify(scheduledService).getRunningJobs();
    }

    @Test
    void testGetRunningJobs_EmptyResult() {
        // Given
        when(scheduledService.getRunningJobs()).thenReturn(Map.of());

        // When
        ResponseEntity<Map<String, ScrapingScheduledService.JobExecutionContext>> response = 
                schedulingController.getRunningJobs();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
        verify(scheduledService).getRunningJobs();
    }

    @Test
    void testGetRunningJobs_ServiceException() {
        // Given
        when(scheduledService.getRunningJobs()).thenThrow(new RuntimeException("Service error"));

        // When
        ResponseEntity<Map<String, ScrapingScheduledService.JobExecutionContext>> response = 
                schedulingController.getRunningJobs();

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(scheduledService).getRunningJobs();
    }

    @Test
    void testGetSchedulingStatus_Success() {
        // Given
        when(scheduledService.isSchedulingEnabled()).thenReturn(true);
        when(scheduledService.getRunningJobs()).thenReturn(Map.of("test-job-1", testJobContext));

        // When
        ResponseEntity<SchedulingController.SchedulingStatus> response = 
                schedulingController.getSchedulingStatus();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEnabled());
        assertEquals(1, response.getBody().getRunningJobsCount());
        verify(scheduledService).isSchedulingEnabled();
        verify(scheduledService).getRunningJobs();
    }

    @Test
    void testGetSchedulingStatus_DisabledScheduling() {
        // Given
        when(scheduledService.isSchedulingEnabled()).thenReturn(false);
        when(scheduledService.getRunningJobs()).thenReturn(Map.of());

        // When
        ResponseEntity<SchedulingController.SchedulingStatus> response = 
                schedulingController.getSchedulingStatus();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isEnabled());
        assertEquals(0, response.getBody().getRunningJobsCount());
    }

    @Test
    void testGetSchedulingStatus_ServiceException() {
        // Given
        when(scheduledService.isSchedulingEnabled()).thenThrow(new RuntimeException("Service error"));

        // When
        ResponseEntity<SchedulingController.SchedulingStatus> response = 
                schedulingController.getSchedulingStatus();

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(scheduledService).isSchedulingEnabled();
    }

    @Test
    void testTriggerDailyJob_Success() {
        // When
        ResponseEntity<String> response = schedulingController.triggerDailyJob();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Daily scraping job trigger requested"));
    }

    @Test
    void testSchedulingStatusBuilder() {
        // Given & When
        SchedulingController.SchedulingStatus status = SchedulingController.SchedulingStatus.builder()
                .enabled(true)
                .runningJobsCount(2)
                .build();

        // Then
        assertTrue(status.isEnabled());
        assertEquals(2, status.getRunningJobsCount());
    }

    @Test
    void testSchedulingStatusSettersAndGetters() {
        // Given
        SchedulingController.SchedulingStatus status = new SchedulingController.SchedulingStatus();

        // When
        status.setEnabled(true);
        status.setRunningJobsCount(3);

        // Then
        assertTrue(status.isEnabled());
        assertEquals(3, status.getRunningJobsCount());
    }
}