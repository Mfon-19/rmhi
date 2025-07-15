package com.mfon.rmhi.scraping.service;

import com.mfon.rmhi.scraping.dto.ScrapedIdea;
import com.mfon.rmhi.scraping.dto.ScrapingConfig;
import com.mfon.rmhi.scraping.dto.ScrapingResult;

import java.util.List;

/**
 * Core interface for scraping project ideas from various websites
 */
public interface IdeaScrapingService {
    
    /**
     * Scrapes ideas from configured sources
     * @param config Configuration for scraping parameters
     * @return Result containing scraped ideas and metadata
     */
    ScrapingResult scrapeIdeas(ScrapingConfig config);
    
    /**
     * Scrapes ideas from a specific source URL
     * @param sourceUrl The URL to scrape from
     * @param config Configuration for scraping parameters
     * @return List of scraped ideas
     */
    List<ScrapedIdea> scrapeFromSource(String sourceUrl, ScrapingConfig config);
    
    /**
     * Validates scraped data for completeness and correctness
     * @param idea The scraped idea to validate
     * @return true if the data is valid, false otherwise
     */
    boolean validateScrapedData(ScrapedIdea idea);
}