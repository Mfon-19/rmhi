package com.mfon.rmhi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "technologies")
@Getter
@Setter
public class Technology {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    @ManyToMany(mappedBy = "technologies")
    private Set<ScrapedIdea> ideas = new HashSet<>();

    @OneToMany(mappedBy = "technology", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TechnologyAlias> aliases = new HashSet<>();

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public Technology(String name) {
        this.name = name;
    }

    public Technology() {

    }
}
