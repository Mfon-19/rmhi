package com.mfon.rmhi.scraping.service;

import com.mfon.rmhi.scraping.dto.TransformedIdea;
import com.mfon.rmhi.scraping.dto.StagingSummaryReport;
import com.mfon.rmhi.scraping.entity.StagedIdea;
import com.mfon.rmhi.scraping.repository.StagedIdeaRepository;
import com.mfon.rmhi.scraping.dto.ReviewStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of StagingService for managing staged ideas
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StagingServiceImpl implements StagingService {
    
    private final StagedIdeaRepository stagedIdeaRepository;
    private final DuplicateDetectionService duplicateDetectionService;
    
    // Similarity threshold for duplicate detection (85%)
    private static final double SIMILARITY_THRESHOLD = 0.85;
    
    @Override
    @Transactional
    public StagedIdea storeIdea(TransformedIdea transformedIdea) {
        log.debug("Storing transformed idea: {}", transformedIdea.getProjectName());
        
        // Check for duplicates first
        if (isDuplicate(transformedIdea)) {
            log.warn("Duplicate idea detected, skipping storage: {}", transformedIdea.getProjectName());
            throw new IllegalArgumentException("Duplicate idea detected: " + transformedIdea.getProjectName());
        }
        
        // Create staged idea entity
        StagedIdea stagedIdea = new StagedIdea();
        
        // Set metadata
        stagedIdea.setOriginalUrl(transformedIdea.getOriginalUrl());
        stagedIdea.setSourceWebsite(transformedIdea.getSourceWebsite());
        stagedIdea.setScrapedAt(transformedIdea.getScrapedAt());
        stagedIdea.setTransformedAt(transformedIdea.getTransformedAt());
        
        // Set transformed content
        stagedIdea.setProjectName(transformedIdea.getProjectName());
        stagedIdea.setShortDescription(transformedIdea.getShortDescription());
        stagedIdea.setSolution(transformedIdea.getSolution());
        stagedIdea.setProblemDescription(transformedIdea.getProblemDescription());
        stagedIdea.setTechnicalDetails(transformedIdea.getTechnicalDetails());
        stagedIdea.setCreatedBy(transformedIdea.getCreatedBy());
        stagedIdea.setLikes(transformedIdea.getLikes() != null ? transformedIdea.getLikes() : 0);
        stagedIdea.setRating(transformedIdea.getRating());
        
        // Convert lists to arrays for JPA
        if (transformedIdea.getTechnologies() != null) {
            stagedIdea.setTechnologies(transformedIdea.getTechnologies());
        }
        if (transformedIdea.getCategories() != null) {
            stagedIdea.setCategories(transformedIdea.getCategories());
        }
        
        // Set content hash for duplicate detection
        String contentHash = generateContentHash(transformedIdea);
        stagedIdea.setContentHash(contentHash);
        
        // Set default statuses
        stagedIdea.setReviewStatus(StagedIdea.ReviewStatus.PENDING);
        stagedIdea.setMigrationStatus(StagedIdea.MigrationStatus.NOT_MIGRATED);
        
        // Store original data as JSON (simplified for now)
        Map<String, Object> originalData = new HashMap<>();
        originalData.put("transformationModel", transformedIdea.getTransformationModel());
        originalData.put("transformationConfidence", transformedIdea.getTransformationConfidence());
        stagedIdea.setOriginalData(originalData);
        
        try {
            StagedIdea saved = stagedIdeaRepository.save(stagedIdea);
            log.info("Successfully stored staged idea with ID: {}", saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("Failed to store staged idea: {}", transformedIdea.getProjectName(), e);
            throw new RuntimeException("Failed to store staged idea", e);
        }
    }
    
    @Override
    public List<StagedIdea> getPendingReview() {
        log.debug("Retrieving pending review ideas");
        return stagedIdeaRepository.findPendingReview();
    }
    
    @Override
    @Transactional
    public void updateReviewStatus(Long stagedIdeaId, ReviewStatus status, String reviewedBy, String reviewNotes) {
        log.debug("Updating review status for idea ID: {} to {}", stagedIdeaId, status);
        
        Optional<StagedIdea> optionalIdea = stagedIdeaRepository.findById(String.valueOf(stagedIdeaId));
        if (optionalIdea.isEmpty()) {
            throw new IllegalArgumentException("Staged idea not found with ID: " + stagedIdeaId);
        }
        
        StagedIdea stagedIdea = optionalIdea.get();
        
        // Convert ReviewStatus enum to StagedIdea.ReviewStatus enum
        StagedIdea.ReviewStatus entityStatus = switch (status) {
            case PENDING -> StagedIdea.ReviewStatus.PENDING;
            case APPROVED -> StagedIdea.ReviewStatus.APPROVED;
            case REJECTED -> StagedIdea.ReviewStatus.REJECTED;
        };
        
        stagedIdea.setReviewStatus(entityStatus);
        stagedIdea.setReviewedBy(reviewedBy);
        stagedIdea.setReviewedAt(LocalDateTime.now());
        stagedIdea.setReviewNotes(reviewNotes);
        
        stagedIdeaRepository.save(stagedIdea);
        log.info("Updated review status for idea ID: {} to {}", stagedIdeaId, status);
    }
    
    @Override
    public List<StagedIdea> getApprovedIdeas() {
        log.debug("Retrieving approved ideas ready for migration");
        return stagedIdeaRepository.findApprovedForMigration();
    }
    
    @Override
    public Optional<StagedIdea> findById(Long id) {
        return stagedIdeaRepository.findById(String.valueOf(id));
    }
    
    @Override
    public boolean isDuplicate(TransformedIdea transformedIdea) {
        log.debug("Checking for duplicates of idea: {}", transformedIdea.getProjectName());
        
        // First check by content hash (exact match)
        String contentHash = generateContentHash(transformedIdea);
        if (stagedIdeaRepository.existsByContentHash(contentHash)) {
            log.debug("Exact duplicate found by content hash");
            return true;
        }
        
        // Check by URL (exact match)
        if (transformedIdea.getOriginalUrl() != null) {
            Optional<StagedIdea> existingByUrl = stagedIdeaRepository.findByOriginalUrl(transformedIdea.getOriginalUrl());
            if (existingByUrl.isPresent()) {
                log.debug("Duplicate found by original URL");
                return true;
            }
        }
        
        // Check for content similarity using the duplicate detection service
        List<StagedIdea> recentIdeas = getRecentIdeas(30); // Check last 30 days
        
        for (StagedIdea existingIdea : recentIdeas) {
            double similarity = duplicateDetectionService.calculateSimilarity(
                transformedIdea.getProjectName() + " " + transformedIdea.getShortDescription(),
                existingIdea.getProjectName() + " " + existingIdea.getShortDescription()
            );
            
            if (similarity >= SIMILARITY_THRESHOLD) {
                log.debug("Similar content found with similarity: {}", similarity);
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public StagingSummaryReport generateSummaryReport() {
        log.debug("Generating staging summary report");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime weekStart = now.minusDays(7);
        LocalDateTime monthStart = now.minusDays(30);
        
        // Get review status counts
        long pendingCount = stagedIdeaRepository.countByReviewStatus(StagedIdea.ReviewStatus.PENDING);
        long approvedCount = stagedIdeaRepository.countByReviewStatus(StagedIdea.ReviewStatus.APPROVED);
        long rejectedCount = stagedIdeaRepository.countByReviewStatus(StagedIdea.ReviewStatus.REJECTED);
        long totalIdeas = stagedIdeaRepository.count();
        
        // Get migration status counts
        long notMigratedCount = stagedIdeaRepository.countByMigrationStatus(StagedIdea.MigrationStatus.NOT_MIGRATED);
        long migratedCount = stagedIdeaRepository.countByMigrationStatus(StagedIdea.MigrationStatus.MIGRATED);
        long migrationFailedCount = stagedIdeaRepository.countByMigrationStatus(StagedIdea.MigrationStatus.FAILED);
        
        // Get ideas by source
        Map<String, Long> ideasBySource = stagedIdeaRepository.findAll().stream()
            .collect(Collectors.groupingBy(
                StagedIdea::getSourceWebsite,
                Collectors.counting()
            ));
        
        // Get time-based counts
        long ideasScrapedToday = stagedIdeaRepository.findByScrapedAtBetween(todayStart, now).size();
        long ideasScrapedThisWeek = stagedIdeaRepository.findByScrapedAtBetween(weekStart, now).size();
        long ideasScrapedThisMonth = stagedIdeaRepository.findByScrapedAtBetween(monthStart, now).size();
        
        // Calculate quality metrics
        List<StagedIdea> allIdeas = stagedIdeaRepository.findAll();
        OptionalDouble avgRating = allIdeas.stream()
            .filter(idea -> idea.getRating() != null)
            .mapToInt(StagedIdea::getRating)
            .average();
        
        long highQualityIdeas = allIdeas.stream()
            .filter(idea -> idea.getRating() != null && idea.getRating() >= 7)
            .count();
        
        long mediumQualityIdeas = allIdeas.stream()
            .filter(idea -> idea.getRating() != null && idea.getRating() >= 4 && idea.getRating() <= 6)
            .count();
        
        long lowQualityIdeas = allIdeas.stream()
            .filter(idea -> idea.getRating() != null && idea.getRating() <= 3)
            .count();
        
        // Get recent activity timestamps
        LocalDateTime lastScrapingTime = allIdeas.stream()
            .map(StagedIdea::getScrapedAt)
            .max(LocalDateTime::compareTo)
            .orElse(null);
        
        LocalDateTime lastReviewTime = allIdeas.stream()
            .map(StagedIdea::getReviewedAt)
            .filter(Objects::nonNull)
            .max(LocalDateTime::compareTo)
            .orElse(null);
        
        LocalDateTime lastMigrationTime = allIdeas.stream()
            .map(StagedIdea::getMigratedAt)
            .filter(Objects::nonNull)
            .max(LocalDateTime::compareTo)
            .orElse(null);
        
        return StagingSummaryReport.builder()
            .generatedAt(now)
            .reportPeriod("All Time")
            .pendingCount(pendingCount)
            .approvedCount(approvedCount)
            .rejectedCount(rejectedCount)
            .totalIdeas(totalIdeas)
            .notMigratedCount(notMigratedCount)
            .migratedCount(migratedCount)
            .migrationFailedCount(migrationFailedCount)
            .ideasBySource(ideasBySource)
            .ideasScrapedToday(ideasScrapedToday)
            .ideasScrapedThisWeek(ideasScrapedThisWeek)
            .ideasScrapedThisMonth(ideasScrapedThisMonth)
            .averageRating(avgRating.isPresent() ? avgRating.getAsDouble() : null)
            .highQualityIdeas(highQualityIdeas)
            .mediumQualityIdeas(mediumQualityIdeas)
            .lowQualityIdeas(lowQualityIdeas)
            .duplicatesDetected(0L) // This would need to be tracked separately
            .transformationFailures(0L) // This would need to be tracked separately
            .lastScrapingTime(lastScrapingTime)
            .lastReviewTime(lastReviewTime)
            .lastMigrationTime(lastMigrationTime)
            .build();
    }
    
    @Override
    @Transactional
    public int cleanupOldIdeas(int retentionDays) {
        log.debug("Cleaning up old staged ideas older than {} days", retentionDays);
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        List<StagedIdea> oldIdeas = stagedIdeaRepository.findOldMigratedIdeas(cutoffDate);
        
        if (oldIdeas.isEmpty()) {
            log.debug("No old ideas found for cleanup");
            return 0;
        }
        
        int deletedCount = oldIdeas.size();
        stagedIdeaRepository.deleteAll(oldIdeas);
        
        log.info("Cleaned up {} old staged ideas", deletedCount);
        return deletedCount;
    }
    
    @Override
    public List<StagedIdea> getIdeasBySource(String sourceWebsite) {
        return stagedIdeaRepository.findBySourceWebsite(sourceWebsite);
    }
    
    @Override
    public List<StagedIdea> getIdeasByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return stagedIdeaRepository.findByScrapedAtBetween(startDate, endDate);
    }
    
    @Override
    public List<StagedIdea> getRecentIdeas(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        return stagedIdeaRepository.findRecentIdeas(cutoffDate);
    }
    
    /**
     * Generate a content hash for duplicate detection
     */
    private String generateContentHash(TransformedIdea idea) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            // Combine key content fields for hashing
            StringBuilder content = new StringBuilder();
            content.append(idea.getProjectName() != null ? idea.getProjectName().toLowerCase().trim() : "");
            content.append("|");
            content.append(idea.getShortDescription() != null ? idea.getShortDescription().toLowerCase().trim() : "");
            content.append("|");
            content.append(idea.getSolution() != null ? idea.getSolution().toLowerCase().trim() : "");
            
            byte[] hash = digest.digest(content.toString().getBytes());
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to generate content hash", e);
            throw new RuntimeException("Failed to generate content hash", e);
        }
    }
}