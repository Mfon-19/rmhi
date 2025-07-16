package com.mfon.rmhi.scraping.service;

import com.mfon.rmhi.scraping.dto.TransformedIdea;
import com.mfon.rmhi.scraping.dto.StagingSummaryReport;
import com.mfon.rmhi.scraping.entity.StagedIdea;
import com.mfon.rmhi.scraping.dto.ReviewStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing staged ideas
 */
public interface StagingService {
    
    /**
     * Store a transformed idea in staging
     * @param transformedIdea The transformed idea to store
     * @return The stored staged idea
     */
    StagedIdea storeIdea(TransformedIdea transformedIdea);
    
    /**
     * Get all staged ideas pending review
     * @return List of staged ideas with PENDING status
     */
    List<StagedIdea> getPendingReview();
    
    /**
     * Update the review status of a staged idea
     * @param stagedIdeaId The ID of the staged idea
     * @param status The new review status
     * @param reviewedBy The reviewer's identifier
     * @param reviewNotes Optional review notes
     */
    void updateReviewStatus(Long stagedIdeaId, ReviewStatus status, String reviewedBy, String reviewNotes);
    
    /**
     * Get all approved ideas ready for migration
     * @return List of approved staged ideas
     */
    List<StagedIdea> getApprovedIdeas();
    
    /**
     * Find a staged idea by ID
     * @param id The staged idea ID
     * @return Optional containing the staged idea if found
     */
    Optional<StagedIdea> findById(Long id);
    
    /**
     * Check if content already exists based on similarity
     * @param transformedIdea The idea to check for duplicates
     * @return true if similar content exists, false otherwise
     */
    boolean isDuplicate(TransformedIdea transformedIdea);
    
    /**
     * Generate a staging summary report
     * @return Staging summary with counts and statistics
     */
    StagingSummaryReport generateSummaryReport();
    
    /**
     * Clean up old staged ideas based on retention policy
     * @param retentionDays Number of days to retain migrated ideas
     * @return Number of ideas cleaned up
     */
    int cleanupOldIdeas(int retentionDays);
    
    /**
     * Get staged ideas by source website
     * @param sourceWebsite The source website name
     * @return List of staged ideas from the specified source
     */
    List<StagedIdea> getIdeasBySource(String sourceWebsite);
    
    /**
     * Get staged ideas scraped within a date range
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of staged ideas within the date range
     */
    List<StagedIdea> getIdeasByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Get recently scraped ideas (last N days)
     * @param days Number of days to look back
     * @return List of recently scraped ideas
     */
    List<StagedIdea> getRecentIdeas(int days);
}