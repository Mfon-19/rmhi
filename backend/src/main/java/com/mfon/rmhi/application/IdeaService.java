package com.mfon.rmhi.application;

import com.mfon.rmhi.api.dto.IdeaResponseDTO;
import com.mfon.rmhi.application.mapper.IdeaMapper;
import com.mfon.rmhi.domain.Category;
import com.mfon.rmhi.domain.Comment;
import com.mfon.rmhi.domain.Idea;
import com.mfon.rmhi.domain.Technology;
import com.mfon.rmhi.infrastructure.persistence.CommentRepository;
import com.mfon.rmhi.infrastructure.persistence.IdeaRepository;
import com.mfon.rmhi.infrastructure.persistence.IdeaTechnologyRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class IdeaService {
    private final IdeaRepository ideaRepository;
    private final IdeaTechnologyRepository ideaTechnologyRepository;
    private final IdeaMapper ideaMapper;
    private final CommentRepository commentRepository;

    public IdeaService(
            IdeaRepository ideaRepository,
            IdeaTechnologyRepository ideaTechnologyRepository,
            IdeaMapper ideaMapper,
            CommentRepository commentRepository
    ) {
        this.ideaRepository = ideaRepository;
        this.ideaTechnologyRepository = ideaTechnologyRepository;
        this.ideaMapper = ideaMapper;
        this.commentRepository = commentRepository;
    }

    @Transactional
    public Long createIdea(Idea ideaToSave) {
        List<String> tags = ideaToSave.getTechnologies()
                .stream()
                .map(Technology::getName)
                .toList();

        List<String> categories = ideaToSave.getCategories()
                .stream()
                .map(Category::getName)
                .toList();

        ideaToSave.setTechnologies(new java.util.HashSet<>());
        ideaToSave.setCategories(new java.util.HashSet<>());

        Idea idea = ideaRepository.save(ideaToSave);

        ideaTechnologyRepository.addTechnologiesToIdea(
                idea.getId(),
                tags.toArray(new String[0])
        );

        ideaRepository.addTagsToIdea(idea.getId(), categories.toArray(new String[0]));

        return idea.getId();
    }

    public List<IdeaResponseDTO> getIdeas() {
        List<Idea> ideas = ideaRepository.findAll();
        List<Comment> comments = commentRepository.findAll();

        Map<Long, List<Comment>> commentsByIdeaId = comments.stream()
                .collect(Collectors.groupingBy(Comment::getIdeaId));

        return ideas.stream()
                .map(ideaMapper::toDto)
                .map(responseDto -> {
                    responseDto.setComments(
                            commentsByIdeaId.getOrDefault(responseDto.getId(), List.of())
                    );
                    return responseDto;
                })
                .toList();
    }
}
