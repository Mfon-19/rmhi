package com.mfon.rmhi.scraping.repository;

import com.mfon.rmhi.scraping.entity.StagedIdea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface StagedIdeaRepository extends JpaRepository<StagedIdea, String> {

    // Find staged ideas by review status
    List<StagedIdea> findByReviewStatus(StagedIdea.ReviewStatus reviewStatus);

    // Find staged ideas by migration status
    List<StagedIdea> findByMigrationStatus(StagedIdea.MigrationStatus migrationStatus);

    // Find pending ideas for review
    @Query("SELECT s FROM StagedIdea s WHERE s.reviewStatus = 'PENDING' ORDER BY s.scrapedAt ASC")
    List<StagedIdea> findPendingReview();

    // Find approved ideas ready for migration
    @Query("SELECT s FROM StagedIdea s WHERE s.reviewStatus = 'APPROVED' AND s.migrationStatus = 'NOT_MIGRATED' ORDER BY s.reviewedAt ASC")
    List<StagedIdea> findApprovedForMigration();

    // Find ideas by source website
    List<StagedIdea> findBySourceWebsite(String sourceWebsite);

    // Find ideas scraped within a date range
    @Query("SELECT s FROM StagedIdea s WHERE s.scrapedAt BETWEEN :startDate AND :endDate ORDER BY s.scrapedAt DESC")
    List<StagedIdea> findByScrapedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Find ideas by content hash for duplicate detection
    Optional<StagedIdea> findByContentHash(String contentHash);

    // Check if content hash exists
    boolean existsByContentHash(String contentHash);

    // Find ideas by original URL
    Optional<StagedIdea> findByOriginalUrl(String originalUrl);

    // Count ideas by review status
    @Query("SELECT COUNT(s) FROM StagedIdea s WHERE s.reviewStatus = :status")
    long countByReviewStatus(@Param("status") StagedIdea.ReviewStatus status);

    // Count ideas by migration status
    @Query("SELECT COUNT(s) FROM StagedIdea s WHERE s.migrationStatus = :status")
    long countByMigrationStatus(@Param("status") StagedIdea.MigrationStatus status);

    // Find ideas older than specified days for cleanup
    @Query("SELECT s FROM StagedIdea s WHERE s.scrapedAt < :cutoffDate AND s.migrationStatus = 'MIGRATED'")
    List<StagedIdea> findOldMigratedIdeas(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Find ideas by reviewer
    List<StagedIdea> findByReviewedBy(String reviewedBy);

    // Get staging summary statistics
    @Query("SELECT s.reviewStatus, COUNT(s) FROM StagedIdea s GROUP BY s.reviewStatus")
    List<Object[]> getStagingSummaryByReviewStatus();

    @Query("SELECT s.migrationStatus, COUNT(s) FROM StagedIdea s GROUP BY s.migrationStatus")
    List<Object[]> getStagingSummaryByMigrationStatus();

    // Find ideas from the last N days for duplicate detection
    @Query("SELECT s FROM StagedIdea s WHERE s.scrapedAt >= :cutoffDate ORDER BY s.scrapedAt DESC")
    List<StagedIdea> findRecentIdeas(@Param("cutoffDate") LocalDateTime cutoffDate);

    boolean existsByOriginalUrl(String originalUrl);

    @Query("SELECT s.originalUrl FROM StagedIdea s")
    Set<String> findAllSourceUrls();
}