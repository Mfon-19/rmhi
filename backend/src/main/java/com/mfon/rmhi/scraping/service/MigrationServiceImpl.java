package com.mfon.rmhi.scraping.service;

import com.mfon.rmhi.model.Category;
import com.mfon.rmhi.model.Idea;
import com.mfon.rmhi.model.Technology;
import com.mfon.rmhi.repository.CategoryRepository;
import com.mfon.rmhi.repository.IdeaRepository;
import com.mfon.rmhi.repository.TechnologyRepository;
import com.mfon.rmhi.scraping.dto.MigrationResult;
import com.mfon.rmhi.scraping.dto.MigrationStatus;
import com.mfon.rmhi.scraping.entity.StagedIdea;
import com.mfon.rmhi.scraping.repository.StagedIdeaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of MigrationService for migrating approved staged ideas to production
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MigrationServiceImpl implements MigrationService {

    private final StagedIdeaRepository stagedIdeaRepository;
    private final IdeaRepository ideaRepository;
    private final TechnologyRepository technologyRepository;
    private final CategoryRepository categoryRepository;
    
    @Value("${scraping.migration.batch-size:50}")
    private int defaultBatchSize;
    
    @Value("${scraping.migration.enable-rollback:true}")
    private boolean rollbackEnabled;
    
    // In-memory storage for migration tracking (in production, this should be persisted)
    private final Map<String, MigrationResult> migrationHistory = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> migrationToIdeaIds = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public MigrationResult migrateApprovedIdeas() {
        return migrateApprovedIdeas(defaultBatchSize);
    }

    @Override
    @Transactional
    public MigrationResult migrateApprovedIdeas(int batchSize) {
        String migrationId = generateMigrationId();
        LocalDateTime startTime = LocalDateTime.now();
        
        log.info("Starting migration {} with batch size {}", migrationId, batchSize);
        
        MigrationResult.MigrationResultBuilder resultBuilder = MigrationResult.builder()
                .migrationId(migrationId)
                .startedAt(startTime)
                .successful(false)
                .ideasMigrated(0)
                .ideasSkipped(0)
                .ideasFailed(0)
                .errors(new ArrayList<>())
                .warnings(new ArrayList<>());

        try {
            List<StagedIdea> approvedIdeas = stagedIdeaRepository.findApprovedForMigration();
            log.info("Found {} approved ideas ready for migration", approvedIdeas.size());
            
            if (approvedIdeas.isEmpty()) {
                resultBuilder.successful(true)
                        .completedAt(LocalDateTime.now());
                MigrationResult result = resultBuilder.build();
                migrationHistory.put(migrationId, result);
                return result;
            }

            List<Long> migratedIdeaIds = new ArrayList<>();
            int totalMigrated = 0;
            int totalSkipped = 0;
            int totalFailed = 0;
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            // Process in batches
            for (int i = 0; i < approvedIdeas.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, approvedIdeas.size());
                List<StagedIdea> batch = approvedIdeas.subList(i, endIndex);
                
                log.debug("Processing batch {}-{} of {}", i + 1, endIndex, approvedIdeas.size());
                
                BatchMigrationResult batchResult = migrateBatch(batch);
                totalMigrated += batchResult.migrated;
                totalSkipped += batchResult.skipped;
                totalFailed += batchResult.failed;
                errors.addAll(batchResult.errors);
                warnings.addAll(batchResult.warnings);
                migratedIdeaIds.addAll(batchResult.migratedIds);
            }

            boolean successful = totalFailed == 0;
            LocalDateTime completedAt = LocalDateTime.now();
            
            MigrationResult result = resultBuilder
                    .successful(successful)
                    .ideasMigrated(totalMigrated)
                    .ideasSkipped(totalSkipped)
                    .ideasFailed(totalFailed)
                    .errors(errors)
                    .warnings(warnings)
                    .completedAt(completedAt)
                    .build();

            // Store migration history
            migrationHistory.put(migrationId, result);
            migrationToIdeaIds.put(migrationId, migratedIdeaIds);
            
            log.info("Migration {} completed. Migrated: {}, Skipped: {}, Failed: {}", 
                    migrationId, totalMigrated, totalSkipped, totalFailed);
            
            return result;
            
        } catch (Exception e) {
            log.error("Migration {} failed with exception", migrationId, e);
            
            MigrationResult result = resultBuilder
                    .successful(false)
                    .errors(List.of("Migration failed: " + e.getMessage()))
                    .completedAt(LocalDateTime.now())
                    .build();
            
            migrationHistory.put(migrationId, result);
            return result;
        }
    }

    @Override
    @Transactional
    public MigrationResult migrateSpecificIdeas(List<Long> stagedIdeaIds) {
        String migrationId = generateMigrationId();
        LocalDateTime startTime = LocalDateTime.now();
        
        log.info("Starting specific migration {} for {} ideas", migrationId, stagedIdeaIds.size());
        
        MigrationResult.MigrationResultBuilder resultBuilder = MigrationResult.builder()
                .migrationId(migrationId)
                .startedAt(startTime)
                .successful(false)
                .ideasMigrated(0)
                .ideasSkipped(0)
                .ideasFailed(0)
                .errors(new ArrayList<>())
                .warnings(new ArrayList<>());

        try {
            List<StagedIdea> stagedIdeas = stagedIdeaRepository.findAllById(stagedIdeaIds);
            
            // Filter only approved ideas
            List<StagedIdea> approvedIdeas = stagedIdeas.stream()
                    .filter(idea -> idea.getReviewStatus() == StagedIdea.ReviewStatus.APPROVED)
                    .filter(idea -> idea.getMigrationStatus() == StagedIdea.MigrationStatus.NOT_MIGRATED)
                    .collect(Collectors.toList());
            
            List<String> warnings = new ArrayList<>();
            if (approvedIdeas.size() != stagedIdeaIds.size()) {
                warnings.add(String.format("Only %d out of %d requested ideas are approved and ready for migration", 
                        approvedIdeas.size(), stagedIdeaIds.size()));
            }

            BatchMigrationResult batchResult = migrateBatch(approvedIdeas);
            
            // Combine warnings from validation and batch processing
            warnings.addAll(batchResult.warnings);
            
            MigrationResult result = resultBuilder
                    .successful(batchResult.failed == 0)
                    .ideasMigrated(batchResult.migrated)
                    .ideasSkipped(batchResult.skipped)
                    .ideasFailed(batchResult.failed)
                    .errors(batchResult.errors)
                    .warnings(warnings)
                    .completedAt(LocalDateTime.now())
                    .build();

            migrationHistory.put(migrationId, result);
            migrationToIdeaIds.put(migrationId, batchResult.migratedIds);
            
            return result;
            
        } catch (Exception e) {
            log.error("Specific migration {} failed with exception", migrationId, e);
            
            MigrationResult result = resultBuilder
                    .successful(false)
                    .errors(List.of("Migration failed: " + e.getMessage()))
                    .completedAt(LocalDateTime.now())
                    .build();
            
            migrationHistory.put(migrationId, result);
            return result;
        }
    }

    @Override
    @Transactional
    public boolean rollbackMigration(String migrationId) {
        if (!rollbackEnabled) {
            log.warn("Rollback is disabled in configuration");
            return false;
        }
        
        if (!migrationHistory.containsKey(migrationId)) {
            log.warn("Migration {} not found in history", migrationId);
            return false;
        }
        
        List<Long> migratedIdeaIds = migrationToIdeaIds.get(migrationId);
        if (migratedIdeaIds == null || migratedIdeaIds.isEmpty()) {
            log.warn("No migrated idea IDs found for migration {}", migrationId);
            return false;
        }
        
        try {
            log.info("Starting rollback for migration {} affecting {} ideas", migrationId, migratedIdeaIds.size());
            
            // Find staged ideas that were migrated
            List<StagedIdea> stagedIdeas = stagedIdeaRepository.findAllById(migratedIdeaIds);
            
            for (StagedIdea stagedIdea : stagedIdeas) {
                if (stagedIdea.getProductionIdeaId() != null) {
                    // Delete the production idea
                    ideaRepository.deleteById(stagedIdea.getProductionIdeaId());
                    
                    // Reset staging status
                    stagedIdea.setMigrationStatus(StagedIdea.MigrationStatus.NOT_MIGRATED);
                    stagedIdea.setMigratedAt(null);
                    stagedIdea.setProductionIdeaId(null);
                    
                    stagedIdeaRepository.save(stagedIdea);
                }
            }
            
            log.info("Rollback completed for migration {}", migrationId);
            return true;
            
        } catch (Exception e) {
            log.error("Rollback failed for migration {}", migrationId, e);
            return false;
        }
    }

    @Override
    public MigrationStatus getMigrationStatus(String migrationId) {
        MigrationResult result = migrationHistory.get(migrationId);
        if (result == null) {
            return MigrationStatus.NOT_STARTED;
        }
        
        if (result.getCompletedAt() == null) {
            return MigrationStatus.IN_PROGRESS;
        }
        
        return result.isSuccessful() ? MigrationStatus.COMPLETED : MigrationStatus.FAILED;
    }

    @Override
    public List<MigrationResult> getAllMigrationResults() {
        return new ArrayList<>(migrationHistory.values());
    }

    /**
     * Migrate a batch of staged ideas
     */
    private BatchMigrationResult migrateBatch(List<StagedIdea> stagedIdeas) {
        BatchMigrationResult result = new BatchMigrationResult();
        
        for (StagedIdea stagedIdea : stagedIdeas) {
            try {
                if (isDuplicateInProduction(stagedIdea)) {
                    log.debug("Skipping duplicate idea: {}", stagedIdea.getProjectName());
                    result.skipped++;
                    result.warnings.add("Skipped duplicate idea: " + stagedIdea.getProjectName());
                    
                    // Mark as skipped but don't change migration status
                    continue;
                }
                
                Idea productionIdea = mapStagedIdeaToProduction(stagedIdea);
                Idea savedIdea = ideaRepository.save(productionIdea);
                
                // Update staged idea with migration info
                stagedIdea.setMigrationStatus(StagedIdea.MigrationStatus.MIGRATED);
                stagedIdea.setMigratedAt(LocalDateTime.now());
                stagedIdea.setProductionIdeaId(savedIdea.getId());
                stagedIdeaRepository.save(stagedIdea);
                
                result.migrated++;
                result.migratedIds.add(stagedIdea.getId());
                
                log.debug("Successfully migrated idea: {}", stagedIdea.getProjectName());
                
            } catch (Exception e) {
                log.error("Failed to migrate staged idea {}: {}", stagedIdea.getId(), e.getMessage(), e);
                result.failed++;
                result.errors.add(String.format("Failed to migrate idea '%s': %s", 
                        stagedIdea.getProjectName(), e.getMessage()));
                
                // Mark as failed
                stagedIdea.setMigrationStatus(StagedIdea.MigrationStatus.FAILED);
                stagedIdeaRepository.save(stagedIdea);
            }
        }
        
        return result;
    }

    /**
     * Map a StagedIdea to a production Idea entity
     */
    private Idea mapStagedIdeaToProduction(StagedIdea stagedIdea) {
        Idea idea = new Idea();
        
        // Map basic fields
        idea.setProjectName(stagedIdea.getProjectName());
        idea.setShortDescription(stagedIdea.getShortDescription());
        idea.setSolution(stagedIdea.getSolution());
        idea.setProblemDescription(stagedIdea.getProblemDescription());
        idea.setTechnicalDetails(stagedIdea.getTechnicalDetails());
        idea.setCreatedBy(stagedIdea.getCreatedBy());
        idea.setLikes(stagedIdea.getLikes());
        idea.setRating(stagedIdea.getRating());
        
        // Map technologies
        if (stagedIdea.getTechnologies() != null && stagedIdea.getTechnologies().length > 0) {
            Set<Technology> technologies = Arrays.stream(stagedIdea.getTechnologies())
                    .map(this::findOrCreateTechnology)
                    .collect(Collectors.toSet());
            idea.setTechnologies(technologies);
        }
        
        // Map categories
        if (stagedIdea.getCategories() != null && stagedIdea.getCategories().length > 0) {
            Set<Category> categories = Arrays.stream(stagedIdea.getCategories())
                    .map(this::findOrCreateCategory)
                    .collect(Collectors.toSet());
            idea.setCategories(categories);
        }
        
        return idea;
    }

    /**
     * Find existing technology or create new one
     */
    private Technology findOrCreateTechnology(String technologyName) {
        String trimmedName = technologyName.trim();
        return technologyRepository.findByNameIgnoreCase(trimmedName)
                .orElseGet(() -> {
                    Technology newTech = new Technology(trimmedName);
                    return technologyRepository.save(newTech);
                });
    }

    /**
     * Find existing category or create new one
     */
    private Category findOrCreateCategory(String categoryName) {
        String trimmedName = categoryName.trim();
        return categoryRepository.findByNameIgnoreCase(trimmedName)
                .orElseGet(() -> {
                    Category newCategory = new Category(trimmedName);
                    return categoryRepository.save(newCategory);
                });
    }

    /**
     * Check if a staged idea already exists in production (duplicate detection)
     */
    private boolean isDuplicateInProduction(StagedIdea stagedIdea) {
        // Efficient duplicate detection based on project name and created by
        return ideaRepository.existsByProjectNameAndCreatedByIgnoreCase(
                stagedIdea.getProjectName(), 
                stagedIdea.getCreatedBy()
        );
    }

    /**
     * Generate a unique migration ID
     */
    private String generateMigrationId() {
        return "MIG_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Internal class to track batch migration results
     */
    private static class BatchMigrationResult {
        int migrated = 0;
        int skipped = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<Long> migratedIds = new ArrayList<>();
    }
}