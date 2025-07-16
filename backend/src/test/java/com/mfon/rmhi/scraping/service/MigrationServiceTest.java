package com.mfon.rmhi.scraping.service;

import com.mfon.rmhi.model.Category;
import com.mfon.rmhi.model.Idea;
import com.mfon.rmhi.model.Technology;
import com.mfon.rmhi.repository.CategoryRepository;
import com.mfon.rmhi.repository.IdeaRepository;
import com.mfon.rmhi.repository.TechnologyRepository;
import com.mfon.rmhi.scraping.dto.MigrationResult;
import com.mfon.rmhi.scraping.dto.MigrationStatus;
import com.mfon.rmhi.scraping.entity.StagedIdea;
import com.mfon.rmhi.scraping.repository.StagedIdeaRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MigrationServiceTest {

    @Mock
    private StagedIdeaRepository stagedIdeaRepository;

    @Mock
    private IdeaRepository ideaRepository;

    @Mock
    private TechnologyRepository technologyRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private MigrationServiceImpl migrationService;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(stagedIdeaRepository, ideaRepository, technologyRepository, categoryRepository);
        
        // Set default values for @Value fields using reflection
        try {
            java.lang.reflect.Field batchSizeField = MigrationServiceImpl.class.getDeclaredField("defaultBatchSize");
            batchSizeField.setAccessible(true);
            batchSizeField.set(migrationService, 50);
            
            java.lang.reflect.Field rollbackField = MigrationServiceImpl.class.getDeclaredField("rollbackEnabled");
            rollbackField.setAccessible(true);
            rollbackField.set(migrationService, true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field values", e);
        }
    }

    @Test
    void testMigrateApprovedIdeas_Success() {
        // Given
        StagedIdea approvedIdea = createStagedIdea("Test Project", "test@example.com", 
                StagedIdea.ReviewStatus.APPROVED, StagedIdea.MigrationStatus.NOT_MIGRATED);
        approvedIdea.setId(1L);
        approvedIdea.setTechnologies(new String[]{"Java", "Spring Boot"});
        approvedIdea.setCategories(new String[]{"Web Development", "Backend"});

        // Mock repository calls
        when(stagedIdeaRepository.findApprovedForMigration()).thenReturn(List.of(approvedIdea));
        when(ideaRepository.existsByProjectNameAndCreatedByIgnoreCase(anyString(), anyString())).thenReturn(false);
        
        Technology javaTech = new Technology("Java");
        javaTech.setId(1);
        Technology springTech = new Technology("Spring Boot");
        springTech.setId(2);
        when(technologyRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.empty());
        when(technologyRepository.findByNameIgnoreCase("Spring Boot")).thenReturn(Optional.empty());
        when(technologyRepository.save(any(Technology.class))).thenReturn(javaTech, springTech);
        
        Category webCategory = new Category("Web Development");
        webCategory.setId(1);
        Category backendCategory = new Category("Backend");
        backendCategory.setId(2);
        when(categoryRepository.findByNameIgnoreCase("Web Development")).thenReturn(Optional.empty());
        when(categoryRepository.findByNameIgnoreCase("Backend")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(webCategory, backendCategory);
        
        Idea savedIdea = new Idea();
        savedIdea.setId(1L);
        savedIdea.setProjectName("Test Project");
        when(ideaRepository.save(any(Idea.class))).thenReturn(savedIdea);
        when(stagedIdeaRepository.save(any(StagedIdea.class))).thenReturn(approvedIdea);

        // When
        MigrationResult result = migrationService.migrateApprovedIdeas();

        // Then
        assertTrue(result.isSuccessful());
        assertEquals(1, result.getIdeasMigrated());
        assertEquals(0, result.getIdeasSkipped());
        assertEquals(0, result.getIdeasFailed());
        assertNotNull(result.getMigrationId());
        assertNotNull(result.getStartedAt());
        assertNotNull(result.getCompletedAt());

        // Verify repository interactions
        verify(stagedIdeaRepository).findApprovedForMigration();
        verify(ideaRepository).existsByProjectNameAndCreatedByIgnoreCase("Test Project", "test@example.com");
        verify(ideaRepository).save(any(Idea.class));
        verify(stagedIdeaRepository).save(any(StagedIdea.class));
        verify(technologyRepository, times(2)).save(any(Technology.class));
        verify(categoryRepository, times(2)).save(any(Category.class));
    }

    @Test
    void testMigrateApprovedIdeas_SkipDuplicates() {
        // Given
        StagedIdea duplicateIdea = createStagedIdea("Duplicate Project", "test@example.com", 
                StagedIdea.ReviewStatus.APPROVED, StagedIdea.MigrationStatus.NOT_MIGRATED);
        duplicateIdea.setId(1L);

        // Mock repository calls - simulate duplicate exists
        when(stagedIdeaRepository.findApprovedForMigration()).thenReturn(List.of(duplicateIdea));
        when(ideaRepository.existsByProjectNameAndCreatedByIgnoreCase("Duplicate Project", "test@example.com")).thenReturn(true);

        // When
        MigrationResult result = migrationService.migrateApprovedIdeas();

        // Then
        assertTrue(result.isSuccessful());
        assertEquals(0, result.getIdeasMigrated());
        assertEquals(1, result.getIdeasSkipped());
        assertEquals(0, result.getIdeasFailed());
        assertEquals(1, result.getWarnings().size());
        assertTrue(result.getWarnings().get(0).contains("Skipped duplicate idea"));

        // Verify no production idea was created
        verify(ideaRepository, never()).save(any(Idea.class));
    }

    @Test
    void testMigrateApprovedIdeas_OnlyMigratesApproved() {
        // Given - Only approved ideas should be returned by the repository query
        StagedIdea approvedIdea = createStagedIdea("Approved Project", "approved@example.com", 
                StagedIdea.ReviewStatus.APPROVED, StagedIdea.MigrationStatus.NOT_MIGRATED);
        approvedIdea.setId(1L);

        // Mock repository calls - only approved ideas are returned
        when(stagedIdeaRepository.findApprovedForMigration()).thenReturn(List.of(approvedIdea));
        when(ideaRepository.existsByProjectNameAndCreatedByIgnoreCase(anyString(), anyString())).thenReturn(false);
        
        Idea savedIdea = new Idea();
        savedIdea.setId(1L);
        savedIdea.setProjectName("Approved Project");
        when(ideaRepository.save(any(Idea.class))).thenReturn(savedIdea);
        when(stagedIdeaRepository.save(any(StagedIdea.class))).thenReturn(approvedIdea);

        // When
        MigrationResult result = migrationService.migrateApprovedIdeas();

        // Then
        assertTrue(result.isSuccessful());
        assertEquals(1, result.getIdeasMigrated());
        assertEquals(0, result.getIdeasSkipped());
        assertEquals(0, result.getIdeasFailed());

        // Verify repository interactions
        verify(stagedIdeaRepository).findApprovedForMigration();
        verify(ideaRepository).save(any(Idea.class));
    }

    @Test
    void testMigrateApprovedIdeas_BatchProcessing() {
        // Given - Create multiple approved ideas
        List<StagedIdea> approvedIdeas = Arrays.asList(
            createStagedIdea("Project 1", "user1@example.com", StagedIdea.ReviewStatus.APPROVED, StagedIdea.MigrationStatus.NOT_MIGRATED),
            createStagedIdea("Project 2", "user2@example.com", StagedIdea.ReviewStatus.APPROVED, StagedIdea.MigrationStatus.NOT_MIGRATED),
            createStagedIdea("Project 3", "user3@example.com", StagedIdea.ReviewStatus.APPROVED, StagedIdea.MigrationStatus.NOT_MIGRATED),
            createStagedIdea("Project 4", "user4@example.com", StagedIdea.ReviewStatus.APPROVED, StagedIdea.MigrationStatus.NOT_MIGRATED),
            createStagedIdea("Project 5", "user5@example.com", StagedIdea.ReviewStatus.APPROVED, StagedIdea.MigrationStatus.NOT_MIGRATED)
        );
        
        // Set IDs for the staged ideas
        for (int i = 0; i < approvedIdeas.size(); i++) {
            approvedIdeas.get(i).setId((long) (i + 1));
        }

        // Mock repository calls
        when(stagedIdeaRepository.findApprovedForMigration()).thenReturn(approvedIdeas);
        when(ideaRepository.existsByProjectNameAndCreatedByIgnoreCase(anyString(), anyString())).thenReturn(false);
        
        // Mock idea saves
        when(ideaRepository.save(any(Idea.class))).thenAnswer(invocation -> {
            Idea idea = invocation.getArgument(0);
            idea.setId(1L); // Set a mock ID
            return idea;
        });
        when(stagedIdeaRepository.save(any(StagedIdea.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When - Use small batch size
        MigrationResult result = migrationService.migrateApprovedIdeas(2);

        // Then
        assertTrue(result.isSuccessful());
        assertEquals(5, result.getIdeasMigrated());
        assertEquals(0, result.getIdeasSkipped());
        assertEquals(0, result.getIdeasFailed());

        // Verify all ideas were processed
        verify(ideaRepository, times(5)).save(any(Idea.class));
        verify(stagedIdeaRepository, times(5)).save(any(StagedIdea.class));
    }

    @Test
    void testMigrateSpecificIdeas() {
        // Given
        StagedIdea idea1 = createStagedIdea("Project 1", "user1@example.com", 
                StagedIdea.ReviewStatus.APPROVED, StagedIdea.MigrationStatus.NOT_MIGRATED);
        idea1.setId(1L);
        StagedIdea idea2 = createStagedIdea("Project 2", "user2@example.com", 
                StagedIdea.ReviewStatus.APPROVED, StagedIdea.MigrationStatus.NOT_MIGRATED);
        idea2.setId(2L);
        StagedIdea idea3 = createStagedIdea("Project 3", "user3@example.com", 
                StagedIdea.ReviewStatus.PENDING, StagedIdea.MigrationStatus.NOT_MIGRATED);
        idea3.setId(3L);
        
        List<Long> idsToMigrate = Arrays.asList(1L, 2L, 3L);
        
        // Mock repository calls
        when(stagedIdeaRepository.findAllById(idsToMigrate)).thenReturn(Arrays.asList(idea1, idea2, idea3));
        when(ideaRepository.existsByProjectNameAndCreatedByIgnoreCase(anyString(), anyString())).thenReturn(false);
        
        // Mock idea saves
        when(ideaRepository.save(any(Idea.class))).thenAnswer(invocation -> {
            Idea idea = invocation.getArgument(0);
            idea.setId(1L); // Set a mock ID
            return idea;
        });
        when(stagedIdeaRepository.save(any(StagedIdea.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When - Migrate specific ideas (including one that's not approved)
        MigrationResult result = migrationService.migrateSpecificIdeas(idsToMigrate);

        // Then
        assertTrue(result.isSuccessful());
        assertEquals(2, result.getIdeasMigrated()); // Only approved ones
        assertEquals(0, result.getIdeasSkipped());
        assertEquals(0, result.getIdeasFailed());
        assertEquals(1, result.getWarnings().size()); // Warning about non-approved idea

        // Verify only approved ideas were processed
        verify(ideaRepository, times(2)).save(any(Idea.class));
        verify(stagedIdeaRepository, times(2)).save(any(StagedIdea.class));
    }

    @Test
    void testRollbackMigration() {
        // Given - Perform a migration first
        StagedIdea approvedIdea = createStagedIdea("Test Project", "test@example.com", 
                StagedIdea.ReviewStatus.APPROVED, StagedIdea.MigrationStatus.NOT_MIGRATED);
        approvedIdea.setId(1L);

        // Mock repository calls for migration
        when(stagedIdeaRepository.findApprovedForMigration()).thenReturn(List.of(approvedIdea));
        when(ideaRepository.existsByProjectNameAndCreatedByIgnoreCase(anyString(), anyString())).thenReturn(false);
        
        Idea savedIdea = new Idea();
        savedIdea.setId(1L);
        savedIdea.setProjectName("Test Project");
        when(ideaRepository.save(any(Idea.class))).thenReturn(savedIdea);
        when(stagedIdeaRepository.save(any(StagedIdea.class))).thenReturn(approvedIdea);

        MigrationResult migrationResult = migrationService.migrateApprovedIdeas();
        assertTrue(migrationResult.isSuccessful());

        // Mock repository calls for rollback
        when(stagedIdeaRepository.findAllById(anyList())).thenReturn(List.of(approvedIdea));

        // When - Rollback the migration
        boolean rollbackSuccess = migrationService.rollbackMigration(migrationResult.getMigrationId());

        // Then
        assertTrue(rollbackSuccess);
        
        // Verify rollback operations
        verify(ideaRepository).deleteById(1L);
        verify(stagedIdeaRepository, times(2)).save(any(StagedIdea.class)); // Once for migration, once for rollback
    }

    @Test
    void testGetMigrationStatus() {
        // Given
        StagedIdea approvedIdea = createStagedIdea("Test Project", "test@example.com", 
                StagedIdea.ReviewStatus.APPROVED, StagedIdea.MigrationStatus.NOT_MIGRATED);
        approvedIdea.setId(1L);

        // Mock repository calls
        when(stagedIdeaRepository.findApprovedForMigration()).thenReturn(List.of(approvedIdea));
        when(ideaRepository.existsByProjectNameAndCreatedByIgnoreCase(anyString(), anyString())).thenReturn(false);
        
        Idea savedIdea = new Idea();
        savedIdea.setId(1L);
        when(ideaRepository.save(any(Idea.class))).thenReturn(savedIdea);
        when(stagedIdeaRepository.save(any(StagedIdea.class))).thenReturn(approvedIdea);

        // When - Perform migration
        MigrationResult result = migrationService.migrateApprovedIdeas();
        
        // Then
        MigrationStatus status = migrationService.getMigrationStatus(result.getMigrationId());
        assertEquals(MigrationStatus.COMPLETED, status);

        // Test non-existent migration
        MigrationStatus nonExistentStatus = migrationService.getMigrationStatus("NON_EXISTENT");
        assertEquals(MigrationStatus.NOT_STARTED, nonExistentStatus);
    }

    @Test
    void testTechnologyAndCategoryCreation() {
        // Given
        StagedIdea approvedIdea = createStagedIdea("Tech Project", "tech@example.com", 
                StagedIdea.ReviewStatus.APPROVED, StagedIdea.MigrationStatus.NOT_MIGRATED);
        approvedIdea.setId(1L);
        approvedIdea.setTechnologies(new String[]{"New Tech", "Another Tech"});
        approvedIdea.setCategories(new String[]{"New Category", "Another Category"});

        // Mock repository calls
        when(stagedIdeaRepository.findApprovedForMigration()).thenReturn(List.of(approvedIdea));
        when(ideaRepository.existsByProjectNameAndCreatedByIgnoreCase(anyString(), anyString())).thenReturn(false);
        
        // Mock technology creation
        when(technologyRepository.findByNameIgnoreCase("New Tech")).thenReturn(Optional.empty());
        when(technologyRepository.findByNameIgnoreCase("Another Tech")).thenReturn(Optional.empty());
        Technology newTech = new Technology("New Tech");
        newTech.setId(1);
        Technology anotherTech = new Technology("Another Tech");
        anotherTech.setId(2);
        when(technologyRepository.save(any(Technology.class))).thenReturn(newTech, anotherTech);
        
        // Mock category creation
        when(categoryRepository.findByNameIgnoreCase("New Category")).thenReturn(Optional.empty());
        when(categoryRepository.findByNameIgnoreCase("Another Category")).thenReturn(Optional.empty());
        Category newCategory = new Category("New Category");
        newCategory.setId(1);
        Category anotherCategory = new Category("Another Category");
        anotherCategory.setId(2);
        when(categoryRepository.save(any(Category.class))).thenReturn(newCategory, anotherCategory);
        
        Idea savedIdea = new Idea();
        savedIdea.setId(1L);
        when(ideaRepository.save(any(Idea.class))).thenReturn(savedIdea);
        when(stagedIdeaRepository.save(any(StagedIdea.class))).thenReturn(approvedIdea);

        // When
        MigrationResult result = migrationService.migrateApprovedIdeas();

        // Then
        assertTrue(result.isSuccessful());
        
        // Verify technologies and categories were created
        verify(technologyRepository, times(2)).save(any(Technology.class));
        verify(categoryRepository, times(2)).save(any(Category.class));
        verify(technologyRepository).findByNameIgnoreCase("New Tech");
        verify(technologyRepository).findByNameIgnoreCase("Another Tech");
        verify(categoryRepository).findByNameIgnoreCase("New Category");
        verify(categoryRepository).findByNameIgnoreCase("Another Category");
    }

    @Test
    void testTechnologyAndCategoryReuse() {
        // Given
        StagedIdea approvedIdea = createStagedIdea("Reuse Project", "reuse@example.com", 
                StagedIdea.ReviewStatus.APPROVED, StagedIdea.MigrationStatus.NOT_MIGRATED);
        approvedIdea.setId(1L);
        approvedIdea.setTechnologies(new String[]{"Existing Tech", "New Tech"});
        approvedIdea.setCategories(new String[]{"Existing Category", "New Category"});

        // Mock repository calls
        when(stagedIdeaRepository.findApprovedForMigration()).thenReturn(List.of(approvedIdea));
        when(ideaRepository.existsByProjectNameAndCreatedByIgnoreCase(anyString(), anyString())).thenReturn(false);
        
        // Mock existing technology and category
        Technology existingTech = new Technology("Existing Tech");
        existingTech.setId(1);
        when(technologyRepository.findByNameIgnoreCase("Existing Tech")).thenReturn(Optional.of(existingTech));
        
        Technology newTech = new Technology("New Tech");
        newTech.setId(2);
        when(technologyRepository.findByNameIgnoreCase("New Tech")).thenReturn(Optional.empty());
        when(technologyRepository.save(any(Technology.class))).thenReturn(newTech);
        
        Category existingCategory = new Category("Existing Category");
        existingCategory.setId(1);
        when(categoryRepository.findByNameIgnoreCase("Existing Category")).thenReturn(Optional.of(existingCategory));
        
        Category newCategory = new Category("New Category");
        newCategory.setId(2);
        when(categoryRepository.findByNameIgnoreCase("New Category")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(newCategory);
        
        Idea savedIdea = new Idea();
        savedIdea.setId(1L);
        when(ideaRepository.save(any(Idea.class))).thenReturn(savedIdea);
        when(stagedIdeaRepository.save(any(StagedIdea.class))).thenReturn(approvedIdea);

        // When
        MigrationResult result = migrationService.migrateApprovedIdeas();

        // Then
        assertTrue(result.isSuccessful());
        
        // Verify existing entities were reused and new ones created
        verify(technologyRepository).findByNameIgnoreCase("Existing Tech");
        verify(technologyRepository).findByNameIgnoreCase("New Tech");
        verify(technologyRepository, times(1)).save(any(Technology.class)); // Only new tech saved
        
        verify(categoryRepository).findByNameIgnoreCase("Existing Category");
        verify(categoryRepository).findByNameIgnoreCase("New Category");
        verify(categoryRepository, times(1)).save(any(Category.class)); // Only new category saved
    }

    @Test
    void testEmptyMigration() {
        // Given - No approved ideas
        when(stagedIdeaRepository.findApprovedForMigration()).thenReturn(List.of());

        // When
        MigrationResult result = migrationService.migrateApprovedIdeas();

        // Then
        assertTrue(result.isSuccessful());
        assertEquals(0, result.getIdeasMigrated());
        assertEquals(0, result.getIdeasSkipped());
        assertEquals(0, result.getIdeasFailed());
        assertNotNull(result.getMigrationId());
        
        // Verify no repository interactions for saving
        verify(ideaRepository, never()).save(any(Idea.class));
        verify(stagedIdeaRepository, never()).save(any(StagedIdea.class));
    }

    @Test
    void testGetAllMigrationResults() {
        // Given - Perform multiple migrations
        StagedIdea idea1 = createStagedIdea("Project 1", "user1@example.com", 
                StagedIdea.ReviewStatus.APPROVED, StagedIdea.MigrationStatus.NOT_MIGRATED);
        idea1.setId(1L);
        
        StagedIdea idea2 = createStagedIdea("Project 2", "user2@example.com", 
                StagedIdea.ReviewStatus.APPROVED, StagedIdea.MigrationStatus.NOT_MIGRATED);
        idea2.setId(2L);

        // Mock first migration
        when(stagedIdeaRepository.findApprovedForMigration()).thenReturn(List.of(idea1));
        when(ideaRepository.existsByProjectNameAndCreatedByIgnoreCase(anyString(), anyString())).thenReturn(false);
        when(ideaRepository.save(any(Idea.class))).thenAnswer(invocation -> {
            Idea idea = invocation.getArgument(0);
            idea.setId(1L);
            return idea;
        });
        when(stagedIdeaRepository.save(any(StagedIdea.class))).thenReturn(idea1);
        
        MigrationResult result1 = migrationService.migrateApprovedIdeas();

        // Mock second migration
        when(stagedIdeaRepository.findApprovedForMigration()).thenReturn(List.of(idea2));
        when(stagedIdeaRepository.save(any(StagedIdea.class))).thenReturn(idea2);
        
        MigrationResult result2 = migrationService.migrateApprovedIdeas();

        // When
        List<MigrationResult> allResults = migrationService.getAllMigrationResults();

        // Then
        assertEquals(2, allResults.size());
        assertTrue(allResults.stream().anyMatch(r -> r.getMigrationId().equals(result1.getMigrationId())));
        assertTrue(allResults.stream().anyMatch(r -> r.getMigrationId().equals(result2.getMigrationId())));
    }

    private StagedIdea createStagedIdea(String projectName, String createdBy, 
                                       StagedIdea.ReviewStatus reviewStatus, 
                                       StagedIdea.MigrationStatus migrationStatus) {
        StagedIdea idea = new StagedIdea();
        idea.setProjectName(projectName);
        idea.setCreatedBy(createdBy);
        idea.setShortDescription("Short description for " + projectName);
        idea.setSolution("Solution for " + projectName);
        idea.setProblemDescription("Problem description for " + projectName);
        idea.setTechnicalDetails("Technical details for " + projectName);
        idea.setOriginalUrl("https://example.com/" + projectName.toLowerCase().replace(" ", "-"));
        idea.setSourceWebsite("example.com");
        idea.setScrapedAt(LocalDateTime.now().minusDays(1));
        idea.setTransformedAt(LocalDateTime.now().minusDays(1));
        idea.setOriginalData(java.util.Map.of("original", "data"));
        idea.setLikes(0);
        idea.setRating(8);
        idea.setReviewStatus(reviewStatus);
        idea.setMigrationStatus(migrationStatus);
        idea.setContentHash("hash_" + projectName.hashCode());
        
        if (reviewStatus == StagedIdea.ReviewStatus.APPROVED) {
            idea.setReviewedBy("reviewer@example.com");
            idea.setReviewedAt(LocalDateTime.now().minusHours(1));
        }
        
        return idea;
    }
}