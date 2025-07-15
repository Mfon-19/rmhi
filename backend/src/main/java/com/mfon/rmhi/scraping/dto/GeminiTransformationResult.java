package com.mfon.rmhi.scraping.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for structured Gemini transformation result
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeminiTransformationResult {
    
    @JsonProperty("project_name")
    private String projectName;
    
    @JsonProperty("short_description")
    private String shortDescription;
    
    @JsonProperty("problem_description")
    private String problemDescription;
    
    private String solution;
    
    @JsonProperty("technical_details")
    private String technicalDetails;
    
    private List<String> technologies;
    
    private List<String> categories;
    
    private Integer rating;
    
    @JsonProperty("transformation_confidence")
    private Double transformationConfidence;
}