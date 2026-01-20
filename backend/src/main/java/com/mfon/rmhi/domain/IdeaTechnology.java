package com.mfon.rmhi.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "scraped_idea_technologies")
public class IdeaTechnology {

    @Id
    @Column(name = "idea_id")
    private Long ideaId;

    @Id
    @Column(name = "technology_id")
    private Integer technologyId;

    @ManyToOne
    @JoinColumn(name = "idea_id", insertable = false, updatable = false)
    private Idea idea;

    @ManyToOne
    @JoinColumn(name = "technology_id", insertable = false, updatable = false)
    private Technology technology;
}
