package com.mfon.rmhi.scraping.controller;

import com.mfon.rmhi.scraping.entity.ScrapingSource;
import com.mfon.rmhi.scraping.service.ScrapingConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for managing scraping source configurations
 */
@RestController
@RequestMapping("/api/scraping/config")
@RequiredArgsConstructor
@Slf4j
public class ScrapingConfigurationController {

    private final ScrapingConfigurationService configurationService;

    /**
     * Get all scraping sources
     */
    @GetMapping("/sources")
    public ResponseEntity<List<ScrapingSource>> getAllSources() {
        log.info("GET /api/scraping/config/sources - Retrieving all scraping sources");
        List<ScrapingSource> sources = configurationService.getAllSources();
        return ResponseEntity.ok(sources);
    }

    /**
     * Get enabled scraping sources only
     */
    @GetMapping("/sources/enabled")
    public ResponseEntity<List<ScrapingSource>> getEnabledSources() {
        log.info("GET /api/scraping/config/sources/enabled - Retrieving enabled scraping sources");
        List<ScrapingSource> sources = configurationService.getEnabledSources();
        return ResponseEntity.ok(sources);
    }

    /**
     * Get scraping source by ID
     */
    @GetMapping("/sources/{id}")
    public ResponseEntity<ScrapingSource> getSourceById(@PathVariable Integer id) {
        log.info("GET /api/scraping/config/sources/{} - Retrieving scraping source by ID", id);
        Optional<ScrapingSource> source = configurationService.getSourceById(id);
        return source.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get scraping source by name
     */
    @GetMapping("/sources/by-name/{name}")
    public ResponseEntity<ScrapingSource> getSourceByName(@PathVariable String name) {
        log.info("GET /api/scraping/config/sources/by-name/{} - Retrieving scraping source by name", name);
        Optional<ScrapingSource> source = configurationService.getSourceByName(name);
        return source.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new scraping source
     */
    @PostMapping("/sources")
    public ResponseEntity<?> createSource(@RequestBody ScrapingSource source) {
        log.info("POST /api/scraping/config/sources - Creating new scraping source: {}", source.getName());
        try {
            ScrapingSource createdSource = configurationService.createSource(source);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdSource);
        } catch (IllegalArgumentException e) {
            log.error("Failed to create scraping source: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error creating scraping source", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Update an existing scraping source
     */
    @PutMapping("/sources/{id}")
    public ResponseEntity<?> updateSource(@PathVariable Integer id, @RequestBody ScrapingSource source) {
        log.info("PUT /api/scraping/config/sources/{} - Updating scraping source", id);
        try {
            ScrapingSource updatedSource = configurationService.updateSource(id, source);
            return ResponseEntity.ok(updatedSource);
        } catch (IllegalArgumentException e) {
            log.error("Failed to update scraping source: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error updating scraping source", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Delete a scraping source
     */
    @DeleteMapping("/sources/{id}")
    public ResponseEntity<?> deleteSource(@PathVariable Integer id) {
        log.info("DELETE /api/scraping/config/sources/{} - Deleting scraping source", id);
        try {
            configurationService.deleteSource(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Failed to delete scraping source: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error deleting scraping source", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Enable or disable a scraping source
     */
    @PatchMapping("/sources/{id}/status")
    public ResponseEntity<?> toggleSourceStatus(@PathVariable Integer id, @RequestParam boolean enabled) {
        log.info("PATCH /api/scraping/config/sources/{}/status - Toggling source status to: {}", id, enabled);
        try {
            ScrapingSource updatedSource = configurationService.toggleSourceStatus(id, enabled);
            return ResponseEntity.ok(updatedSource);
        } catch (IllegalArgumentException e) {
            log.error("Failed to toggle scraping source status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error toggling scraping source status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Validate a scraping source configuration without saving
     */
    @PostMapping("/sources/validate")
    public ResponseEntity<?> validateSource(@RequestBody ScrapingSource source) {
        log.info("POST /api/scraping/config/sources/validate - Validating scraping source configuration");
        try {
            configurationService.validateScrapingSource(source);
            return ResponseEntity.ok(Map.of("valid", true, "message", "Configuration is valid"));
        } catch (IllegalArgumentException e) {
            log.warn("Scraping source validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("valid", false, "error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error validating scraping source", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("valid", false, "error", "Internal server error"));
        }
    }

    /**
     * Get configuration summary
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getConfigurationSummary() {
        log.info("GET /api/scraping/config/summary - Retrieving configuration summary");
        Map<String, Object> summary = configurationService.getConfigurationSummary();
        return ResponseEntity.ok(summary);
    }

    /**
     * Reload configuration (for runtime updates)
     */
    @PostMapping("/reload")
    public ResponseEntity<?> reloadConfiguration() {
        log.info("POST /api/scraping/config/reload - Reloading configuration");
        try {
            configurationService.reloadConfiguration();
            return ResponseEntity.ok(Map.of("message", "Configuration reloaded successfully"));
        } catch (Exception e) {
            log.error("Failed to reload configuration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to reload configuration"));
        }
    }
}