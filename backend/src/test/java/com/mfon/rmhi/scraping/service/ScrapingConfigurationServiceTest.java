package com.mfon.rmhi.scraping.service;

import com.mfon.rmhi.scraping.entity.ScrapingSource;
import com.mfon.rmhi.scraping.repository.ScrapingSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScrapingConfigurationServiceTest {

    @Mock
    private ScrapingSourceRepository scrapingSourceRepository;

    @InjectMocks
    private ScrapingConfigurationService configurationService;

    private ScrapingSource validSource;
    private ScrapingSource invalidSource;

    @BeforeEach
    void setUp() {
        validSource = new ScrapingSource();
        validSource.setId(1);
        validSource.setName("DevPost");
        validSource.setBaseUrl("https://devpost.com");
        validSource.setScraperClass("com.mfon.rmhi.scraping.service.DevPostScraper");
        validSource.setEnabled(true);
        validSource.setCronExpression("0 0 2 * * ?");
        validSource.setRateLimitMs(1000);
        validSource.setMaxPages(100);

        invalidSource = new ScrapingSource();
        // Will be configured per test
    }

    @Test
    void getAllSources_ShouldReturnAllSources() {
        // Given
        List<ScrapingSource> expectedSources = Arrays.asList(validSource);
        when(scrapingSourceRepository.findAll()).thenReturn(expectedSources);

        // When
        List<ScrapingSource> actualSources = configurationService.getAllSources();

        // Then
        assertEquals(expectedSources, actualSources);
        verify(scrapingSourceRepository).findAll();
    }

    @Test
    void getEnabledSources_ShouldReturnOnlyEnabledSources() {
        // Given
        List<ScrapingSource> expectedSources = Arrays.asList(validSource);
        when(scrapingSourceRepository.findByEnabledTrue()).thenReturn(expectedSources);

        // When
        List<ScrapingSource> actualSources = configurationService.getEnabledSources();

        // Then
        assertEquals(expectedSources, actualSources);
        verify(scrapingSourceRepository).findByEnabledTrue();
    }

    @Test
    void getSourceById_WhenExists_ShouldReturnSource() {
        // Given
        when(scrapingSourceRepository.findById(1)).thenReturn(Optional.of(validSource));

        // When
        Optional<ScrapingSource> result = configurationService.getSourceById(1);

        // Then
        assertTrue(result.isPresent());
        assertEquals(validSource, result.get());
        verify(scrapingSourceRepository).findById(1);
    }

    @Test
    void getSourceById_WhenNotExists_ShouldReturnEmpty() {
        // Given
        when(scrapingSourceRepository.findById(999)).thenReturn(Optional.empty());

        // When
        Optional<ScrapingSource> result = configurationService.getSourceById(999);

        // Then
        assertFalse(result.isPresent());
        verify(scrapingSourceRepository).findById(999);
    }

    @Test
    void getSourceByName_WhenExists_ShouldReturnSource() {
        // Given
        when(scrapingSourceRepository.findByName("DevPost")).thenReturn(Optional.of(validSource));

        // When
        Optional<ScrapingSource> result = configurationService.getSourceByName("DevPost");

        // Then
        assertTrue(result.isPresent());
        assertEquals(validSource, result.get());
        verify(scrapingSourceRepository).findByName("DevPost");
    }

    @Test
    void createSource_WithValidSource_ShouldCreateSuccessfully() {
        // Given
        when(scrapingSourceRepository.existsByName(validSource.getName())).thenReturn(false);
        when(scrapingSourceRepository.save(any(ScrapingSource.class))).thenReturn(validSource);

        // When
        ScrapingSource result = configurationService.createSource(validSource);

        // Then
        assertEquals(validSource, result);
        verify(scrapingSourceRepository).existsByName(validSource.getName());
        verify(scrapingSourceRepository).save(validSource);
    }

    @Test
    void createSource_WithDuplicateName_ShouldThrowException() {
        // Given
        when(scrapingSourceRepository.existsByName(validSource.getName())).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> configurationService.createSource(validSource)
        );
        assertEquals("Scraping source with name 'DevPost' already exists", exception.getMessage());
        verify(scrapingSourceRepository).existsByName(validSource.getName());
        verify(scrapingSourceRepository, never()).save(any());
    }

    @Test
    void updateSource_WithValidSource_ShouldUpdateSuccessfully() {
        // Given
        ScrapingSource updatedSource = new ScrapingSource();
        updatedSource.setName("Updated DevPost");
        updatedSource.setBaseUrl("https://devpost.com/updated");
        updatedSource.setScraperClass("com.mfon.rmhi.scraping.service.UpdatedDevPostScraper");
        updatedSource.setEnabled(false);
        updatedSource.setCronExpression("0 0 3 * * ?");
        updatedSource.setRateLimitMs(2000);
        updatedSource.setMaxPages(200);

        when(scrapingSourceRepository.findById(1)).thenReturn(Optional.of(validSource));
        when(scrapingSourceRepository.existsByName("Updated DevPost")).thenReturn(false);
        when(scrapingSourceRepository.save(any(ScrapingSource.class))).thenReturn(validSource);

        // When
        ScrapingSource result = configurationService.updateSource(1, updatedSource);

        // Then
        assertNotNull(result);
        verify(scrapingSourceRepository).findById(1);
        verify(scrapingSourceRepository).save(validSource);
    }

    @Test
    void updateSource_WithNonExistentId_ShouldThrowException() {
        // Given
        when(scrapingSourceRepository.findById(999)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> configurationService.updateSource(999, validSource)
        );
        assertEquals("Scraping source with ID 999 not found", exception.getMessage());
        verify(scrapingSourceRepository).findById(999);
        verify(scrapingSourceRepository, never()).save(any());
    }

    @Test
    void deleteSource_WithExistingId_ShouldDeleteSuccessfully() {
        // Given
        when(scrapingSourceRepository.existsById(1)).thenReturn(true);

        // When
        configurationService.deleteSource(1);

        // Then
        verify(scrapingSourceRepository).existsById(1);
        verify(scrapingSourceRepository).deleteById(1);
    }

    @Test
    void deleteSource_WithNonExistentId_ShouldThrowException() {
        // Given
        when(scrapingSourceRepository.existsById(999)).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> configurationService.deleteSource(999)
        );
        assertEquals("Scraping source with ID 999 not found", exception.getMessage());
        verify(scrapingSourceRepository).existsById(999);
        verify(scrapingSourceRepository, never()).deleteById(any());
    }

    @Test
    void toggleSourceStatus_WithExistingId_ShouldToggleSuccessfully() {
        // Given
        when(scrapingSourceRepository.findById(1)).thenReturn(Optional.of(validSource));
        when(scrapingSourceRepository.save(any(ScrapingSource.class))).thenReturn(validSource);

        // When
        ScrapingSource result = configurationService.toggleSourceStatus(1, false);

        // Then
        assertNotNull(result);
        verify(scrapingSourceRepository).findById(1);
        verify(scrapingSourceRepository).save(validSource);
    }

    @Test
    void validateScrapingSource_WithValidSource_ShouldPass() {
        // When & Then
        assertDoesNotThrow(() -> configurationService.validateScrapingSource(validSource));
    }

    @Test
    void validateScrapingSource_WithNullName_ShouldThrowException() {
        // Given
        invalidSource.setName(null);
        invalidSource.setBaseUrl("https://example.com");
        invalidSource.setScraperClass("com.example.Scraper");

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> configurationService.validateScrapingSource(invalidSource)
        );
        assertEquals("Source name cannot be null or empty", exception.getMessage());
    }

    @Test
    void validateScrapingSource_WithEmptyName_ShouldThrowException() {
        // Given
        invalidSource.setName("   ");
        invalidSource.setBaseUrl("https://example.com");
        invalidSource.setScraperClass("com.example.Scraper");

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> configurationService.validateScrapingSource(invalidSource)
        );
        assertEquals("Source name cannot be null or empty", exception.getMessage());
    }

    @Test
    void validateScrapingSource_WithNullBaseUrl_ShouldThrowException() {
        // Given
        invalidSource.setName("Test");
        invalidSource.setBaseUrl(null);
        invalidSource.setScraperClass("com.example.Scraper");

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> configurationService.validateScrapingSource(invalidSource)
        );
        assertEquals("Base URL cannot be null or empty", exception.getMessage());
    }

    @Test
    void validateScrapingSource_WithInvalidUrl_ShouldThrowException() {
        // Given
        invalidSource.setName("Test");
        invalidSource.setBaseUrl("invalid-url");
        invalidSource.setScraperClass("com.example.Scraper");

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> configurationService.validateScrapingSource(invalidSource)
        );
        assertEquals("Invalid URL format: invalid-url", exception.getMessage());
    }

    @Test
    void validateScrapingSource_WithNullScraperClass_ShouldThrowException() {
        // Given
        invalidSource.setName("Test");
        invalidSource.setBaseUrl("https://example.com");
        invalidSource.setScraperClass(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> configurationService.validateScrapingSource(invalidSource)
        );
        assertEquals("Scraper class cannot be null or empty", exception.getMessage());
    }

    @Test
    void validateScrapingSource_WithInvalidScraperClass_ShouldThrowException() {
        // Given
        invalidSource.setName("Test");
        invalidSource.setBaseUrl("https://example.com");
        invalidSource.setScraperClass("InvalidClass");

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> configurationService.validateScrapingSource(invalidSource)
        );
        assertEquals("Scraper class must be a fully qualified class name", exception.getMessage());
    }

    @Test
    void validateScrapingSource_WithNegativeRateLimit_ShouldThrowException() {
        // Given
        invalidSource.setName("Test");
        invalidSource.setBaseUrl("https://example.com");
        invalidSource.setScraperClass("com.example.Scraper");
        invalidSource.setRateLimitMs(-100);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> configurationService.validateScrapingSource(invalidSource)
        );
        assertEquals("Rate limit must be non-negative", exception.getMessage());
    }

    @Test
    void validateScrapingSource_WithZeroMaxPages_ShouldThrowException() {
        // Given
        invalidSource.setName("Test");
        invalidSource.setBaseUrl("https://example.com");
        invalidSource.setScraperClass("com.example.Scraper");
        invalidSource.setMaxPages(0);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> configurationService.validateScrapingSource(invalidSource)
        );
        assertEquals("Max pages must be positive", exception.getMessage());
    }

    @Test
    void getConfigurationSummary_ShouldReturnCorrectSummary() {
        // Given
        ScrapingSource disabledSource = new ScrapingSource();
        disabledSource.setEnabled(false);
        
        List<ScrapingSource> allSources = Arrays.asList(validSource, disabledSource);
        when(scrapingSourceRepository.findAll()).thenReturn(allSources);

        // When
        Map<String, Object> summary = configurationService.getConfigurationSummary();

        // Then
        assertEquals(2, summary.get("totalSources"));
        assertEquals(1L, summary.get("enabledSources"));
        assertEquals(1L, summary.get("disabledSources"));
        assertEquals(allSources, summary.get("sources"));
        verify(scrapingSourceRepository).findAll();
    }

    @Test
    void reloadConfiguration_ShouldCompleteSuccessfully() {
        // When & Then
        assertDoesNotThrow(() -> configurationService.reloadConfiguration());
    }
}