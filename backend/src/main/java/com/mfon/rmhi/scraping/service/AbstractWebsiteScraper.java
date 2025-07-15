package com.mfon.rmhi.scraping.service;

import com.mfon.rmhi.scraping.dto.ScrapedIdea;
import com.mfon.rmhi.scraping.dto.ScrapingConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Abstract base class for website scrapers providing common functionality
 * including rate limiting, retry logic, and content hashing
 */
@Slf4j
@Component
public abstract class AbstractWebsiteScraper {
    
    private static final int MAX_RETRIES = 3;
    private static final int BASE_DELAY_MS = 1000;
    private static final int MAX_DELAY_MS = 30000;
    
    /**
     * Template method for scraping ideas from a website
     * Handles rate limiting, retries, and error handling
     */
    public List<ScrapedIdea> scrapeIdeas(ScrapingConfig config) {
        log.info("Starting scraping for source: {}", config.getSourceName());
        
        int attempt = 0;
        Exception lastException = null;
        
        while (attempt < MAX_RETRIES) {
            try {
                // Apply rate limiting
                applyRateLimit(config.getRateLimitMs());
                
                // Perform the actual scraping
                List<ScrapedIdea> ideas = performScraping(config);
                
                // Generate content hashes for duplicate detection
                ideas.forEach(this::generateContentHash);
                
                log.info("Successfully scraped {} ideas from {}", ideas.size(), config.getSourceName());
                return ideas;
                
            } catch (Exception e) {
                lastException = e;
                attempt++;
                
                if (attempt < MAX_RETRIES) {
                    int delay = calculateExponentialBackoff(attempt);
                    log.warn("Scraping attempt {} failed for {}, retrying in {}ms: {}", 
                            attempt, config.getSourceName(), delay, e.getMessage());
                    
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Scraping interrupted", ie);
                    }
                } else {
                    log.error("All scraping attempts failed for {}", config.getSourceName(), e);
                }
            }
        }
        
        throw new RuntimeException("Failed to scrape after " + MAX_RETRIES + " attempts", lastException);
    }
    
    /**
     * Abstract method to be implemented by concrete scrapers
     * Contains the actual scraping logic specific to each website
     */
    protected abstract List<ScrapedIdea> performScraping(ScrapingConfig config) throws Exception;
    
    /**
     * Validates scraped data for completeness and correctness
     */
    public boolean validateScrapedData(ScrapedIdea idea) {
        if (idea == null) {
            return false;
        }
        
        // Check required fields
        if (isBlank(idea.getTitle()) || isBlank(idea.getDescription()) || 
            isBlank(idea.getSourceUrl()) || isBlank(idea.getSourceWebsite())) {
            log.warn("Scraped idea missing required fields: {}", idea.getTitle());
            return false;
        }
        
        // Check URL format
        if (!isValidUrl(idea.getSourceUrl())) {
            log.warn("Invalid source URL: {}", idea.getSourceUrl());
            return false;
        }
        
        return true;
    }
    
    /**
     * Applies rate limiting based on configuration
     */
    private void applyRateLimit(int rateLimitMs) {
        if (rateLimitMs > 0) {
            try {
                Thread.sleep(rateLimitMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Rate limiting interrupted", e);
            }
        }
    }
    
    /**
     * Calculates exponential backoff delay with jitter
     */
    private int calculateExponentialBackoff(int attempt) {
        int delay = Math.min(BASE_DELAY_MS * (int) Math.pow(2, attempt - 1), MAX_DELAY_MS);
        
        // Add jitter to prevent thundering herd
        int jitter = ThreadLocalRandom.current().nextInt(0, delay / 4);
        return delay + jitter;
    }
    
    /**
     * Generates SHA-256 content hash for duplicate detection
     */
    private void generateContentHash(ScrapedIdea idea) {
        try {
            String content = idea.getTitle() + "|" + idea.getDescription() + "|" + idea.getContent();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            idea.setContentHash(hexString.toString());
            idea.setScrapedAt(LocalDateTime.now());
            
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to generate content hash", e);
            // Use a simple hash as fallback
            String fallbackContent = idea.getTitle() + "|" + idea.getDescription() + "|" + idea.getContent();
            idea.setContentHash(String.valueOf(fallbackContent.hashCode()));
        }
    }
    
    /**
     * Utility method to check if string is blank
     */
    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * Basic URL validation
     */
    private boolean isValidUrl(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }
}