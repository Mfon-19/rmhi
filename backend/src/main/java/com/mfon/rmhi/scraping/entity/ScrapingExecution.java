package com.mfon.rmhi.scraping.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "scraping_executions")
@Getter
@Setter
public class ScrapingExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private ScrapingSource source;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExecutionStatus status;

    @Column(name = "ideas_scraped")
    private Integer ideasScraped = 0;

    @Column(name = "ideas_transformed")
    private Integer ideasTransformed = 0;

    @Column(name = "ideas_staged")
    private Integer ideasStaged = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "execution_metadata", columnDefinition = "jsonb")
    private Map<String, Object> executionMetadata;

    public enum ExecutionStatus {
        RUNNING, COMPLETED, FAILED
    }

    public ScrapingExecution() {
    }

    public ScrapingExecution(ScrapingSource source, LocalDateTime startedAt, ExecutionStatus status) {
        this.source = source;
        this.startedAt = startedAt;
        this.status = status;
    }

    // Helper methods for updating execution progress
    public void markCompleted() {
        this.status = ExecutionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.status = ExecutionStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }

    public boolean isRunning() {
        return this.status == ExecutionStatus.RUNNING;
    }

    public boolean isCompleted() {
        return this.status == ExecutionStatus.COMPLETED;
    }

    public boolean isFailed() {
        return this.status == ExecutionStatus.FAILED;
    }
}