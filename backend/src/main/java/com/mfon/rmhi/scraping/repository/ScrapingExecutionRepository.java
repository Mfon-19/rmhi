package com.mfon.rmhi.scraping.repository;

import com.mfon.rmhi.scraping.entity.ScrapingExecution;
import com.mfon.rmhi.scraping.entity.ScrapingSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScrapingExecutionRepository extends JpaRepository<ScrapingExecution, Long> {

    // Find executions by source
    List<ScrapingExecution> findBySource(ScrapingSource source);

    // Find executions by source ID
    List<ScrapingExecution> findBySourceId(Integer sourceId);

    // Find executions by status
    List<ScrapingExecution> findByStatus(ScrapingExecution.ExecutionStatus status);

    // Find currently running executions
    @Query("SELECT e FROM ScrapingExecution e WHERE e.status = 'RUNNING'")
    List<ScrapingExecution> findRunningExecutions();

    // Find completed executions
    @Query("SELECT e FROM ScrapingExecution e WHERE e.status = 'COMPLETED' ORDER BY e.completedAt DESC")
    List<ScrapingExecution> findCompletedExecutions();

    // Find failed executions
    @Query("SELECT e FROM ScrapingExecution e WHERE e.status = 'FAILED' ORDER BY e.completedAt DESC")
    List<ScrapingExecution> findFailedExecutions();

    // Find executions within date range
    @Query("SELECT e FROM ScrapingExecution e WHERE e.startedAt BETWEEN :startDate AND :endDate ORDER BY e.startedAt DESC")
    List<ScrapingExecution> findByStartedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Find latest execution for a source
    @Query("SELECT e FROM ScrapingExecution e WHERE e.source = :source ORDER BY e.startedAt DESC LIMIT 1")
    Optional<ScrapingExecution> findLatestBySource(@Param("source") ScrapingSource source);

    // Find latest execution for a source by ID
    @Query("SELECT e FROM ScrapingExecution e WHERE e.source.id = :sourceId ORDER BY e.startedAt DESC LIMIT 1")
    Optional<ScrapingExecution> findLatestBySourceId(@Param("sourceId") Integer sourceId);

    // Check if source has running execution
    @Query("SELECT COUNT(e) > 0 FROM ScrapingExecution e WHERE e.source = :source AND e.status = 'RUNNING'")
    boolean hasRunningExecution(@Param("source") ScrapingSource source);

    // Find executions by source and status
    List<ScrapingExecution> findBySourceAndStatus(ScrapingSource source, ScrapingExecution.ExecutionStatus status);

    // Get execution statistics by status
    @Query("SELECT e.status, COUNT(e) FROM ScrapingExecution e GROUP BY e.status")
    List<Object[]> getExecutionCountByStatus();

    // Get execution statistics by source
    @Query("SELECT e.source.name, COUNT(e) FROM ScrapingExecution e GROUP BY e.source.name")
    List<Object[]> getExecutionCountBySource();

    // Find recent executions (last N days)
    @Query("SELECT e FROM ScrapingExecution e WHERE e.startedAt >= :since ORDER BY e.startedAt DESC")
    List<ScrapingExecution> findRecentExecutions(@Param("since") LocalDateTime since);

    // Find executions with errors
    @Query("SELECT e FROM ScrapingExecution e WHERE e.errorMessage IS NOT NULL ORDER BY e.startedAt DESC")
    List<ScrapingExecution> findExecutionsWithErrors();

    // Get average execution duration for completed executions
    @Query("SELECT AVG(EXTRACT(EPOCH FROM (e.completedAt - e.startedAt))) FROM ScrapingExecution e WHERE e.status = 'COMPLETED' AND e.completedAt IS NOT NULL")
    Double getAverageExecutionDurationSeconds();

    // Find long-running executions (running for more than specified hours)
    @Query("SELECT e FROM ScrapingExecution e WHERE e.status = 'RUNNING' AND e.startedAt < :cutoffTime")
    List<ScrapingExecution> findLongRunningExecutions(@Param("cutoffTime") LocalDateTime cutoffTime);

    // Get daily execution summary
    @Query("SELECT DATE(e.startedAt), e.status, COUNT(e) FROM ScrapingExecution e WHERE e.startedAt >= :since GROUP BY DATE(e.startedAt), e.status ORDER BY DATE(e.startedAt) DESC")
    List<Object[]> getDailyExecutionSummary(@Param("since") LocalDateTime since);
}