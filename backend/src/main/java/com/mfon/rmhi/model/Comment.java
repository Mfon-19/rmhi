package com.mfon.rmhi.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;

@Entity
@Table(name = "comments")
@AllArgsConstructor
@NoArgsConstructor
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "idea_id")
    private Long ideaId;
    
    @Column(name = "user_id")
    private String userId;
}