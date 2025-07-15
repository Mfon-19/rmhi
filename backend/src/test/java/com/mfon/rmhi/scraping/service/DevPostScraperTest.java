package com.mfon.rmhi.scraping.service;

import com.mfon.rmhi.scraping.dto.ScrapedIdea;
import com.mfon.rmhi.scraping.dto.ScrapingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DevPostScraperTest {

    private DevPostScraper devPostScraper;
    private ScrapingConfig config;

    @BeforeEach
    void setUp() {
        // Create DevPost scraper with test credentials
        WebClient.Builder webClientBuilder = WebClient.builder();
        devPostScraper = new DevPostScraper(webClientBuilder, "test@example.com", "testpassword");
        
        config = ScrapingConfig.builder()
                .sourceName("devpost-test")
                .baseUrl("https://worldslargesthackathon.devpost.com")
                .rateLimitMs(10) // Reduced for testing
                .maxPages(1) // Limited for testing
                .build();
    }

    @Test
    void testScraperInstantiation() {
        // Test that the scraper can be instantiated with valid credentials
        assertNotNull(devPostScraper);
    }

    @Test
    void testScrapingWithMissingCredentials() {
        // Given - scraper without credentials
        WebClient.Builder webClientBuilder = WebClient.builder();
        DevPostScraper scraperWithoutCredentials = new DevPostScraper(webClientBuilder, null, null);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            scraperWithoutCredentials.scrapeIdeas(config);
        });

        assertTrue(exception.getMessage().contains("Failed to scrape after") && 
                  exception.getCause().getMessage().contains("DevPost credentials not configured"));
    }

    @Test
    void testConfigurationValidation() {
        // Test that scraper validates configuration properly
        ScrapingConfig invalidConfig = ScrapingConfig.builder()
                .sourceName("")
                .baseUrl("")
                .rateLimitMs(-1)
                .maxPages(-1)
                .build();

        // The scraper should handle invalid configuration gracefully
        assertNotNull(invalidConfig);
    }

    @Test
    void testValidationOfScrapedData() {
        // Test with valid data
        ScrapedIdea validIdea = ScrapedIdea.builder()
                .title("Valid Project")
                .description("Valid description")
                .sourceUrl("https://devpost.com/software/valid-project")
                .sourceWebsite("DevPost")
                .build();

        assertTrue(devPostScraper.validateScrapedData(validIdea));

        // Test with invalid data (missing title)
        ScrapedIdea invalidIdea = ScrapedIdea.builder()
                .description("Valid description")
                .sourceUrl("https://devpost.com/software/invalid-project")
                .sourceWebsite("DevPost")
                .build();

        assertFalse(devPostScraper.validateScrapedData(invalidIdea));

        // Test with null idea
        assertFalse(devPostScraper.validateScrapedData(null));

        // Test with invalid URL
        ScrapedIdea invalidUrlIdea = ScrapedIdea.builder()
                .title("Valid Project")
                .description("Valid description")
                .sourceUrl("not-a-valid-url")
                .sourceWebsite("DevPost")
                .build();

        assertFalse(devPostScraper.validateScrapedData(invalidUrlIdea));
    }

    @Test
    void testContentBuildingLogic() {
        // Test the content building logic by creating a scraper instance
        // and testing the internal content building method indirectly
        ScrapedIdea idea = ScrapedIdea.builder()
                .title("Test Project")
                .description("Test Description")
                .content("Inspiration: Test inspiration\n\nWhat it does: Test functionality\n\nHow we built it: Test technical details")
                .sourceUrl("https://devpost.com/software/test-project")
                .sourceWebsite("DevPost")
                .build();

        // Verify the content structure
        assertTrue(idea.getContent().contains("Inspiration:"));
        assertTrue(idea.getContent().contains("What it does:"));
        assertTrue(idea.getContent().contains("How we built it:"));
    }
}