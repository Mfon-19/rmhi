package com.mfon.rmhi.scraping.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "staged_ideas")
@Getter
@Setter
public class StagedIdea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_url", nullable = false, length = 500)
    private String originalUrl;

    @Column(name = "source_website", nullable = false, length = 100)
    private String sourceWebsite;

    @Column(name = "scraped_at", nullable = false)
    private LocalDateTime scrapedAt = LocalDateTime.now();

    @Column(name = "transformed_at")
    private LocalDateTime transformedAt;

    // Original scraped data stored as JSON
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "original_data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> originalData;

    // Transformed data matching production schema
    @JsonProperty("project_name")
    @Column(name = "project_name", nullable = false)
    private String projectName;

    @JsonProperty("short_description")
    @Column(name = "short_description", columnDefinition = "TEXT")
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String solution;

    @JsonProperty("problem_description")
    @Column(name = "problem_description", columnDefinition = "TEXT")
    private String problemDescription;

    @JsonProperty("technical_details")
    @Column(name = "technical_details", columnDefinition = "TEXT")
    private String technicalDetails;

    @JsonProperty("created_by")
    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private Integer likes = 0;

    private Integer rating;

    // Technologies and categories as JSON arrays
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String[] technologies;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String[] categories;

    // Review and migration status
    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", length = 20)
    private ReviewStatus reviewStatus = ReviewStatus.PENDING;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "migration_status", length = 20)
    private MigrationStatus migrationStatus = MigrationStatus.NOT_MIGRATED;

    @Column(name = "migrated_at")
    private LocalDateTime migratedAt;

    @Column(name = "production_idea_id")
    private Long productionIdeaId;

    // Duplicate detection
    @Column(name = "content_hash", length = 64, unique = true)
    private String contentHash;

    public enum ReviewStatus {
        PENDING, APPROVED, REJECTED
    }

    public enum MigrationStatus {
        NOT_MIGRATED, MIGRATED, FAILED
    }

    public StagedIdea() {
    }
}