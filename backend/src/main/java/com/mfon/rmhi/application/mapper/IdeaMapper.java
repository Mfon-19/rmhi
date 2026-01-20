package com.mfon.rmhi.application.mapper;

import com.mfon.rmhi.api.dto.IdeaResponseDTO;
import com.mfon.rmhi.domain.Idea;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IdeaMapper {
    @Mapping(target = "comments", ignore = true)
    IdeaResponseDTO toDto(Idea idea);

    Idea toIdea(IdeaResponseDTO ideaResponseDTO);
}
