package com.mfon.rmhi.scraping.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mfon.rmhi.scraping.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

/**
 * Implementation of AI transformation service using Google Gemini API
 */
@Service
@Slf4j
public class GeminiTransformationService implements AITransformationService {
    
    private static final String GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final int MAX_CONCURRENT_REQUESTS = 8;
    private static final int MAX_TECHNOLOGIES = 7;
    private static final int MAX_CATEGORIES = 5;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final Semaphore concurrencyLimiter;
    private final MonitoringService monitoringService;
    
    @Value("${scraping.ai-transformation.api-key}")
    private String apiKey;
    
    @Value("${scraping.ai-transformation.model:gemini-2.5-pro}")
    private String model;
    
    @Value("${scraping.ai-transformation.temperature:0.7}")
    private Double temperature;
    
    @Value("${scraping.ai-transformation.max-tokens:1000}")
    private Integer maxTokens;
    
    public GeminiTransformationService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper, MonitoringService monitoringService) {
        this.webClient = webClientBuilder
                .baseUrl(GEMINI_API_BASE_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = objectMapper;
        this.concurrencyLimiter = new Semaphore(MAX_CONCURRENT_REQUESTS);
        this.monitoringService = monitoringService;
    }
    
    // Constructor for testing with custom WebClient
    public GeminiTransformationService(WebClient webClient, ObjectMapper objectMapper, MonitoringService monitoringService) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.concurrencyLimiter = new Semaphore(MAX_CONCURRENT_REQUESTS);
        this.monitoringService = monitoringService;
    }
    
    @Override
    public TransformedIdea transformIdea(ScrapedIdea originalIdea) {
        log.debug("Transforming single idea: {}", originalIdea.getTitle());
        
        try {
            GeminiTransformationResult result = callGeminiAPI(originalIdea).block();
            return mapToTransformedIdea(originalIdea, result);
        } catch (Exception e) {
            log.error("Failed to transform idea: {}", originalIdea.getTitle(), e);
            return null;
        }
    }
    
    @Override
    public List<TransformedIdea> batchTransform(List<ScrapedIdea> ideas) {
        String operationId = "batch-transform-" + System.currentTimeMillis();
        long startTime = System.currentTimeMillis();
        
        monitoringService.recordOperationStart("AI_TRANSFORMATION", operationId, 
                String.format("Starting batch transformation of %d ideas", ideas.size()));
        
        try {
            List<CompletableFuture<TransformedIdea>> futures = ideas.stream()
                    .map(this::transformIdeaAsync)
                    .collect(Collectors.toList());
            
            List<TransformedIdea> results = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            long durationMs = System.currentTimeMillis() - startTime;
            
            // Record performance metrics
            recordTransformationMetrics(operationId, ideas.size(), results.size(), durationMs);
            
            boolean allSuccessful = results.size() == ideas.size();
            monitoringService.recordOperationComplete("AI_TRANSFORMATION", operationId, durationMs, allSuccessful);
            
            log.info("Batch transformation completed. Successfully transformed {}/{} ideas", 
                    results.size(), ideas.size());
            
            return results;
            
        } catch (Exception e) {
            monitoringService.recordError("AI_TRANSFORMATION", operationId, e.getMessage(), e, 
                    String.format("Failed during batch transformation of %d ideas", ideas.size()));
            throw e;
        }
    }
    
    @Override
    public boolean validateTransformation(TransformedIdea transformed) {
        if (transformed == null) {
            return false;
        }
        
        // Validate required fields
        if (isBlank(transformed.getProjectName()) || 
            isBlank(transformed.getShortDescription()) ||
            isBlank(transformed.getSolution()) ||
            transformed.getRating() == null ||
            transformed.getRating() < 1 || 
            transformed.getRating() > 10) {
            return false;
        }
        
        // Validate technologies and categories limits
        if (transformed.getTechnologies() != null && transformed.getTechnologies().size() > MAX_TECHNOLOGIES) {
            return false;
        }
        
        if (transformed.getCategories() != null && transformed.getCategories().size() > MAX_CATEGORIES) {
            return false;
        }
        
        return true;
    }
    
    private CompletableFuture<TransformedIdea> transformIdeaAsync(ScrapedIdea originalIdea) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                concurrencyLimiter.acquire();
                log.debug("Transforming idea: {}", originalIdea.getTitle());
                
                GeminiTransformationResult result = callGeminiAPI(originalIdea).block();
                return mapToTransformedIdea(originalIdea, result);
                
            } catch (Exception e) {
                log.error("Failed to transform idea: {}", originalIdea.getTitle(), e);
                return null;
            } finally {
                concurrencyLimiter.release();
            }
        });
    }
    
    private Mono<GeminiTransformationResult> callGeminiAPI(ScrapedIdea idea) {
        String prompt = buildTransformationPrompt(idea);
        GeminiRequest request = buildGeminiRequest(prompt);
        
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(model + ":generateContent")
                        .queryParam("key", apiKey)
                        .build())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GeminiResponse.class)
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                        .filter(this::isRetryableException))
                .map(this::parseGeminiResponse)
                .onErrorResume(e -> {
                    log.error("Gemini API call failed for idea: {}", idea.getTitle(), e);
                    return Mono.empty();
                });
    }
    
    private String buildTransformationPrompt(ScrapedIdea idea) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Transform the following project idea into a fresh, unique variation while preserving its core purpose and value.\n\n");
        prompt.append("Original Idea:\n");
        prompt.append("Title: ").append(idea.getTitle()).append("\n");
        prompt.append("Description: ").append(idea.getDescription()).append("\n");
        
        if (idea.getContent() != null && !idea.getContent().trim().isEmpty()) {
            prompt.append("Content: ").append(idea.getContent()).append("\n");
        }
        
        if (idea.getTechnologies() != null && !idea.getTechnologies().isEmpty()) {
            prompt.append("Technologies: ").append(String.join(", ", idea.getTechnologies())).append("\n");
        }
        
        if (idea.getCategories() != null && !idea.getCategories().isEmpty()) {
            prompt.append("Categories: ").append(String.join(", ", idea.getCategories())).append("\n");
        }
        
        prompt.append("\nInstructions:\n");
        prompt.append("1. Extract the core essence and purpose of this idea\n");
        prompt.append("2. Generate a fresh variation that solves the same problem but with a different approach\n");
        prompt.append("3. Ensure the variation is unique and not a direct copy\n");
        prompt.append("4. Rate the quality and innovation of your variation on a scale of 1-10\n");
        prompt.append("5. Limit technologies to maximum 7 items\n");
        prompt.append("6. Limit categories to maximum 5 items\n");
        prompt.append("7. Provide a confidence score (0.0-1.0) for your transformation\n\n");
        prompt.append("Generate a creative, implementable project idea that developers would find interesting and valuable.");
        
        return prompt.toString();
    }
    
    private GeminiRequest buildGeminiRequest(String prompt) {
        Map<String, Object> responseSchema = buildResponseSchema();
        
        return GeminiRequest.builder()
                .contents(List.of(
                        GeminiRequest.Content.builder()
                                .parts(List.of(
                                        GeminiRequest.Part.builder()
                                                .text(prompt)
                                                .build()))
                                .build()))
                .generationConfig(GeminiRequest.GenerationConfig.builder()
                        .temperature(temperature)
                        .maxOutputTokens(maxTokens)
                        .responseMimeType("application/json")
                        .responseSchema(responseSchema)
                        .build())
                .build();
    }
    
    private Map<String, Object> buildResponseSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        // Define each property with its type and constraints
        properties.put("project_name", Map.of("type", "string"));
        properties.put("short_description", Map.of("type", "string"));
        properties.put("problem_description", Map.of("type", "string"));
        properties.put("solution", Map.of("type", "string"));
        properties.put("technical_details", Map.of("type", "string"));
        
        properties.put("technologies", Map.of(
                "type", "array",
                "items", Map.of("type", "string"),
                "maxItems", MAX_TECHNOLOGIES
        ));
        
        properties.put("categories", Map.of(
                "type", "array",
                "items", Map.of("type", "string"),
                "maxItems", MAX_CATEGORIES
        ));
        
        properties.put("rating", Map.of(
                "type", "integer",
                "minimum", 1,
                "maximum", 10
        ));
        
        properties.put("transformation_confidence", Map.of(
                "type", "number",
                "minimum", 0.0,
                "maximum", 1.0
        ));
        
        schema.put("properties", properties);
        schema.put("required", List.of("project_name", "short_description", "solution", "rating"));
        
        return schema;
    }
    
    private GeminiTransformationResult parseGeminiResponse(GeminiResponse response) {
        try {
            if (response.getCandidates() == null || response.getCandidates().isEmpty()) {
                throw new RuntimeException("No candidates in Gemini response");
            }
            
            GeminiResponse.Candidate candidate = response.getCandidates().get(0);
            if (candidate.getContent() == null || candidate.getContent().getParts() == null || 
                candidate.getContent().getParts().isEmpty()) {
                throw new RuntimeException("No content in Gemini response");
            }
            
            String jsonText = candidate.getContent().getParts().get(0).getText();
            return objectMapper.readValue(jsonText, GeminiTransformationResult.class);
            
        } catch (JsonProcessingException e) {
            log.error("Failed to parse Gemini response JSON", e);
            throw new RuntimeException("Failed to parse Gemini response", e);
        }
    }
    
    private TransformedIdea mapToTransformedIdea(ScrapedIdea original, GeminiTransformationResult result) {
        if (result == null) {
            return null;
        }
        
        return TransformedIdea.builder()
                .originalUrl(original.getSourceUrl())
                .sourceWebsite(original.getSourceWebsite())
                .scrapedAt(original.getScrapedAt())
                .transformedAt(LocalDateTime.now())
                .projectName(result.getProjectName())
                .shortDescription(result.getShortDescription())
                .solution(result.getSolution())
                .problemDescription(result.getProblemDescription())
                .technicalDetails(result.getTechnicalDetails())
                .createdBy("AI-Generated")
                .likes(0)
                .rating(result.getRating())
                .technologies(limitList(result.getTechnologies(), MAX_TECHNOLOGIES))
                .categories(limitList(result.getCategories(), MAX_CATEGORIES))
                .contentHash(generateContentHash(result))
                .transformationModel(model)
                .transformationConfidence(result.getTransformationConfidence())
                .build();
    }
    
    private List<String> limitList(List<String> list, int maxSize) {
        if (list == null || list.size() <= maxSize) {
            return list;
        }
        return list.subList(0, maxSize);
    }
    
    private String generateContentHash(GeminiTransformationResult result) {
        String content = result.getProjectName() + result.getShortDescription() + result.getSolution();
        return Integer.toHexString(content.hashCode());
    }
    
    private boolean isRetryableException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException ex = (WebClientResponseException) throwable;
            int statusCode = ex.getStatusCode().value();
            // Retry on rate limiting (429) and server errors (5xx)
            return statusCode == 429 || (statusCode >= 500 && statusCode < 600);
        }
        return false;
    }
    
    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * Records performance metrics for transformation operations
     */
    private void recordTransformationMetrics(String operationId, int inputCount, int outputCount, long durationMs) {
        PerformanceMetrics metrics = PerformanceMetrics.builder()
                .operationType("AI_TRANSFORMATION")
                .operationId(operationId)
                .timestamp(LocalDateTime.now())
                .durationMs(durationMs)
                .itemsProcessed(outputCount)
                .itemsPerSecond(durationMs > 0 ? (double) outputCount / (durationMs / 1000.0) : 0)
                .batchSize(inputCount)
                .aiRequestCount(inputCount)
                .aiSuccessRate((double) outputCount / inputCount * 100)
                .successful(outputCount > 0)
                .context(String.format("Transformed %d/%d ideas successfully", outputCount, inputCount))
                .build();
        
        monitoringService.recordPerformanceMetrics("AI_TRANSFORMATION", metrics);
    }
}