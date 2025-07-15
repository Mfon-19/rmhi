package com.mfon.rmhi.scraping.service;

import com.mfon.rmhi.scraping.dto.TransformedIdea;
import com.mfon.rmhi.scraping.entity.StagedIdea;
import com.mfon.rmhi.scraping.dto.ReviewStatus;

import java.util.List;

/**
 * Interface for managing staged ideas before production migration
 */
public interface StagingService {
    
    /**
     * Stores a transformed idea in the staging area
     * @param idea The transformed idea to store
     */
    void storeIdea(TransformedIdea idea);
    
    /**
     * Retrieves all ideas pending review
     * @return List of staged ideas awaiting review
     */
    List<StagedIdea> getPendingReview();
    
    /**
     * Updates the review status of a staged idea
     * @param stagedIdeaId The ID of the staged idea
     * @param status The new review status
     */
    void updateReviewStatus(Long stagedIdeaId, ReviewStatus status);
    
    /**
     * Retrieves all approved ideas ready for migration
     * @return List of approved staged ideas
     */
    List<StagedIdea> getApprovedIdeas();
}