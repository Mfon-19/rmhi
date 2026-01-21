package com.mfon.rmhi.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class TransformedIdeaResponseDTO {
    private Long id;
    private String projectName;
    private Integer likes;
    private String createdBy;
    private List<String> categories;
    private String shortDescription;
    private String solution;
    private String problemDescription;
    private String technicalDetails;
    private List<String> technologies;
    private Double rating;
}
