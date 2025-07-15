package com.mfon.rmhi.scraping.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO representing a raw scraped idea before AI transformation
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScrapedIdea {
    
    private String sourceUrl;
    private String sourceWebsite;
    private LocalDateTime scrapedAt;
    
    // Raw scraped content
    private String title;
    private String description;
    private String content;
    private String author;
    private Integer likes;
    private Integer rating;
    
    // Raw technology and category data
    private List<String> technologies;
    private List<String> categories;
    
    // Additional metadata from scraping
    private Map<String, Object> metadata;
    
    // Content hash for duplicate detection
    private String contentHash;
}