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
    date = "2025-07-15T15:59:12-0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21 (Eclipse Adoptium)"
)
@Component
public class IdeaMapperImpl implements IdeaMapper {

    @Override
    public IdeaResponseDTO toDto(Idea idea) {
        if ( idea == null ) {
            return null;
        }

        IdeaResponseDTO ideaResponseDTO = new IdeaResponseDTO();

        ideaResponseDTO.setId( idea.getId() );
        ideaResponseDTO.setProjectName( idea.getProjectName() );
        ideaResponseDTO.setLikes( idea.getLikes() );
        ideaResponseDTO.setCreatedBy( idea.getCreatedBy() );
        Set<Category> set = idea.getCategories();
        if ( set != null ) {
            ideaResponseDTO.setCategories( new LinkedHashSet<Category>( set ) );
        }
        ideaResponseDTO.setShortDescription( idea.getShortDescription() );
        ideaResponseDTO.setSolution( idea.getSolution() );
        ideaResponseDTO.setProblemDescription( idea.getProblemDescription() );
        ideaResponseDTO.setTechnicalDetails( idea.getTechnicalDetails() );
        Set<Technology> set1 = idea.getTechnologies();
        if ( set1 != null ) {
            ideaResponseDTO.setTechnologies( new LinkedHashSet<Technology>( set1 ) );
        }
        ideaResponseDTO.setRating( idea.getRating() );

        return ideaResponseDTO;
    }

    @Override
    public Idea toIdea(IdeaResponseDTO ideaResponseDTO) {
        if ( ideaResponseDTO == null ) {
            return null;
        }

        Idea idea = new Idea();

        idea.setId( ideaResponseDTO.getId() );
        idea.setProjectName( ideaResponseDTO.getProjectName() );
        idea.setLikes( ideaResponseDTO.getLikes() );
        idea.setCreatedBy( ideaResponseDTO.getCreatedBy() );
        Set<Category> set = ideaResponseDTO.getCategories();
        if ( set != null ) {
            idea.setCategories( new LinkedHashSet<Category>( set ) );
        }
        idea.setShortDescription( ideaResponseDTO.getShortDescription() );
        idea.setSolution( ideaResponseDTO.getSolution() );
        idea.setProblemDescription( ideaResponseDTO.getProblemDescription() );
        idea.setTechnicalDetails( ideaResponseDTO.getTechnicalDetails() );
        Set<Technology> set1 = ideaResponseDTO.getTechnologies();
        if ( set1 != null ) {
            idea.setTechnologies( new LinkedHashSet<Technology>( set1 ) );
        }
        idea.setRating( ideaResponseDTO.getRating() );

        return idea;
    }
}
