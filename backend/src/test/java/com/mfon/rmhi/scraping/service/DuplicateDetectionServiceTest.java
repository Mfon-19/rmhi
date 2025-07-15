package com.mfon.rmhi.scraping.service;

import com.mfon.rmhi.scraping.dto.ScrapedIdea;
import com.mfon.rmhi.scraping.entity.StagedIdea;
import com.mfon.rmhi.scraping.repository.StagedIdeaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DuplicateDetectionServiceTest {

    @Mock
    private StagedIdeaRepository stagedIdeaRepository;

    @InjectMocks
    private DuplicateDetectionService duplicateDetectionService;

    private ScrapedIdea testIdea;
    private StagedIdea existingStagedIdea;

    @BeforeEach
    void setUp() {
        testIdea = ScrapedIdea.builder()
                .title("Test Project")
                .description("A test project for unit testing")
                .content("Detailed content about the test project")
                .sourceUrl("https://example.com/project/1")
                .sourceWebsite("example.com")
                .contentHash("abc123hash")
                .build();

        existingStagedIdea = new StagedIdea();
        existingStagedIdea.setId(1L);
        existingStagedIdea.setProjectName("Existing Project");
        existingStagedIdea.setShortDescription("An existing project in staging");
        existingStagedIdea.setContentHash("def456hash");
        existingStagedIdea.setScrapedAt(LocalDateTime.now().minusDays(1));
    }

    @Test
    void testIsDuplicateWithExactHashMatch() {
        // Given
        when(stagedIdeaRepository.existsByContentHash("abc123hash")).thenReturn(true);

        // When
        boolean isDuplicate = duplicateDetectionService.isDuplicate(testIdea);

        // Then
        assertTrue(isDuplicate);
        verify(stagedIdeaRepository).existsByContentHash("abc123hash");
        // Should not check for similarity when exact hash match is found
        verify(stagedIdeaRepository, never()).findRecentIdeas(any());
    }

    @Test
    void testIsDuplicateWithNoHashMatch() {
        // Given
        when(stagedIdeaRepository.existsByContentHash("abc123hash")).thenReturn(false);
        when(stagedIdeaRepository.findRecentIdeas(any())).thenReturn(List.of());

        // When
        boolean isDuplicate = duplicateDetectionService.isDuplicate(testIdea);

        // Then
        assertFalse(isDuplicate);
        verify(stagedIdeaRepository).existsByContentHash("abc123hash");
        verify(stagedIdeaRepository).findRecentIdeas(any());
    }

    @Test
    void testIsDuplicateWithSimilarContent() {
        // Given
        StagedIdea similarIdea = new StagedIdea();
        similarIdea.setProjectName("Test Project"); // Same title
        similarIdea.setShortDescription("A test project for unit testing"); // Same description
        similarIdea.setContentHash("different123hash");

        when(stagedIdeaRepository.existsByContentHash("abc123hash")).thenReturn(false);
        when(stagedIdeaRepository.findRecentIdeas(any())).thenReturn(List.of(similarIdea));

        // When
        boolean isDuplicate = duplicateDetectionService.isDuplicate(testIdea);

        // Then
        assertTrue(isDuplicate);
        verify(stagedIdeaRepository).existsByContentHash("abc123hash");
        verify(stagedIdeaRepository).findRecentIdeas(any());
    }

    @Test
    void testIsDuplicateWithDissimilarContent() {
        // Given
        StagedIdea dissimilarIdea = new StagedIdea();
        dissimilarIdea.setProjectName("Completely Different Project");
        dissimilarIdea.setShortDescription("This has nothing in common with the test");
        dissimilarIdea.setContentHash("different123hash");

        when(stagedIdeaRepository.existsByContentHash("abc123hash")).thenReturn(false);
        when(stagedIdeaRepository.findRecentIdeas(any())).thenReturn(List.of(dissimilarIdea));

        // When
        boolean isDuplicate = duplicateDetectionService.isDuplicate(testIdea);

        // Then
        assertFalse(isDuplicate);
        verify(stagedIdeaRepository).existsByContentHash("abc123hash");
        verify(stagedIdeaRepository).findRecentIdeas(any());
    }

    @Test
    void testIsDuplicateWithNullContentHash() {
        // Given
        testIdea.setContentHash(null);

        // When
        boolean isDuplicate = duplicateDetectionService.isDuplicate(testIdea);

        // Then
        assertFalse(isDuplicate);
        verify(stagedIdeaRepository, never()).existsByContentHash(any());
        verify(stagedIdeaRepository, never()).findRecentIdeas(any());
    }

    @Test
    void testFilterDuplicatesRemovesExactDuplicates() {
        // Given
        ScrapedIdea idea1 = ScrapedIdea.builder()
                .title("Project 1")
                .description("Description 1")
                .contentHash("hash1")
                .build();

        ScrapedIdea idea2 = ScrapedIdea.builder()
                .title("Project 2")
                .description("Description 2")
                .contentHash("hash2")
                .build();

        ScrapedIdea idea3 = ScrapedIdea.builder()
                .title("Project 1 Duplicate")
                .description("Description 1")
                .contentHash("hash1") // Same hash as idea1
                .build();

        List<ScrapedIdea> ideas = List.of(idea1, idea2, idea3);

        when(stagedIdeaRepository.existsByContentHash("hash1")).thenReturn(false);
        when(stagedIdeaRepository.existsByContentHash("hash2")).thenReturn(false);
        when(stagedIdeaRepository.findRecentIdeas(any())).thenReturn(List.of());

        // When
        List<ScrapedIdea> filtered = duplicateDetectionService.filterDuplicates(ideas);

        // Then
        assertEquals(2, filtered.size());
        assertTrue(filtered.contains(idea1));
        assertTrue(filtered.contains(idea2));
        assertFalse(filtered.contains(idea3)); // Duplicate should be removed
    }

    @Test
    void testFilterDuplicatesRemovesExistingDuplicates() {
        // Given
        ScrapedIdea newIdea = ScrapedIdea.builder()
                .title("New Project")
                .description("New Description")
                .contentHash("newhash")
                .build();

        ScrapedIdea existingIdea = ScrapedIdea.builder()
                .title("Existing Project")
                .description("Existing Description")
                .contentHash("existinghash")
                .build();

        List<ScrapedIdea> ideas = List.of(newIdea, existingIdea);

        when(stagedIdeaRepository.existsByContentHash("newhash")).thenReturn(false);
        when(stagedIdeaRepository.existsByContentHash("existinghash")).thenReturn(true);
        when(stagedIdeaRepository.findRecentIdeas(any())).thenReturn(List.of());

        // When
        List<ScrapedIdea> filtered = duplicateDetectionService.filterDuplicates(ideas);

        // Then
        assertEquals(1, filtered.size());
        assertTrue(filtered.contains(newIdea));
        assertFalse(filtered.contains(existingIdea)); // Existing duplicate should be removed
    }

    @Test
    void testFilterDuplicatesWithEmptyList() {
        // Given
        List<ScrapedIdea> emptyList = List.of();

        // When
        List<ScrapedIdea> filtered = duplicateDetectionService.filterDuplicates(emptyList);

        // Then
        assertTrue(filtered.isEmpty());
        verify(stagedIdeaRepository, never()).existsByContentHash(any());
        verify(stagedIdeaRepository, never()).findRecentIdeas(any());
    }

    @Test
    void testJaccardSimilarityCalculation() {
        // Test with identical content (should be detected as duplicate)
        ScrapedIdea identicalIdea = ScrapedIdea.builder()
                .title("Test Project")
                .description("A test project for unit testing")
                .contentHash("different123hash")
                .build();

        StagedIdea existingIdentical = new StagedIdea();
        existingIdentical.setProjectName("Test Project");
        existingIdentical.setShortDescription("A test project for unit testing");

        when(stagedIdeaRepository.existsByContentHash("different123hash")).thenReturn(false);
        when(stagedIdeaRepository.findRecentIdeas(any())).thenReturn(List.of(existingIdentical));

        boolean isDuplicate = duplicateDetectionService.isDuplicate(identicalIdea);
        assertTrue(isDuplicate, "Identical content should be detected as duplicate");
    }

    @Test
    void testSimilarityWithPartialOverlap() {
        // Test with partial overlap (should not be detected as duplicate)
        ScrapedIdea partialIdea = ScrapedIdea.builder()
                .title("Test Project Advanced")
                .description("A completely different description for testing")
                .contentHash("partial123hash")
                .build();

        StagedIdea existingPartial = new StagedIdea();
        existingPartial.setProjectName("Test Project Basic");
        existingPartial.setShortDescription("A test project for unit testing");

        when(stagedIdeaRepository.existsByContentHash("partial123hash")).thenReturn(false);
        when(stagedIdeaRepository.findRecentIdeas(any())).thenReturn(List.of(existingPartial));

        boolean isDuplicate = duplicateDetectionService.isDuplicate(partialIdea);
        assertFalse(isDuplicate, "Partially similar content should not be detected as duplicate");
    }

    @Test
    void testSimilarityWithNullContent() {
        // Test with null content in existing idea
        ScrapedIdea testIdeaWithContent = ScrapedIdea.builder()
                .title("Test Project")
                .description("A test project")
                .contentHash("test123hash")
                .build();

        StagedIdea existingWithNulls = new StagedIdea();
        existingWithNulls.setProjectName(null);
        existingWithNulls.setShortDescription(null);

        when(stagedIdeaRepository.existsByContentHash("test123hash")).thenReturn(false);
        when(stagedIdeaRepository.findRecentIdeas(any())).thenReturn(List.of(existingWithNulls));

        boolean isDuplicate = duplicateDetectionService.isDuplicate(testIdeaWithContent);
        assertFalse(isDuplicate, "Should handle null content gracefully");
    }
}