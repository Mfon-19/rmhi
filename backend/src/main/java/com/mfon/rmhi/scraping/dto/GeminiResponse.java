package com.mfon.rmhi.scraping.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for Gemini API response structure
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeminiResponse {
    
    private List<Candidate> candidates;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Candidate {
        private Content content;
        
        @JsonProperty("finishReason")
        private String finishReason;
        
        private Integer index;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Content {
        private List<Part> parts;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Part {
        private String text;
    }
}