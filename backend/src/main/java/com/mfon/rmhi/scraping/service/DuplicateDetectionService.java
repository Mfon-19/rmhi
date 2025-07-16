package com.mfon.rmhi.scraping.service;

import com.mfon.rmhi.scraping.dto.ScrapedIdea;
import com.mfon.rmhi.scraping.entity.StagedIdea;
import com.mfon.rmhi.scraping.repository.StagedIdeaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for detecting duplicate ideas using content hashing and similarity algorithms
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DuplicateDetectionService {
    
    private final StagedIdeaRepository stagedIdeaRepository;
    
    private static final double SIMILARITY_THRESHOLD = 0.85;
    
    /**
     * Checks if a scraped idea is a duplicate of existing staged ideas
     */
    public boolean isDuplicate(ScrapedIdea idea) {
        if (idea.getContentHash() == null) {
            log.warn("Scraped idea has no content hash, cannot check for duplicates: {}", idea.getTitle());
            return false;
        }
        
        // First check for exact hash matches
        if (stagedIdeaRepository.existsByContentHash(idea.getContentHash())) {
            log.info("Found exact duplicate by hash for idea: {}", idea.getTitle());
            return true;
        }
        
        // Check for similar content using text similarity
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        List<StagedIdea> existingIdeas = stagedIdeaRepository.findRecentIdeas(cutoffDate);
        
        for (StagedIdea existing : existingIdeas) {
            double similarity = calculateTextSimilarity(idea, existing);
            if (similarity >= SIMILARITY_THRESHOLD) {
                log.info("Found similar duplicate (similarity: {}) for idea: {} vs {}", 
                        similarity, idea.getTitle(), existing.getProjectName());
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Filters out duplicate ideas from a list
     */
    public List<ScrapedIdea> filterDuplicates(List<ScrapedIdea> ideas) {
        log.info("Filtering duplicates from {} ideas", ideas.size());
        
        // Remove duplicates within the current batch
        Set<String> seenHashes = ideas.stream()
                .map(ScrapedIdea::getContentHash)
                .collect(Collectors.toSet());
        
        List<ScrapedIdea> uniqueIdeas = ideas.stream()
                .filter(idea -> {
                    if (seenHashes.contains(idea.getContentHash())) {
                        seenHashes.remove(idea.getContentHash()); // Keep first occurrence
                        return true;
                    }
                    return false;
                })
                .collect(Collectors.toList());
        
        // Filter against existing staged ideas
        List<ScrapedIdea> filteredIdeas = uniqueIdeas.stream()
                .filter(idea -> !isDuplicate(idea))
                .collect(Collectors.toList());
        
        int duplicatesRemoved = ideas.size() - filteredIdeas.size();
        log.info("Removed {} duplicates, {} unique ideas remaining", duplicatesRemoved, filteredIdeas.size());
        
        return filteredIdeas;
    }
    
    /**
     * Calculates text similarity between a scraped idea and existing staged idea
     * Uses Jaccard similarity on word sets
     */
    private double calculateTextSimilarity(ScrapedIdea scraped, StagedIdea existing) {
        String scrapedText = normalizeText(scraped.getTitle() + " " + scraped.getDescription());
        String existingText = normalizeText(existing.getProjectName() + " " + existing.getShortDescription());
        
        Set<String> scrapedWords = new HashSet<>(List.of(scrapedText.split("\\s+")));
        Set<String> existingWords = new HashSet<>(List.of(existingText.split("\\s+")));
        
        return calculateJaccardSimilarity(scrapedWords, existingWords);
    }
    
    /**
     * Calculates Jaccard similarity between two sets
     */
    private double calculateJaccardSimilarity(Set<String> set1, Set<String> set2) {
        if (set1.isEmpty() && set2.isEmpty()) {
            return 1.0;
        }
        
        Set<String> intersection = set1.stream()
                .filter(set2::contains)
                .collect(Collectors.toSet());
        
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        return (double) intersection.size() / union.size();
    }
    
    /**
     * Calculates similarity between two text strings
     * @param text1 First text string
     * @param text2 Second text string
     * @return Similarity score between 0.0 and 1.0
     */
    public double calculateSimilarity(String text1, String text2) {
        String normalizedText1 = normalizeText(text1);
        String normalizedText2 = normalizeText(text2);
        
        Set<String> words1 = new HashSet<>(List.of(normalizedText1.split("\\s+")));
        Set<String> words2 = new HashSet<>(List.of(normalizedText2.split("\\s+")));
        
        return calculateJaccardSimilarity(words1, words2);
    }
    
    /**
     * Normalizes text for similarity comparison
     */
    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        
        return text.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\s]", "") // Remove special characters
                .replaceAll("\\s+", " ") // Normalize whitespace
                .trim();
    }
}