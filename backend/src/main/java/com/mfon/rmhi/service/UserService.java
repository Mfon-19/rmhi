package com.mfon.rmhi.service;

import com.mfon.rmhi.model.Idea;
import com.mfon.rmhi.repository.IdeaRepository;
import com.mfon.rmhi.repository.UserRepository;
import com.mfon.rmhi.model.User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final IdeaRepository ideaRepository;

    public UserService(UserRepository userRepository, IdeaRepository ideaRepository) {
        this.userRepository = userRepository;
        this.ideaRepository = ideaRepository;
    }

    public void registerUser(String username, Jwt jwt) {
        String uid   = jwt.getSubject();
        String email = jwt.getClaim("email");
        String prov  = (String) ((Map<?,?>) jwt.getClaim("firebase")).get("sign_in_provider");
        User user = new User(uid, email, username, prov);

        userRepository.save(user);
    }

    public void createIdea(Idea idea) {
        ideaRepository.save(idea);
    }
}
