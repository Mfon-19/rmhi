package com.mfon.rmhi.controller;

import com.mfon.rmhi.model.Idea;
import com.mfon.rmhi.model.ScrapedIdea;
import com.mfon.rmhi.repository.UserRepository;
import com.mfon.rmhi.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register-username")
    public ResponseEntity<Integer> registerUsername(@AuthenticationPrincipal Jwt jwt,
                                           @RequestBody Map<String, String> request) {
        String username = request.get("username");

        if (userRepository.existsUserByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        userService.registerUser(username, jwt);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/create-idea")
    public ResponseEntity<Map<String, Long>> createIdea(@AuthenticationPrincipal Jwt jwt, @RequestBody Map<String, Idea> request) {

        Idea idea = request.get("idea");
        Long id   = userService.createIdea(idea);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(Map.of("id", id));
    }

    @PostMapping("/create-scraped-idea")
    public ResponseEntity<Map<String, Long>> createScrapedIdea(@AuthenticationPrincipal Jwt jwt, @RequestBody Map<String, ScrapedIdea> request) {
        ScrapedIdea idea = request.get("idea");
        Long id = userService.saveScrapedIdea(idea);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(Map.of("id", id));
    }
}
