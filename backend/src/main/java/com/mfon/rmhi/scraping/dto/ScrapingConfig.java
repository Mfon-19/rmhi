package com.mfon.rmhi.scraping.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Configuration for scraping operations
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScrapingConfig {
    
    private String sourceName;
    private String baseUrl;
    private String scraperClass;
    private boolean enabled;
    private String cronExpression;
    private int rateLimitMs;
    private int maxPages;
    private Map<String, Object> configJson;
}