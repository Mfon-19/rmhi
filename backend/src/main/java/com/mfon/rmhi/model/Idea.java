package com.mfon.rmhi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "ideas")
@Getter
@Setter
public class Idea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("project_name")
    @Column(name = "project_name", nullable = false)
    private String projectName;

    @Column(nullable = false)
    private Integer likes = 0;

    @JsonProperty("created_by")
    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "idea_categories",
            joinColumns        = @JoinColumn(name = "idea_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> categories = new HashSet<>();

    @JsonProperty("short_description")
    @Column(columnDefinition = "TEXT", name = "short_description")
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String solution;

    @JsonProperty("problem_description")
    @Column(columnDefinition = "TEXT", name = "problem_description")
    private String problemDescription;

    @JsonProperty("technical_details")
    @Column(columnDefinition = "TEXT", name = "technical_details")
    private String technicalDetails;

    @ManyToMany
    @JoinTable(
            name = "idea_technologies",
            joinColumns = @JoinColumn(name = "idea_id"),
            inverseJoinColumns = @JoinColumn(name = "technology_id")
    )
    @JsonManagedReference
    private Set<Technology> technologies = new HashSet<>();

    private Integer rating;

    public Idea() {
    }
}
