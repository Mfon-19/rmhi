package com.mfon.rmhi.scraping.config;

import com.mfon.rmhi.scraping.service.DevPostScraper;
import com.mfon.rmhi.scraping.service.ScrapingOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Configuration component that registers all available scrapers with the orchestrator
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScraperRegistrationConfig implements CommandLineRunner {
    
    private final ScrapingOrchestrator scrapingOrchestrator;
    private final DevPostScraper devPostScraper;
    
    @Override
    public void run(String... args) {
        log.info("Registering scrapers with orchestrator");
        
        // Register DevPost scraper
        scrapingOrchestrator.registerScraper("DevPostScraper", devPostScraper);
        
        log.info("Scraper registration completed");
    }
}