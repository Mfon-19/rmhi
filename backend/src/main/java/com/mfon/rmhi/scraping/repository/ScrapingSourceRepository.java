package com.mfon.rmhi.scraping.repository;

import com.mfon.rmhi.scraping.entity.ScrapingSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScrapingSourceRepository extends JpaRepository<ScrapingSource, Integer> {

    // Find scraping source by name
    Optional<ScrapingSource> findByName(String name);

    // Find all enabled scraping sources
    List<ScrapingSource> findByEnabledTrue();

    // Find all disabled scraping sources
    List<ScrapingSource> findByEnabledFalse();

    // Find sources by scraper class
    List<ScrapingSource> findByScraperClass(String scraperClass);

    // Find sources with specific cron expression
    List<ScrapingSource> findByCronExpression(String cronExpression);

    // Check if source name exists
    boolean existsByName(String name);

    // Find sources by base URL pattern
    @Query("SELECT s FROM ScrapingSource s WHERE s.baseUrl LIKE %:urlPattern%")
    List<ScrapingSource> findByBaseUrlContaining(@Param("urlPattern") String urlPattern);

    // Find sources with rate limit above threshold
    @Query("SELECT s FROM ScrapingSource s WHERE s.rateLimitMs > :threshold")
    List<ScrapingSource> findByRateLimitAbove(@Param("threshold") Integer threshold);

    // Find sources with max pages above threshold
    @Query("SELECT s FROM ScrapingSource s WHERE s.maxPages > :threshold")
    List<ScrapingSource> findByMaxPagesAbove(@Param("threshold") Integer threshold);

    // Get enabled sources ordered by name
    @Query("SELECT s FROM ScrapingSource s WHERE s.enabled = true ORDER BY s.name ASC")
    List<ScrapingSource> findEnabledSourcesOrderedByName();

    // Count enabled vs disabled sources
    @Query("SELECT s.enabled, COUNT(s) FROM ScrapingSource s GROUP BY s.enabled")
    List<Object[]> getSourceCountByStatus();
}