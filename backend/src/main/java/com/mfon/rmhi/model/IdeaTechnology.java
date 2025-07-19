package com.mfon.rmhi.model;

import jakarta.persistence.*;

@Entity
@Table(name = "scraped_idea_technologies")
public class IdeaTechnology {

    @EmbeddedId
    private IdeaTechnologyId id;

    @ManyToOne
    @MapsId("ideaId")
    @JoinColumn(name = "idea_id")
    private Idea idea;

    @ManyToOne
    @MapsId("technologyId")
    @JoinColumn(name = "technology_id")
    private Technology technology;
}
