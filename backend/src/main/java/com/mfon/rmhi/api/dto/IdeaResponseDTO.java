package com.mfon.rmhi.api.dto;

import com.mfon.rmhi.domain.Category;
import com.mfon.rmhi.domain.Comment;
import com.mfon.rmhi.domain.Technology;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Getter
@Setter
public class IdeaResponseDTO{
    Long id;
    String projectName;
    Integer likes;
    String createdBy;
    Set<Category> categories;
    String shortDescription;
    String solution;
    String problemDescription;
    String technicalDetails;
    Set<Technology> technologies;
    Integer rating;
    List<Comment> comments;
}
