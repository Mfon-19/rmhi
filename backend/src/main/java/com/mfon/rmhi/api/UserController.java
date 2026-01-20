package com.mfon.rmhi.api;

import com.mfon.rmhi.api.request.RegisterUsernameRequest;
import com.mfon.rmhi.application.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register-username")
    public ResponseEntity<Void> registerUsername(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody RegisterUsernameRequest request
    ) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        if (!userService.registerUser(request.username(), jwt)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
