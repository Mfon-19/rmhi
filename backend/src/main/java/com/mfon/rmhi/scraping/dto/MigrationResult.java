package com.mfon.rmhi.scraping.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Result of a migration operation from staging to production
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MigrationResult {
    
    private String migrationId;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private boolean successful;
    private int ideasMigrated;
    private int ideasSkipped;
    private int ideasFailed;
    private List<String> errors;
    private List<String> warnings;
}