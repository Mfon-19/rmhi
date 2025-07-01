package com.mfon.rmhi.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "technologies")
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

    public Integer getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Set<ScrapedIdea> getIdeas() { return ideas; }
    public Set<TechnologyAlias> getAliases() { return aliases; }
}
