package com.mfon.rmhi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "scraped_ideas")
@Getter
@Setter
public class ScrapedIdea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("project_name")
    @Column(name = "project_name", nullable = false)
    private String projectName;

    @Column(nullable = false)
    private Integer likes = 0;

    @JsonProperty("submitted_to")
    @Column(name = "submitted_to")
    private String submittedTo;

    @Column(nullable = false)
    private Boolean winner = false;

    @JsonProperty("created_by")
    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(columnDefinition = "TEXT", name = "short_description")
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String solution;

    @Column(columnDefinition = "TEXT", name = "problem_description")
    private String problemDescription;

    @Column(columnDefinition = "TEXT", name = "technical_details")
    private String technicalDetails;

    @ManyToMany
    @JoinTable(
            name = "scraped_idea_technologies",
            joinColumns = @JoinColumn(name = "idea_id"),
            inverseJoinColumns = @JoinColumn(name = "technology_id")
    )
    private Set<Technology> technologies = new HashSet<>();

    public ScrapedIdea() {
    }
}
