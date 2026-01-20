package com.mfon.rmhi.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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
    @JsonIgnore
    private Set<Idea> ideas = new HashSet<>();

    @OneToMany(mappedBy = "technology", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<TechnologyAlias> aliases = new HashSet<>();

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public Technology(String name) {
        this.name = name;
    }

    public Technology() {

    }
}
