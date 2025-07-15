package com.mfon.rmhi.scraping.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the scraping module
 */
@Data
@ConfigurationProperties(prefix = "scraping")
public class ScrapingProperties {
    
    /**
     * Whether scraping is enabled globally
     */
    private boolean enabled = true;
    
    /**
     * Default rate limit between requests in milliseconds
     */
    private int defaultRateLimit = 1000;
    
    /**
     * Maximum number of concurrent scrapers
     */
    private int maxConcurrentScrapers = 3;
    
    /**
     * AI transformation configuration
     */
    private AiTransformation aiTransformation = new AiTransformation();
    
    /**
     * Staging configuration
     */
    private Staging staging = new Staging();
    
    /**
     * Migration configuration
     */
    private Migration migration = new Migration();
    
    /**
     * Monitoring configuration
     */
    private Monitoring monitoring = new Monitoring();
    
    @Data
    public static class AiTransformation {
        private String provider = "gemini";
        private String apiKey;
        private String model = "gemini-2.0-flash-exp";
        private int maxTokens = 1000;
        private double temperature = 0.7;
    }
    
    @Data
    public static class Staging {
        private double duplicateThreshold = 0.85;
        private boolean autoApprove = false;
        private int retentionDays = 30;
    }
    
    @Data
    public static class Migration {
        private int batchSize = 50;
        private boolean enableRollback = true;
    }
    
    @Data
    public static class Monitoring {
        private boolean enableMetrics = true;
        private String logLevel = "INFO";
        private String alertWebhook;
    }
}