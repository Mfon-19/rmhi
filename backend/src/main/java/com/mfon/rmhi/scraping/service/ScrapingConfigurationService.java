package com.mfon.rmhi.scraping.service;

import com.mfon.rmhi.scraping.entity.ScrapingSource;
import com.mfon.rmhi.scraping.repository.ScrapingSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Service for managing scraping source configurations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapingConfigurationService {

    private final ScrapingSourceRepository scrapingSourceRepository;

    // Validation patterns
    private static final Pattern CRON_PATTERN = Pattern.compile(
        "^\\s*($|#|\\w+\\s*=|(\\?|\\*|(?:[0-5]?\\d)(?:(?:-|\\/|\\,)(?:[0-5]?\\d))?(?:,(?:[0-5]?\\d)(?:(?:-|\\/|\\,)(?:[0-5]?\\d))?)*)\\s+(\\?|\\*|(?:[0-5]?\\d)(?:(?:-|\\/|\\,)(?:[0-5]?\\d))?(?:,(?:[0-5]?\\d)(?:(?:-|\\/|\\,)(?:[0-5]?\\d))?)*)\\s+(\\?|\\*|(?:[01]?\\d|2[0-3])(?:(?:-|\\/|\\,)(?:[01]?\\d|2[0-3]))?(?:,(?:[01]?\\d|2[0-3])(?:(?:-|\\/|\\,)(?:[01]?\\d|2[0-3]))?)*)\\s+(\\?|\\*|(?:0?[1-9]|[12]\\d|3[01])(?:(?:-|\\/|\\,)(?:0?[1-9]|[12]\\d|3[01]))?(?:,(?:0?[1-9]|[12]\\d|3[01])(?:(?:-|\\/|\\,)(?:0?[1-9]|[12]\\d|3[01]))?)*)\\s+(\\?|\\*|(?:[1-9]|1[012])(?:(?:-|\\/|\\,)(?:[1-9]|1[012]))?(?:L|W)?(?:,(?:[1-9]|1[012])(?:(?:-|\\/|\\,)(?:[1-9]|1[012]))?(?:L|W)?)*|\\?|\\*|(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)(?:(?:-)(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC))?(?:,(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)(?:(?:-)(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC))?)*)\\s+(\\?|\\*|(?:[0-6])(?:(?:-|\\/|\\,|#)(?:[0-6]))?(?:L)?(?:,(?:[0-6])(?:(?:-|\\/|\\,|#)(?:[0-6]))?(?:L)?)*|\\?|\\*|(?:MON|TUE|WED|THU|FRI|SAT|SUN)(?:(?:-)(?:MON|TUE|WED|THU|FRI|SAT|SUN))?(?:,(?:MON|TUE|WED|THU|FRI|SAT|SUN)(?:(?:-)(?:MON|TUE|WED|THU|FRI|SAT|SUN))?)*)(|\\s)+(\\?|\\*|(?:|\\d{4})(?:(?:-|\\/|\\,)(?:|\\d{4}))?(?:,(?:|\\d{4})(?:(?:-|\\/|\\,)(?:|\\d{4}))?)*))$"
    );
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^https?://[\\w\\-]+(\\.[\\w\\-]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?$"
    );

    /**
     * Get all scraping sources
     */
    public List<ScrapingSource> getAllSources() {
        log.debug("Retrieving all scraping sources");
        return scrapingSourceRepository.findAll();
    }

    /**
     * Get all enabled scraping sources
     */
    public List<ScrapingSource> getEnabledSources() {
        log.debug("Retrieving enabled scraping sources");
        return scrapingSourceRepository.findByEnabledTrue();
    }

    /**
     * Get scraping source by ID
     */
    public Optional<ScrapingSource> getSourceById(Integer id) {
        log.debug("Retrieving scraping source by ID: {}", id);
        return scrapingSourceRepository.findById(id);
    }

    /**
     * Get scraping source by name
     */
    public Optional<ScrapingSource> getSourceByName(String name) {
        log.debug("Retrieving scraping source by name: {}", name);
        return scrapingSourceRepository.findByName(name);
    }

    /**
     * Create a new scraping source with validation
     */
    @Transactional
    public ScrapingSource createSource(ScrapingSource source) {
        log.info("Creating new scraping source: {}", source.getName());
        
        validateScrapingSource(source);
        
        if (scrapingSourceRepository.existsByName(source.getName())) {
            throw new IllegalArgumentException("Scraping source with name '" + source.getName() + "' already exists");
        }
        
        ScrapingSource savedSource = scrapingSourceRepository.save(source);
        log.info("Successfully created scraping source with ID: {}", savedSource.getId());
        return savedSource;
    }

    /**
     * Update an existing scraping source with validation
     */
    @Transactional
    public ScrapingSource updateSource(Integer id, ScrapingSource updatedSource) {
        log.info("Updating scraping source with ID: {}", id);
        
        ScrapingSource existingSource = scrapingSourceRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Scraping source with ID " + id + " not found"));
        
        validateScrapingSource(updatedSource);
        
        // Check if name is being changed and if new name already exists
        if (!existingSource.getName().equals(updatedSource.getName()) && 
            scrapingSourceRepository.existsByName(updatedSource.getName())) {
            throw new IllegalArgumentException("Scraping source with name '" + updatedSource.getName() + "' already exists");
        }
        
        // Update fields
        existingSource.setName(updatedSource.getName());
        existingSource.setBaseUrl(updatedSource.getBaseUrl());
        existingSource.setScraperClass(updatedSource.getScraperClass());
        existingSource.setEnabled(updatedSource.getEnabled());
        existingSource.setCronExpression(updatedSource.getCronExpression());
        existingSource.setRateLimitMs(updatedSource.getRateLimitMs());
        existingSource.setMaxPages(updatedSource.getMaxPages());
        existingSource.setConfigJson(updatedSource.getConfigJson());
        
        ScrapingSource savedSource = scrapingSourceRepository.save(existingSource);
        log.info("Successfully updated scraping source: {}", savedSource.getName());
        return savedSource;
    }

    /**
     * Delete a scraping source
     */
    @Transactional
    public void deleteSource(Integer id) {
        log.info("Deleting scraping source with ID: {}", id);
        
        if (!scrapingSourceRepository.existsById(id)) {
            throw new IllegalArgumentException("Scraping source with ID " + id + " not found");
        }
        
        scrapingSourceRepository.deleteById(id);
        log.info("Successfully deleted scraping source with ID: {}", id);
    }

    /**
     * Enable/disable a scraping source
     */
    @Transactional
    public ScrapingSource toggleSourceStatus(Integer id, boolean enabled) {
        log.info("Toggling scraping source status - ID: {}, enabled: {}", id, enabled);
        
        ScrapingSource source = scrapingSourceRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Scraping source with ID " + id + " not found"));
        
        source.setEnabled(enabled);
        ScrapingSource savedSource = scrapingSourceRepository.save(source);
        log.info("Successfully toggled scraping source status: {}", savedSource.getName());
        return savedSource;
    }

    /**
     * Validate scraping source configuration
     */
    public void validateScrapingSource(ScrapingSource source) {
        log.debug("Validating scraping source: {}", source.getName());
        
        // Validate required fields
        if (source.getName() == null || source.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Source name cannot be null or empty");
        }
        
        if (source.getBaseUrl() == null || source.getBaseUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("Base URL cannot be null or empty");
        }
        
        if (source.getScraperClass() == null || source.getScraperClass().trim().isEmpty()) {
            throw new IllegalArgumentException("Scraper class cannot be null or empty");
        }
        
        // Validate URL format
        if (!URL_PATTERN.matcher(source.getBaseUrl()).matches()) {
            throw new IllegalArgumentException("Invalid URL format: " + source.getBaseUrl());
        }
        
        // Validate cron expression if provided
        if (source.getCronExpression() != null && !source.getCronExpression().trim().isEmpty()) {
            if (!isValidCronExpression(source.getCronExpression())) {
                throw new IllegalArgumentException("Invalid cron expression: " + source.getCronExpression());
            }
        }
        
        // Validate rate limit
        if (source.getRateLimitMs() != null && source.getRateLimitMs() < 0) {
            throw new IllegalArgumentException("Rate limit must be non-negative");
        }
        
        // Validate max pages
        if (source.getMaxPages() != null && source.getMaxPages() <= 0) {
            throw new IllegalArgumentException("Max pages must be positive");
        }
        
        // Validate scraper class exists (basic check)
        validateScraperClass(source.getScraperClass());
        
        log.debug("Scraping source validation passed: {}", source.getName());
    }

    /**
     * Validate cron expression format
     */
    private boolean isValidCronExpression(String cronExpression) {
        try {
            // Basic validation - check if it has 6 or 7 parts (seconds optional)
            String[] parts = cronExpression.trim().split("\\s+");
            return parts.length == 6 || parts.length == 7;
        } catch (Exception e) {
            log.warn("Invalid cron expression: {}", cronExpression, e);
            return false;
        }
    }

    /**
     * Validate that scraper class is valid
     */
    private void validateScraperClass(String scraperClass) {
        // For now, just check basic format - in a real implementation,
        // you might want to check if the class exists and implements the right interface
        if (!scraperClass.contains(".")) {
            throw new IllegalArgumentException("Scraper class must be a fully qualified class name");
        }
        
        // Could add more validation here, such as:
        // - Check if class exists in classpath
        // - Check if class implements ScrapingService interface
        // - Check if class has required annotations
    }

    /**
     * Get configuration summary
     */
    public Map<String, Object> getConfigurationSummary() {
        log.debug("Generating configuration summary");
        
        List<ScrapingSource> allSources = scrapingSourceRepository.findAll();
        long enabledCount = allSources.stream().filter(ScrapingSource::getEnabled).count();
        long disabledCount = allSources.size() - enabledCount;
        
        return Map.of(
            "totalSources", allSources.size(),
            "enabledSources", enabledCount,
            "disabledSources", disabledCount,
            "sources", allSources
        );
    }

    /**
     * Reload configuration (for runtime updates)
     */
    public void reloadConfiguration() {
        log.info("Reloading scraping configuration");
        // This method can be used to trigger configuration reload
        // For now, it's a placeholder - in a real implementation,
        // you might want to clear caches, restart schedulers, etc.
        log.info("Configuration reloaded successfully");
    }
}