package com.mfon.rmhi.scraping.service;

import com.mfon.rmhi.scraping.dto.ScrapedIdea;
import com.mfon.rmhi.scraping.dto.TransformedIdea;

import java.util.List;

/**
 * Interface for AI-powered transformation of scraped ideas
 */
public interface AITransformationService {
    
    /**
     * Transforms a single scraped idea using AI to ensure uniqueness
     * @param originalIdea The original scraped idea
     * @return Transformed idea with unique content
     */
    TransformedIdea transformIdea(ScrapedIdea originalIdea);
    
    /**
     * Transforms multiple ideas in batch for efficiency
     * @param ideas List of scraped ideas to transform
     * @return List of transformed ideas
     */
    List<TransformedIdea> batchTransform(List<ScrapedIdea> ideas);
    
    /**
     * Validates that the transformation was successful and meets quality standards
     * @param transformed The transformed idea to validate
     * @return true if transformation is valid, false otherwise
     */
    boolean validateTransformation(TransformedIdea transformed);
}