package com.mfon.rmhi.scraping.service;

import com.mfon.rmhi.scraping.dto.TransformedIdea;
import com.mfon.rmhi.scraping.dto.StagingSummaryReport;
import com.mfon.rmhi.scraping.entity.StagedIdea;
import com.mfon.rmhi.scraping.repository.StagedIdeaRepository;
import com.mfon.rmhi.scraping.dto.ReviewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StagingService using Mockito
 */
@ExtendWith(MockitoExtension.class)
class StagingServiceTest {

    @Mock
    private StagedIdeaRepository stagedIdeaRepository;

    @Mock
    private DuplicateDetectionService duplicateDetectionService;

    @InjectMocks
    private StagingServiceImpl stagingService;

    private TransformedIdea sampleTransformedIdea;
    private StagedIdea sampleStagedIdea;

    @BeforeEach
    void setUp() {
        // Setup sample transformed idea
        sampleTransformedIdea = TransformedIdea.builder()
                .originalUrl("https://devpost.com/software/test-project")
                .sourceWebsite("DevPost")
                .scrapedAt(LocalDateTime.now().minusHours(1))
                .transformedAt(LocalDateTime.now())
                .projectName("Test Project")
                .shortDescription("A test project for unit testing")
                .solution("This project solves testing problems")
                .problemDescription("Testing is hard")
                .technicalDetails("Built with Java and Spring Boot")
                .createdBy("test-user")
                .likes(5)
                .rating(8)
                .technologies(Arrays.asList("Java", "Spring Boot", "JUnit"))
                .categories(Arrays.asList("Testing", "Development"))
                .contentHash("test-hash-123")
                .transformationModel("gemini-2.0-flash-exp")
                .transformationConfidence(0.95)
                .build();

        // Setup sample staged idea
        sampleStagedIdea = new StagedIdea();
        sampleStagedIdea.setOriginalUrl("https://devpost.com/software/existing-project");
        sampleStagedIdea.setSourceWebsite("DevPost");
        sampleStagedIdea.setScrapedAt(LocalDateTime.now().minusDays(1));
        sampleStagedIdea.setProjectName("Existing Project");
        sampleStagedIdea.setShortDescription("An existing project");
        sampleStagedIdea.setCreatedBy("existing-user");
        sampleStagedIdea.setContentHash("existing-hash-456");
        sampleStagedIdea.setReviewStatus(StagedIdea.ReviewStatus.PENDING);
        sampleStagedIdea.setMigrationStatus(StagedIdea.MigrationStatus.NOT_MIGRATED);
    }

    @Test
    void testStoreIdea_Success() {
        // Given
        when(stagedIdeaRepository.existsByContentHash(anyString())).thenReturn(false);
        when(stagedIdeaRepository.findByOriginalUrl(anyString())).thenReturn(Optional.empty());
        when(stagedIdeaRepository.findRecentIdeas(any(LocalDateTime.class))).thenReturn(Arrays.asList());
        when(duplicateDetectionService.calculateSimilarity(anyString(), anyString())).thenReturn(0.3);
        
        StagedIdea savedIdea = new StagedIdea();
        savedIdea.setId(1L);
        savedIdea.setProjectName("Test Project");
        savedIdea.setShortDescription("A test project for unit testing");
        savedIdea.setReviewStatus(StagedIdea.ReviewStatus.PENDING);
        savedIdea.setMigrationStatus(StagedIdea.MigrationStatus.NOT_MIGRATED);
        savedIdea.setTechnologies(new String[]{"Java", "Spring Boot", "JUnit"});
        savedIdea.setCategories(new String[]{"Testing", "Development"});
        
        when(stagedIdeaRepository.save(any(StagedIdea.class))).thenReturn(savedIdea);

        // When
        StagedIdea result = stagingService.storeIdea(sampleTransformedIdea);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getProjectName()).isEqualTo("Test Project");
        assertThat(result.getShortDescription()).isEqualTo("A test project for unit testing");
        assertThat(result.getReviewStatus()).isEqualTo(StagedIdea.ReviewStatus.PENDING);
        assertThat(result.getMigrationStatus()).isEqualTo(StagedIdea.MigrationStatus.NOT_MIGRATED);
        assertThat(result.getTechnologies()).containsExactly("Java", "Spring Boot", "JUnit");
        assertThat(result.getCategories()).containsExactly("Testing", "Development");
        
        verify(stagedIdeaRepository).save(any(StagedIdea.class));
    }

    @Test
    void testStoreIdea_DuplicateDetected() {
        // Given
        when(stagedIdeaRepository.existsByContentHash(anyString())).thenReturn(false);
        when(stagedIdeaRepository.findByOriginalUrl(anyString())).thenReturn(Optional.empty());
        when(stagedIdeaRepository.findRecentIdeas(any(LocalDateTime.class))).thenReturn(Arrays.asList(sampleStagedIdea));
        when(duplicateDetectionService.calculateSimilarity(anyString(), anyString())).thenReturn(0.9);

        // When & Then
        assertThatThrownBy(() -> stagingService.storeIdea(sampleTransformedIdea))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate idea detected");
    }

    @Test
    void testStoreIdea_DuplicateByContentHash() {
        // Given
        when(stagedIdeaRepository.existsByContentHash(anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> stagingService.storeIdea(sampleTransformedIdea))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate idea detected");
    }

    @Test
    void testStoreIdea_DuplicateByUrl() {
        // Given
        when(stagedIdeaRepository.existsByContentHash(anyString())).thenReturn(false);
        when(stagedIdeaRepository.findByOriginalUrl(sampleTransformedIdea.getOriginalUrl())).thenReturn(Optional.of(sampleStagedIdea));

        // When & Then
        assertThatThrownBy(() -> stagingService.storeIdea(sampleTransformedIdea))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate idea detected");
    }

    @Test
    void testGetPendingReview() {
        // Given
        StagedIdea pendingIdea1 = createStagedIdea("Pending 1", StagedIdea.ReviewStatus.PENDING);
        StagedIdea pendingIdea2 = createStagedIdea("Pending 2", StagedIdea.ReviewStatus.PENDING);
        
        when(stagedIdeaRepository.findPendingReview()).thenReturn(Arrays.asList(pendingIdea1, pendingIdea2));

        // When
        List<StagedIdea> result = stagingService.getPendingReview();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(StagedIdea::getProjectName)
                .containsExactlyInAnyOrder("Pending 1", "Pending 2");
        
        verify(stagedIdeaRepository).findPendingReview();
    }

    @Test
    void testUpdateReviewStatus() {
        // Given
        Long ideaId = 1L;
        sampleStagedIdea.setId(ideaId);
        String reviewer = "test-reviewer";
        String notes = "Looks good!";
        
        when(stagedIdeaRepository.findById(ideaId)).thenReturn(Optional.of(sampleStagedIdea));
        when(stagedIdeaRepository.save(any(StagedIdea.class))).thenReturn(sampleStagedIdea);

        // When
        stagingService.updateReviewStatus(ideaId, ReviewStatus.APPROVED, reviewer, notes);

        // Then
        verify(stagedIdeaRepository).findById(ideaId);
        verify(stagedIdeaRepository).save(argThat(idea -> 
            idea.getReviewStatus() == StagedIdea.ReviewStatus.APPROVED &&
            reviewer.equals(idea.getReviewedBy()) &&
            notes.equals(idea.getReviewNotes()) &&
            idea.getReviewedAt() != null
        ));
    }

    @Test
    void testUpdateReviewStatus_NotFound() {
        // Given
        when(stagedIdeaRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> stagingService.updateReviewStatus(999L, ReviewStatus.APPROVED, "reviewer", "notes"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Staged idea not found with ID: 999");
        
        verify(stagedIdeaRepository).findById(999L);
        verify(stagedIdeaRepository, never()).save(any());
    }

    @Test
    void testGetApprovedIdeas() {
        // Given
        StagedIdea approvedIdea1 = createStagedIdea("Approved 1", StagedIdea.ReviewStatus.APPROVED);
        StagedIdea approvedIdea2 = createStagedIdea("Approved 2", StagedIdea.ReviewStatus.APPROVED);
        
        // Set migration status to NOT_MIGRATED for approved ideas
        approvedIdea1.setMigrationStatus(StagedIdea.MigrationStatus.NOT_MIGRATED);
        approvedIdea2.setMigrationStatus(StagedIdea.MigrationStatus.NOT_MIGRATED);
        
        when(stagedIdeaRepository.findApprovedForMigration()).thenReturn(Arrays.asList(approvedIdea1, approvedIdea2));

        // When
        List<StagedIdea> result = stagingService.getApprovedIdeas();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(StagedIdea::getProjectName)
                .containsExactlyInAnyOrder("Approved 1", "Approved 2");
        
        verify(stagedIdeaRepository).findApprovedForMigration();
    }

    @Test
    void testFindById() {
        // Given
        Long ideaId = 1L;
        sampleStagedIdea.setId(ideaId);
        when(stagedIdeaRepository.findById(ideaId)).thenReturn(Optional.of(sampleStagedIdea));

        // When
        Optional<StagedIdea> result = stagingService.findById(ideaId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getProjectName()).isEqualTo(sampleStagedIdea.getProjectName());
        
        verify(stagedIdeaRepository).findById(ideaId);
    }

    @Test
    void testIsDuplicate_NotDuplicate() {
        // Given
        when(stagedIdeaRepository.existsByContentHash(anyString())).thenReturn(false);
        when(stagedIdeaRepository.findByOriginalUrl(anyString())).thenReturn(Optional.empty());
        when(stagedIdeaRepository.findRecentIdeas(any(LocalDateTime.class))).thenReturn(Arrays.asList());
        when(duplicateDetectionService.calculateSimilarity(anyString(), anyString())).thenReturn(0.3);

        // When
        boolean result = stagingService.isDuplicate(sampleTransformedIdea);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void testIsDuplicate_SimilarContent() {
        // Given
        when(stagedIdeaRepository.existsByContentHash(anyString())).thenReturn(false);
        when(stagedIdeaRepository.findByOriginalUrl(anyString())).thenReturn(Optional.empty());
        when(stagedIdeaRepository.findRecentIdeas(any(LocalDateTime.class))).thenReturn(Arrays.asList(sampleStagedIdea));
        when(duplicateDetectionService.calculateSimilarity(anyString(), anyString())).thenReturn(0.9);

        // When
        boolean result = stagingService.isDuplicate(sampleTransformedIdea);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void testGenerateSummaryReport() {
        // Given
        StagedIdea pendingIdea = createStagedIdea("Pending", StagedIdea.ReviewStatus.PENDING);
        pendingIdea.setRating(8);
        pendingIdea.setSourceWebsite("DevPost");
        
        StagedIdea approvedIdea = createStagedIdea("Approved", StagedIdea.ReviewStatus.APPROVED);
        approvedIdea.setRating(6);
        approvedIdea.setSourceWebsite("GitHub");
        approvedIdea.setMigrationStatus(StagedIdea.MigrationStatus.MIGRATED);
        
        StagedIdea rejectedIdea = createStagedIdea("Rejected", StagedIdea.ReviewStatus.REJECTED);
        rejectedIdea.setRating(3);
        rejectedIdea.setSourceWebsite("DevPost");
        
        List<StagedIdea> allIdeas = Arrays.asList(pendingIdea, approvedIdea, rejectedIdea);
        
        // Mock repository calls
        when(stagedIdeaRepository.countByReviewStatus(StagedIdea.ReviewStatus.PENDING)).thenReturn(1L);
        when(stagedIdeaRepository.countByReviewStatus(StagedIdea.ReviewStatus.APPROVED)).thenReturn(1L);
        when(stagedIdeaRepository.countByReviewStatus(StagedIdea.ReviewStatus.REJECTED)).thenReturn(1L);
        when(stagedIdeaRepository.count()).thenReturn(3L);
        
        when(stagedIdeaRepository.countByMigrationStatus(StagedIdea.MigrationStatus.NOT_MIGRATED)).thenReturn(2L);
        when(stagedIdeaRepository.countByMigrationStatus(StagedIdea.MigrationStatus.MIGRATED)).thenReturn(1L);
        when(stagedIdeaRepository.countByMigrationStatus(StagedIdea.MigrationStatus.FAILED)).thenReturn(0L);
        
        when(stagedIdeaRepository.findAll()).thenReturn(allIdeas);
        when(stagedIdeaRepository.findByScrapedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList()); // Empty for time-based queries in this test

        // When
        StagingSummaryReport report = stagingService.generateSummaryReport();

        // Then
        assertThat(report).isNotNull();
        assertThat(report.getTotalIdeas()).isEqualTo(3);
        assertThat(report.getPendingCount()).isEqualTo(1);
        assertThat(report.getApprovedCount()).isEqualTo(1);
        assertThat(report.getRejectedCount()).isEqualTo(1);
        assertThat(report.getMigratedCount()).isEqualTo(1);
        assertThat(report.getNotMigratedCount()).isEqualTo(2);
        assertThat(report.getHighQualityIdeas()).isEqualTo(1); // Rating >= 7
        assertThat(report.getMediumQualityIdeas()).isEqualTo(1); // Rating 4-6
        assertThat(report.getLowQualityIdeas()).isEqualTo(1); // Rating <= 3
        assertThat(report.getIdeasBySource()).containsEntry("DevPost", 2L);
        assertThat(report.getIdeasBySource()).containsEntry("GitHub", 1L);
        assertThat(report.getApprovalRate()).isEqualTo(50.0); // 1 approved out of 2 reviewed
    }

    @Test
    void testCleanupOldIdeas() {
        // Given
        LocalDateTime oldDate = LocalDateTime.now().minusDays(40);
        
        StagedIdea oldMigratedIdea = createStagedIdea("Old Migrated", StagedIdea.ReviewStatus.APPROVED);
        oldMigratedIdea.setMigrationStatus(StagedIdea.MigrationStatus.MIGRATED);
        oldMigratedIdea.setScrapedAt(oldDate);
        
        when(stagedIdeaRepository.findOldMigratedIdeas(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(oldMigratedIdea));

        // When
        int cleanedUp = stagingService.cleanupOldIdeas(30);

        // Then
        assertThat(cleanedUp).isEqualTo(1);
        
        verify(stagedIdeaRepository).findOldMigratedIdeas(any(LocalDateTime.class));
        verify(stagedIdeaRepository).deleteAll(Arrays.asList(oldMigratedIdea));
    }

    @Test
    void testGetIdeasBySource() {
        // Given
        StagedIdea devPostIdea1 = createStagedIdea("DevPost 1", StagedIdea.ReviewStatus.PENDING);
        devPostIdea1.setSourceWebsite("DevPost");
        
        StagedIdea devPostIdea2 = createStagedIdea("DevPost 2", StagedIdea.ReviewStatus.APPROVED);
        devPostIdea2.setSourceWebsite("DevPost");
        
        when(stagedIdeaRepository.findBySourceWebsite("DevPost"))
                .thenReturn(Arrays.asList(devPostIdea1, devPostIdea2));

        // When
        List<StagedIdea> result = stagingService.getIdeasBySource("DevPost");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(StagedIdea::getProjectName)
                .containsExactlyInAnyOrder("DevPost 1", "DevPost 2");
        
        verify(stagedIdeaRepository).findBySourceWebsite("DevPost");
    }

    @Test
    void testGetIdeasByDateRange() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now().minusDays(1);
        
        StagedIdea ideaInRange = createStagedIdea("In Range", StagedIdea.ReviewStatus.PENDING);
        ideaInRange.setScrapedAt(LocalDateTime.now().minusDays(3));
        
        when(stagedIdeaRepository.findByScrapedAtBetween(startDate, endDate))
                .thenReturn(Arrays.asList(ideaInRange));

        // When
        List<StagedIdea> result = stagingService.getIdeasByDateRange(startDate, endDate);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProjectName()).isEqualTo("In Range");
        
        verify(stagedIdeaRepository).findByScrapedAtBetween(startDate, endDate);
    }

    @Test
    void testGetRecentIdeas() {
        // Given
        StagedIdea recentIdea = createStagedIdea("Recent", StagedIdea.ReviewStatus.PENDING);
        recentIdea.setScrapedAt(LocalDateTime.now().minusDays(2));
        
        when(stagedIdeaRepository.findRecentIdeas(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(recentIdea));

        // When
        List<StagedIdea> result = stagingService.getRecentIdeas(7);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProjectName()).isEqualTo("Recent");
        
        verify(stagedIdeaRepository).findRecentIdeas(any(LocalDateTime.class));
    }

    private StagedIdea createStagedIdea(String projectName, StagedIdea.ReviewStatus reviewStatus) {
        StagedIdea idea = new StagedIdea();
        idea.setProjectName(projectName);
        idea.setShortDescription("Description for " + projectName);
        idea.setOriginalUrl("https://example.com/" + projectName.toLowerCase().replace(" ", "-"));
        idea.setSourceWebsite("TestSource");
        idea.setScrapedAt(LocalDateTime.now().minusHours(1));
        idea.setCreatedBy("test-user");
        idea.setContentHash(projectName.toLowerCase().replace(" ", "-") + "-hash");
        idea.setReviewStatus(reviewStatus);
        idea.setMigrationStatus(StagedIdea.MigrationStatus.NOT_MIGRATED);
        return idea;
    }
}