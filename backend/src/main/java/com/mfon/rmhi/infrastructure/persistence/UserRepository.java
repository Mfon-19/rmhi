package com.mfon.rmhi.infrastructure.persistence;

import com.mfon.rmhi.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    boolean existsUserByUsername(String username);
}
