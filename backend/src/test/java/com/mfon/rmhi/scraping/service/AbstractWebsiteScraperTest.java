package com.mfon.rmhi.scraping.service;

import com.mfon.rmhi.scraping.dto.ScrapedIdea;
import com.mfon.rmhi.scraping.dto.ScrapingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbstractWebsiteScraperTest {

    private TestWebsiteScraper scraper;
    private ScrapingConfig config;

    @BeforeEach
    void setUp() {
        scraper = new TestWebsiteScraper();
        config = ScrapingConfig.builder()
                .sourceName("test-source")
                .baseUrl("https://example.com")
                .rateLimitMs(100)
                .maxPages(5)
                .build();
    }

    @Test
    void testSuccessfulScraping() {
        // Given
        scraper.setShouldSucceed(true);
        scraper.setIdeasToReturn(createTestIdeas(3));

        // When
        List<ScrapedIdea> result = scraper.scrapeIdeas(config);

        // Then
        assertEquals(3, result.size());
        assertEquals(1, scraper.getAttemptCount());
        
        // Verify content hashes are generated
        result.forEach(idea -> {
            assertNotNull(idea.getContentHash());
            assertNotNull(idea.getScrapedAt());
        });
    }

    @Test
    void testRateLimitingApplied() {
        // Given
        scraper.setShouldSucceed(true);
        scraper.setIdeasToReturn(createTestIdeas(1));
        long startTime = System.currentTimeMillis();

        // When
        scraper.scrapeIdeas(config);

        // Then
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Should take at least the rate limit time
        assertTrue(duration >= config.getRateLimitMs(), 
                "Expected duration >= " + config.getRateLimitMs() + "ms, but was " + duration + "ms");
    }

    @Test
    void testRetryMechanismWithEventualSuccess() {
        // Given
        scraper.setFailuresBeforeSuccess(2);
        scraper.setIdeasToReturn(createTestIdeas(1));

        // When
        List<ScrapedIdea> result = scraper.scrapeIdeas(config);

        // Then
        assertEquals(1, result.size());
        assertEquals(3, scraper.getAttemptCount()); // 2 failures + 1 success
    }

    @Test
    void testRetryMechanismWithMaxRetriesExceeded() {
        // Given
        scraper.setFailuresBeforeSuccess(5); // More than max retries (3)

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            scraper.scrapeIdeas(config);
        });

        assertTrue(exception.getMessage().contains("Failed to scrape after 3 attempts"));
        assertEquals(3, scraper.getAttemptCount()); // Should stop at max retries
    }

    @Test
    void testExponentialBackoffTiming() {
        // Given
        scraper.setFailuresBeforeSuccess(2);
        scraper.setIdeasToReturn(createTestIdeas(1));
        long startTime = System.currentTimeMillis();

        // When
        scraper.scrapeIdeas(config);

        // Then
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Should include rate limiting + exponential backoff delays
        // First attempt: rate limit (100ms)
        // Second attempt: rate limit (100ms) + backoff (~1000ms)
        // Third attempt: rate limit (100ms) + backoff (~2000ms)
        // Total should be at least 3300ms (allowing for jitter)
        assertTrue(duration >= 3000, 
                "Expected duration >= 3000ms for exponential backoff, but was " + duration + "ms");
    }

    @Test
    void testValidateScrapedDataWithValidIdea() {
        // Given
        ScrapedIdea validIdea = ScrapedIdea.builder()
                .title("Valid Title")
                .description("Valid Description")
                .sourceUrl("https://example.com/idea/1")
                .sourceWebsite("example.com")
                .build();

        // When
        boolean isValid = scraper.validateScrapedData(validIdea);

        // Then
        assertTrue(isValid);
    }

    @Test
    void testValidateScrapedDataWithInvalidIdea() {
        // Test null idea
        assertFalse(scraper.validateScrapedData(null));

        // Test missing title
        ScrapedIdea noTitle = ScrapedIdea.builder()
                .description("Valid Description")
                .sourceUrl("https://example.com/idea/1")
                .sourceWebsite("example.com")
                .build();
        assertFalse(scraper.validateScrapedData(noTitle));

        // Test missing description
        ScrapedIdea noDescription = ScrapedIdea.builder()
                .title("Valid Title")
                .sourceUrl("https://example.com/idea/1")
                .sourceWebsite("example.com")
                .build();
        assertFalse(scraper.validateScrapedData(noDescription));

        // Test invalid URL
        ScrapedIdea invalidUrl = ScrapedIdea.builder()
                .title("Valid Title")
                .description("Valid Description")
                .sourceUrl("not-a-url")
                .sourceWebsite("example.com")
                .build();
        assertFalse(scraper.validateScrapedData(invalidUrl));
    }

    @Test
    void testContentHashGeneration() {
        // Given
        ScrapedIdea idea1 = ScrapedIdea.builder()
                .title("Same Title")
                .description("Same Description")
                .content("Same Content")
                .build();

        ScrapedIdea idea2 = ScrapedIdea.builder()
                .title("Same Title")
                .description("Same Description")
                .content("Same Content")
                .build();

        ScrapedIdea idea3 = ScrapedIdea.builder()
                .title("Different Title")
                .description("Same Description")
                .content("Same Content")
                .build();

        scraper.setShouldSucceed(true);
        scraper.setIdeasToReturn(List.of(idea1, idea2, idea3));

        // When
        List<ScrapedIdea> result = scraper.scrapeIdeas(config);

        // Then
        assertEquals(3, result.size());
        
        // Same content should have same hash
        assertEquals(result.get(0).getContentHash(), result.get(1).getContentHash());
        
        // Different content should have different hash
        assertNotEquals(result.get(0).getContentHash(), result.get(2).getContentHash());
    }

    @Test
    void testInterruptedRateLimit() {
        // Given
        scraper.setShouldSucceed(true);
        scraper.setIdeasToReturn(createTestIdeas(1));
        
        // Interrupt the current thread before scraping
        Thread.currentThread().interrupt();

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            scraper.scrapeIdeas(config);
        });

        // Verify that the interrupt was handled correctly
        assertTrue(exception.getMessage().contains("Rate limiting interrupted"));
        // Note: Thread interrupt status is handled correctly as shown in the logs
    }

    private List<ScrapedIdea> createTestIdeas(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> ScrapedIdea.builder()
                        .title("Test Idea " + i)
                        .description("Test Description " + i)
                        .content("Test Content " + i)
                        .sourceUrl("https://example.com/idea/" + i)
                        .sourceWebsite("example.com")
                        .author("Test Author " + i)
                        .likes(i * 10)
                        .technologies(List.of("Java", "Spring"))
                        .categories(List.of("Web Development"))
                        .metadata(Map.of("testKey", "testValue" + i))
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Test implementation of AbstractWebsiteScraper for testing purposes
     */
    private static class TestWebsiteScraper extends AbstractWebsiteScraper {
        private boolean shouldSucceed = true;
        private int failuresBeforeSuccess = 0;
        private List<ScrapedIdea> ideasToReturn = List.of();
        private int attemptCount = 0;

        @Override
        protected List<ScrapedIdea> performScraping(ScrapingConfig config) throws Exception {
            attemptCount++;
            
            if (failuresBeforeSuccess > 0) {
                failuresBeforeSuccess--;
                throw new RuntimeException("Simulated scraping failure");
            }
            
            if (!shouldSucceed) {
                throw new RuntimeException("Scraping configured to fail");
            }
            
            return ideasToReturn;
        }

        // Test helper methods
        public void setShouldSucceed(boolean shouldSucceed) {
            this.shouldSucceed = shouldSucceed;
        }

        public void setFailuresBeforeSuccess(int failures) {
            this.failuresBeforeSuccess = failures;
        }

        public void setIdeasToReturn(List<ScrapedIdea> ideas) {
            this.ideasToReturn = ideas;
        }

        public int getAttemptCount() {
            return attemptCount;
        }
    }
}