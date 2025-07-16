package com.mfon.rmhi.scraping.service;

import com.mfon.rmhi.scraping.dto.MigrationResult;
import com.mfon.rmhi.scraping.dto.MigrationStatus;

import java.util.List;

/**
 * Service interface for migrating approved staged ideas to production database
 */
public interface MigrationService {
    
    /**
     * Migrate all approved staged ideas to production database
     * @return MigrationResult containing migration statistics and status
     */
    MigrationResult migrateApprovedIdeas();
    
    /**
     * Migrate approved staged ideas in batches
     * @param batchSize number of ideas to migrate in each batch
     * @return MigrationResult containing migration statistics and status
     */
    MigrationResult migrateApprovedIdeas(int batchSize);
    
    /**
     * Migrate specific staged ideas by their IDs
     * @param stagedIdeaIds list of staged idea IDs to migrate
     * @return MigrationResult containing migration statistics and status
     */
    MigrationResult migrateSpecificIdeas(List<Long> stagedIdeaIds);
    
    /**
     * Rollback a migration by migration ID
     * @param migrationId the ID of the migration to rollback
     * @return true if rollback was successful, false otherwise
     */
    boolean rollbackMigration(String migrationId);
    
    /**
     * Get the status of a migration by migration ID
     * @param migrationId the ID of the migration
     * @return MigrationStatus of the specified migration
     */
    MigrationStatus getMigrationStatus(String migrationId);
    
    /**
     * Get all migration results for audit purposes
     * @return list of all migration results
     */
    List<MigrationResult> getAllMigrationResults();
}