package com.mfon.rmhi.service;

import com.mfon.rmhi.dto.IdeaResponseDTO;
import com.mfon.rmhi.mapper.IdeaMapper;
import com.mfon.rmhi.model.*;
import com.mfon.rmhi.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final IdeaRepository ideaRepository;
    private final IdeaTechnologyRepository ideaTechnologyRepository;
    private final IdeaMapper ideaMapper;
    private final CommentRepository commentRepository;
    private final TechnologyRepository technologyRepository;
    private final CategoryRepository categoryRepository;

    public UserService(UserRepository userRepository, IdeaRepository ideaRepository, IdeaTechnologyRepository ideaTechnologyRepository, IdeaMapper ideaMapper, CommentRepository commentRepository, TechnologyRepository technologyRepository, CategoryRepository categoryRepository) {
        this.userRepository = userRepository;
        this.ideaRepository = ideaRepository;
        this.ideaTechnologyRepository = ideaTechnologyRepository;
        this.ideaMapper = ideaMapper;
        this.commentRepository = commentRepository;
        this.technologyRepository = technologyRepository;
        this.categoryRepository = categoryRepository;
    }

    public void registerUser(String username, Jwt jwt) {
        String uid   = jwt.getSubject();
        String email = jwt.getClaim("email");
        String prov  = (String) ((Map<?,?>) jwt.getClaim("firebase")).get("sign_in_provider");
        User user = new User(uid, email, username, prov);

        userRepository.save(user);
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

    // for now, return all ideas
    public List<IdeaResponseDTO> getIdeas() {
        List<Idea> ideas = ideaRepository.findAll();
        List<Comment> comments = commentRepository.findAll();
        List<Technology> technologies = technologyRepository.findAll();
        List<Category> categories = categoryRepository.findAll();
        List<IdeaResponseDTO> responseDTOs = ideas.stream().map(ideaMapper::toDto).toList();
        // get all comments, then map those comments.ideaId to responseDTO.comment

        Map<Long, List<Comment>> commentsByIdeaId = comments.stream()
                .collect(Collectors.groupingBy(
                        Comment::getIdeaId,
                        Collectors.mapping(comment -> comment, Collectors.toList())
                ));

        responseDTOs = responseDTOs.stream()
                .map(responseDto -> {
                    responseDto.setComments(commentsByIdeaId.getOrDefault(responseDto.getId(), List.of()));
                    return responseDto;
                }).toList();

        return responseDTOs;
    }
}
