package com.mfon.rmhi.repository;

import com.mfon.rmhi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    boolean existsUserByUsername(String username);
}
