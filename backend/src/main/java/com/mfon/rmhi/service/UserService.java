package com.mfon.rmhi.service;

import com.mfon.rmhi.model.*;
import com.mfon.rmhi.repository.IdeaRepository;
import com.mfon.rmhi.repository.IdeaTechnologyRepository;
import com.mfon.rmhi.repository.ScrapedIdeaRepository;
import com.mfon.rmhi.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final IdeaRepository ideaRepository;
    private final ScrapedIdeaRepository scrapedIdeaRepository;
    private final IdeaTechnologyRepository ideaTechnologyRepository;

    public UserService(UserRepository userRepository, IdeaRepository ideaRepository, ScrapedIdeaRepository scrapedIdeaRepository, IdeaTechnologyRepository ideaTechnologyRepository) {
        this.userRepository = userRepository;
        this.ideaRepository = ideaRepository;
        this.scrapedIdeaRepository = scrapedIdeaRepository;
        this.ideaTechnologyRepository = ideaTechnologyRepository;
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
        List<String> tags = ideaToSave.getCategories()
                .stream()
                .map(Category::getName)
                .collect(Collectors.toList());

        Idea idea = ideaRepository.save(ideaToSave);
        ideaRepository.addTagsToIdea(idea.getId(), tags.toArray(new String[0]));
        return idea.getId();
    }

    @Transactional
    public Long saveScrapedIdea(ScrapedIdea scrapedIdea) {
        List<String> tags = scrapedIdea.getTechnologies()
                .stream()
                .map(Technology::getName)
                .collect(Collectors.toList());

        scrapedIdea.setTechnologies(new java.util.HashSet<>());

        ScrapedIdea idea = scrapedIdeaRepository.save(scrapedIdea);

        ideaTechnologyRepository.addTechnologiesToIdea(
                idea.getId(),
                tags.toArray(new String[0])
        );

        return idea.getId();
    }
}
