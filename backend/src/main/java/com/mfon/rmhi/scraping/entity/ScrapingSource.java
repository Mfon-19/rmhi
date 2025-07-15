package com.mfon.rmhi.scraping.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "scraping_sources")
@Getter
@Setter
public class ScrapingSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @Column(name = "scraper_class", nullable = false, length = 200)
    private String scraperClass;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "cron_expression", length = 50)
    private String cronExpression = "0 0 2 * * ?"; // Daily at 2 AM

    @Column(name = "rate_limit_ms")
    private Integer rateLimitMs = 1000;

    @Column(name = "max_pages")
    private Integer maxPages = 100;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_json", columnDefinition = "jsonb")
    private Map<String, Object> configJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public ScrapingSource() {
    }

    public ScrapingSource(String name, String baseUrl, String scraperClass) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.scraperClass = scraperClass;
    }
}