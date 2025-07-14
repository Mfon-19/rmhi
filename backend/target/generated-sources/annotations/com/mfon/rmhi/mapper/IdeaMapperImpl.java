package com.mfon.rmhi.mapper;

import com.mfon.rmhi.dto.IdeaResponseDTO;
import com.mfon.rmhi.model.Category;
import com.mfon.rmhi.model.Idea;
import com.mfon.rmhi.model.Technology;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-07-13T16:25:41-0700",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.42.50.v20250628-1110, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class IdeaMapperImpl implements IdeaMapper {

    @Override
    public IdeaResponseDTO toDto(Idea idea) {
        if ( idea == null ) {
            return null;
        }

        IdeaResponseDTO ideaResponseDTO = new IdeaResponseDTO();

        Set<Category> set = idea.getCategories();
        if ( set != null ) {
            ideaResponseDTO.setCategories( new LinkedHashSet<Category>( set ) );
        }
        ideaResponseDTO.setCreatedBy( idea.getCreatedBy() );
        ideaResponseDTO.setId( idea.getId() );
        ideaResponseDTO.setLikes( idea.getLikes() );
        ideaResponseDTO.setProblemDescription( idea.getProblemDescription() );
        ideaResponseDTO.setProjectName( idea.getProjectName() );
        ideaResponseDTO.setRating( idea.getRating() );
        ideaResponseDTO.setShortDescription( idea.getShortDescription() );
        ideaResponseDTO.setSolution( idea.getSolution() );
        ideaResponseDTO.setTechnicalDetails( idea.getTechnicalDetails() );
        Set<Technology> set1 = idea.getTechnologies();
        if ( set1 != null ) {
            ideaResponseDTO.setTechnologies( new LinkedHashSet<Technology>( set1 ) );
        }

        return ideaResponseDTO;
    }

    @Override
    public Idea toIdea(IdeaResponseDTO ideaResponseDTO) {
        if ( ideaResponseDTO == null ) {
            return null;
        }

        Idea idea = new Idea();

        Set<Category> set = ideaResponseDTO.getCategories();
        if ( set != null ) {
            idea.setCategories( new LinkedHashSet<Category>( set ) );
        }
        idea.setCreatedBy( ideaResponseDTO.getCreatedBy() );
        idea.setId( ideaResponseDTO.getId() );
        idea.setLikes( ideaResponseDTO.getLikes() );
        idea.setProblemDescription( ideaResponseDTO.getProblemDescription() );
        idea.setProjectName( ideaResponseDTO.getProjectName() );
        idea.setRating( ideaResponseDTO.getRating() );
        idea.setShortDescription( ideaResponseDTO.getShortDescription() );
        idea.setSolution( ideaResponseDTO.getSolution() );
        idea.setTechnicalDetails( ideaResponseDTO.getTechnicalDetails() );
        Set<Technology> set1 = ideaResponseDTO.getTechnologies();
        if ( set1 != null ) {
            idea.setTechnologies( new LinkedHashSet<Technology>( set1 ) );
        }

        return idea;
    }
}
