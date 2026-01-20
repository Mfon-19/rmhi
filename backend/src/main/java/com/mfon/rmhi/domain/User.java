package com.mfon.rmhi.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@AllArgsConstructor
@Getter
@Setter
@Table(name = "users")
@NoArgsConstructor
public class User {
    @Id
    @Column(name = "uid", length = 28)
    private String uid;
    
    @Column(name = "email", length = 255, unique = true, nullable = false)
    private String email;
    
    @Column(name = "username", length = 32, unique = true, nullable = false)
    private String username;
    
    @Column(name = "provider", length = 32, nullable = false)
    private String provider = "password";
}
