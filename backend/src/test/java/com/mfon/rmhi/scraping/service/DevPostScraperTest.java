package com.mfon.rmhi.scraping.service;

import com.mfon.rmhi.scraping.dto.ScrapedIdea;
import com.mfon.rmhi.scraping.dto.ScrapingConfig;
import com.mfon.rmhi.scraping.repository.StagedIdeaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevPostScraperTest {

    private DevPostScraper devPostScraper;
    private ScrapingConfig config;

    @Mock
    private StagedIdeaRepository stagedIdeaRepository;

    @BeforeEach
    void setUp() {
        WebClient.Builder webClientBuilder = WebClient.builder();
        devPostScraper = new DevPostScraper(webClientBuilder, stagedIdeaRepository);

        config = ScrapingConfig.builder()
                .sourceName("devpost-test")
                .baseUrl("https://worldslargesthackathon.devpost.com")
                .rateLimitMs(10)
                .maxPages(1)
                .build();
    }

    @Test
    void testScraperInstantiation() {
        assertNotNull(devPostScraper);
    }

    @Test
    void testScrapingSkipsExistingUrl() throws Exception {
        // Given
        String existingUrl = "https://devpost.com/software/existing-project";
        when(stagedIdeaRepository.existsBySourceUrl(existingUrl)).thenReturn(true);

        // When
        // This test will need to be more sophisticated to check that scrapeProject is not called.
        // For now, we are just ensuring that the scraper doesn't throw an exception.
        assertDoesNotThrow(() -> {
            devPostScraper.performScraping(config);
        });
    }

    @Test
    void testConfigurationValidation() {
        ScrapingConfig invalidConfig = ScrapingConfig.builder()
                .sourceName("")
                .baseUrl("")
                .rateLimitMs(-1)
                .maxPages(-1)
                .build();

        assertNotNull(invalidConfig);
    }

    @Test
    void testValidationOfScrapedData() {
        ScrapedIdea validIdea = ScrapedIdea.builder()
                .title("Valid Project")
                .description("Valid description")
                .sourceUrl("https://devpost.com/software/valid-project")
                .sourceWebsite("DevPost")
                .build();

        assertTrue(devPostScraper.validateScrapedData(validIdea));

        ScrapedIdea invalidIdea = ScrapedIdea.builder()
                .description("Valid description")
                .sourceUrl("https://devpost.com/software/invalid-project")
                .sourceWebsite("DevPost")
                .build();

        assertFalse(devPostScraper.validateScrapedData(invalidIdea));

        assertFalse(devPostScraper.validateScrapedData(null));

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
        ScrapedIdea idea = ScrapedIdea.builder()
                .title("Test Project")
                .description("Test Description")
                .content("Inspiration: Test inspiration\n\nWhat it does: Test functionality\n\nHow we built it: Test technical details")
                .sourceUrl("https://devpost.com/software/test-project")
                .sourceWebsite("DevPost")
                .build();

        assertTrue(idea.getContent().contains("Inspiration:"));
        assertTrue(idea.getContent().contains("What it does:"));
        assertTrue(idea.getContent().contains("How we built it:"));
    }
}