package com.mfon.rmhi.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "ideas")
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Idea {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length = 255)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer likes = 0;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "idea_categories",
            joinColumns = @JoinColumn(name = "idea_id"))
    @Column(name = "category", nullable = false)
    private Set<Category> categories = new HashSet<>();

    @Column(name = "user_id")
    private String userId;
}