package com.mfon.rmhi.scraping.repository;

import com.mfon.rmhi.scraping.entity.StagedIdea;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class StagedIdeaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private StagedIdeaRepository stagedIdeaRepository;

    @Test
    void testFindByReviewStatus() {
        // Given
        StagedIdea pendingIdea = createTestStagedIdea("https://example.com/1", StagedIdea.ReviewStatus.PENDING);
        StagedIdea approvedIdea = createTestStagedIdea("https://example.com/2", StagedIdea.ReviewStatus.APPROVED);
        
        entityManager.persistAndFlush(pendingIdea);
        entityManager.persistAndFlush(approvedIdea);

        // When
        List<StagedIdea> pendingIdeas = stagedIdeaRepository.findByReviewStatus(StagedIdea.ReviewStatus.PENDING);
        List<StagedIdea> approvedIdeas = stagedIdeaRepository.findByReviewStatus(StagedIdea.ReviewStatus.APPROVED);

        // Then
        assertThat(pendingIdeas).hasSize(1);
        assertThat(pendingIdeas.get(0).getOriginalUrl()).isEqualTo("https://example.com/1");
        
        assertThat(approvedIdeas).hasSize(1);
        assertThat(approvedIdeas.get(0).getOriginalUrl()).isEqualTo("https://example.com/2");
    }

    @Test
    void testFindPendingReview() {
        // Given
        StagedIdea pendingIdea = createTestStagedIdea("https://example.com/pending", StagedIdea.ReviewStatus.PENDING);
        StagedIdea approvedIdea = createTestStagedIdea("https://example.com/approved", StagedIdea.ReviewStatus.APPROVED);
        
        entityManager.persistAndFlush(pendingIdea);
        entityManager.persistAndFlush(approvedIdea);

        // When
        List<StagedIdea> pendingIdeas = stagedIdeaRepository.findPendingReview();

        // Then
        assertThat(pendingIdeas).hasSize(1);
        assertThat(pendingIdeas.get(0).getReviewStatus()).isEqualTo(StagedIdea.ReviewStatus.PENDING);
    }

    @Test
    void testExistsByContentHash() {
        // Given
        StagedIdea idea = createTestStagedIdea("https://example.com/test", StagedIdea.ReviewStatus.PENDING);
        idea.setContentHash("test-hash-123");
        entityManager.persistAndFlush(idea);

        // When & Then
        assertThat(stagedIdeaRepository.existsByContentHash("test-hash-123")).isTrue();
        assertThat(stagedIdeaRepository.existsByContentHash("non-existent-hash")).isFalse();
    }

    private StagedIdea createTestStagedIdea(String url, StagedIdea.ReviewStatus reviewStatus) {
        StagedIdea idea = new StagedIdea();
        idea.setOriginalUrl(url);
        idea.setSourceWebsite("TestSite");
        idea.setScrapedAt(LocalDateTime.now());
        idea.setProjectName("Test Project");
        idea.setCreatedBy("test-user");
        idea.setReviewStatus(reviewStatus);
        idea.setMigrationStatus(StagedIdea.MigrationStatus.NOT_MIGRATED);
        
        // Create test original data
        Map<String, Object> originalData = new HashMap<>();
        originalData.put("title", "Test Project");
        originalData.put("description", "Test Description");
        idea.setOriginalData(originalData);
        
        return idea;
    }
}