package com.mfon.rmhi.dto;

import com.mfon.rmhi.model.Category;
import com.mfon.rmhi.model.Comment;
import com.mfon.rmhi.model.Technology;
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
