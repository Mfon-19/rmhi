package com.mfon.rmhi.mapper;

import com.mfon.rmhi.dto.IdeaResponseDTO;
import com.mfon.rmhi.model.Idea;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface IdeaMapper {
    @Mapping(target = "comments", ignore = true)
    IdeaResponseDTO toDto(Idea idea);

    Idea toIdea(IdeaResponseDTO ideaResponseDTO);
}
