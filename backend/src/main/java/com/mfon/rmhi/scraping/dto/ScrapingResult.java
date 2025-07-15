package com.mfon.rmhi.scraping.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Result of a scraping operation
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScrapingResult {
    
    private String sourceName;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private boolean successful;
    private int ideasScraped;
    private List<ScrapedIdea> scrapedIdeas;
    private String errorMessage;
    private List<String> warnings;
}