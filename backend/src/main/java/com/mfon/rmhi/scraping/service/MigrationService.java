package com.mfon.rmhi.scraping.service;

import com.mfon.rmhi.scraping.dto.MigrationResult;
import com.mfon.rmhi.scraping.dto.MigrationStatus;

/**
 * Interface for migrating approved ideas from staging to production
 */
public interface MigrationService {
    
    /**
     * Migrates all approved ideas from staging to production database
     * @return Result containing migration statistics and status
     */
    MigrationResult migrateApprovedIdeas();
    
    /**
     * Rolls back a specific migration if issues are detected
     * @param migrationId The ID of the migration to rollback
     */
    void rollbackMigration(String migrationId);
    
    /**
     * Gets the status of a specific migration
     * @param migrationId The ID of the migration to check
     * @return Current status of the migration
     */
    MigrationStatus getMigrationStatus(String migrationId);
}