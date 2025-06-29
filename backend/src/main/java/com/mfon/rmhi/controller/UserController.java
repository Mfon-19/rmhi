package com.mfon.rmhi.controller;

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
@RequestMapping("/api/auth")
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

        // Check if username exists
        if (userRepository.existsUserByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        userService.registerUser(username, jwt);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
