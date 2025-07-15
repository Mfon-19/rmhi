package com.mfon.rmhi.scraping.dto;

/**
 * Enum representing the status of migration operations
 */
public enum MigrationStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    ROLLED_BACK
}