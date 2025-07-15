package com.mfon.rmhi.scraping.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO representing an AI-transformed idea ready for staging
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransformedIdea {
    
    // Original scraping metadata
    private String originalUrl;
    private String sourceWebsite;
    private LocalDateTime scrapedAt;
    private LocalDateTime transformedAt;
    
    // Transformed content matching production schema
    private String projectName;
    private String shortDescription;
    private String solution;
    private String problemDescription;
    private String technicalDetails;
    private String createdBy;
    private Integer likes;
    private Integer rating;
    
    // Processed technologies and categories
    private List<String> technologies;
    private List<String> categories;
    
    // Content hash for duplicate detection
    private String contentHash;
    
    // Transformation metadata
    private String transformationModel;
    private Double transformationConfidence;
}