package com.mfon.rmhi.scraping.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration class for the scraping module
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(ScrapingProperties.class)
@ComponentScan(basePackages = "com.mfon.rmhi.scraping")
public class ScrapingConfiguration {
}