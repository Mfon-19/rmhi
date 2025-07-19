package com.mfon.rmhi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class IdeaTechnologyId implements Serializable {

    @Column(name = "idea_id")
    private Long ideaId;

    @Column(name = "technology_id")
    private Integer technologyId;

    public IdeaTechnologyId() {
    }

    public IdeaTechnologyId(Long ideaId, Integer technologyId) {
        this.ideaId = ideaId;
        this.technologyId = technologyId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdeaTechnologyId that = (IdeaTechnologyId) o;
        return Objects.equals(ideaId, that.ideaId) &&
               Objects.equals(technologyId, that.technologyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ideaId, technologyId);
    }
} 