package com.mfon.rmhi.application;

import com.mfon.rmhi.domain.User;
import com.mfon.rmhi.infrastructure.persistence.UserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean registerUser(String username, Jwt jwt) {
        if (userRepository.existsUserByUsername(username)) {
            return false;
        }

        String uid = jwt.getSubject();
        String email = jwt.getClaim("email");
        String prov = (String) ((Map<?, ?>) jwt.getClaim("firebase")).get("sign_in_provider");
        User user = new User(uid, email, username, prov);
        userRepository.save(user);
        return true;
    }
}
