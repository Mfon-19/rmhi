package com.mfon.rmhi.scraping.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mfon.rmhi.scraping.dto.*;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class GeminiTransformationServiceTest {
    
    private MockWebServer mockWebServer;
    private GeminiTransformationService service;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        objectMapper = new ObjectMapper();
        
        // Create WebClient that points to mock server
        WebClient mockWebClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/v1beta/models/").toString())
                .build();
        
        // Use the test constructor that accepts WebClient directly
        MonitoringService mockMonitoringService = mock(MonitoringService.class);
        service = new GeminiTransformationService(mockWebClient, objectMapper, mockMonitoringService);
        
        // Set private fields using reflection
        ReflectionTestUtils.setField(service, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(service, "model", "gemini-2.0-flash-exp");
        ReflectionTestUtils.setField(service, "temperature", 0.7);
        ReflectionTestUtils.setField(service, "maxTokens", 1000);
    }
    
    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }
    
    @Test
    void testTransformIdea_Success() throws Exception {
        // Arrange
        ScrapedIdea scrapedIdea = createTestScrapedIdea();
        String mockResponseJson = createMockGeminiResponse();
        
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseJson)
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json"));
        
        // Act
        TransformedIdea result = service.transformIdea(scrapedIdea);
        
        // Assert
        assertNotNull(result);
        assertEquals("AI Task Manager Pro", result.getProjectName());
        assertEquals("Advanced task management with AI prioritization", result.getShortDescription());
        assertEquals("Many people struggle with task prioritization", result.getProblemDescription());
        assertEquals("AI-powered task management system", result.getSolution());
        assertEquals("Uses machine learning algorithms", result.getTechnicalDetails());
        assertEquals(8, result.getRating());
        assertEquals(Arrays.asList("React", "Node.js", "Python"), result.getTechnologies());
        assertEquals(Arrays.asList("Productivity", "AI"), result.getCategories());
        assertEquals("gemini-2.0-flash-exp", result.getTransformationModel());
        assertEquals(0.85, result.getTransformationConfidence());
        
        // Verify API call
        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("POST", request.getMethod());
        assertTrue(request.getPath().contains("gemini-2.0-flash-exp:generateContent"));
        assertTrue(request.getPath().contains("key=test-api-key"));
    }
    
    @Test
    void testTransformIdea_ApiFailure() {
        // Arrange
        ScrapedIdea scrapedIdea = createTestScrapedIdea();
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));
        
        // Act
        TransformedIdea result = service.transformIdea(scrapedIdea);
        
        // Assert
        assertNull(result);
    }
    
    @Test
    void testBatchTransform_Success() throws Exception {
        // Arrange
        List<ScrapedIdea> scrapedIdeas = Arrays.asList(
                createTestScrapedIdea(),
                createTestScrapedIdea("Another Idea", "Another description")
        );
        
        String mockResponseJson = createMockGeminiResponse();
        
        // Enqueue responses for both ideas
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseJson)
                .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseJson)
                .addHeader("Content-Type", "application/json"));
        
        // Act
        List<TransformedIdea> results = service.batchTransform(scrapedIdeas);
        
        // Assert
        assertEquals(2, results.size());
        assertNotNull(results.get(0));
        assertNotNull(results.get(1));
    }
    
    @Test
    void testBatchTransform_PartialFailure() throws Exception {
        // Arrange
        List<ScrapedIdea> scrapedIdeas = Arrays.asList(
                createTestScrapedIdea(),
                createTestScrapedIdea("Another Idea", "Another description")
        );
        
        String mockResponseJson = createMockGeminiResponse();
        
        // First request succeeds, second fails
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseJson)
                .addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));
        
        // Act
        List<TransformedIdea> results = service.batchTransform(scrapedIdeas);
        
        // Assert
        assertEquals(1, results.size());
        assertNotNull(results.get(0));
    }
    
    @Test
    void testValidateTransformation_ValidIdea() {
        // Arrange
        TransformedIdea validIdea = TransformedIdea.builder()
                .projectName("Test Project")
                .shortDescription("Test description")
                .solution("Test solution")
                .rating(8)
                .technologies(Arrays.asList("Java", "Spring"))
                .categories(Arrays.asList("Web", "Backend"))
                .build();
        
        // Act & Assert
        assertTrue(service.validateTransformation(validIdea));
    }
    
    @Test
    void testValidateTransformation_InvalidRating() {
        // Arrange
        TransformedIdea invalidIdea = TransformedIdea.builder()
                .projectName("Test Project")
                .shortDescription("Test description")
                .solution("Test solution")
                .rating(11) // Invalid rating > 10
                .build();
        
        // Act & Assert
        assertFalse(service.validateTransformation(invalidIdea));
    }
    
    @Test
    void testValidateTransformation_TooManyTechnologies() {
        // Arrange
        TransformedIdea invalidIdea = TransformedIdea.builder()
                .projectName("Test Project")
                .shortDescription("Test description")
                .solution("Test solution")
                .rating(8)
                .technologies(Arrays.asList("Java", "Spring", "React", "Node.js", "Python", "Docker", "Kubernetes", "MongoDB"))
                .build();
        
        // Act & Assert
        assertFalse(service.validateTransformation(invalidIdea));
    }
    
    @Test
    void testValidateTransformation_TooManyCategories() {
        // Arrange
        TransformedIdea invalidIdea = TransformedIdea.builder()
                .projectName("Test Project")
                .shortDescription("Test description")
                .solution("Test solution")
                .rating(8)
                .categories(Arrays.asList("Web", "Backend", "AI", "Mobile", "Desktop", "IoT"))
                .build();
        
        // Act & Assert
        assertFalse(service.validateTransformation(invalidIdea));
    }
    
    @Test
    void testValidateTransformation_MissingRequiredFields() {
        // Arrange
        TransformedIdea invalidIdea = TransformedIdea.builder()
                .projectName("Test Project")
                // Missing shortDescription and solution
                .rating(8)
                .build();
        
        // Act & Assert
        assertFalse(service.validateTransformation(invalidIdea));
    }
    
    @Test
    void testValidateTransformation_NullIdea() {
        // Act & Assert
        assertFalse(service.validateTransformation(null));
    }
    
    private ScrapedIdea createTestScrapedIdea() {
        return createTestScrapedIdea("Test Idea", "Test description");
    }
    
    private ScrapedIdea createTestScrapedIdea(String title, String description) {
        return ScrapedIdea.builder()
                .sourceUrl("https://example.com/idea/1")
                .sourceWebsite("example.com")
                .scrapedAt(LocalDateTime.now())
                .title(title)
                .description(description)
                .content("Detailed content about the idea")
                .author("Test Author")
                .likes(10)
                .rating(7)
                .technologies(Arrays.asList("Java", "Spring Boot"))
                .categories(Arrays.asList("Web Development"))
                .contentHash("abc123")
                .build();
    }
    
    private String createMockGeminiResponse() throws Exception {
        GeminiTransformationResult transformationResult = new GeminiTransformationResult();
        transformationResult.setProjectName("AI Task Manager Pro");
        transformationResult.setShortDescription("Advanced task management with AI prioritization");
        transformationResult.setProblemDescription("Many people struggle with task prioritization");
        transformationResult.setSolution("AI-powered task management system");
        transformationResult.setTechnicalDetails("Uses machine learning algorithms");
        transformationResult.setTechnologies(Arrays.asList("React", "Node.js", "Python"));
        transformationResult.setCategories(Arrays.asList("Productivity", "AI"));
        transformationResult.setRating(8);
        transformationResult.setTransformationConfidence(0.85);
        
        String transformationJson = objectMapper.writeValueAsString(transformationResult);
        
        GeminiResponse.Part part = new GeminiResponse.Part();
        part.setText(transformationJson);
        
        GeminiResponse.Content content = new GeminiResponse.Content();
        content.setParts(Arrays.asList(part));
        
        GeminiResponse.Candidate candidate = new GeminiResponse.Candidate();
        candidate.setContent(content);
        candidate.setFinishReason("STOP");
        candidate.setIndex(0);
        
        GeminiResponse response = new GeminiResponse();
        response.setCandidates(Arrays.asList(candidate));
        
        return objectMapper.writeValueAsString(response);
    }
}