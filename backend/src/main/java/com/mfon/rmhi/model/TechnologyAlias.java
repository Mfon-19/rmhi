package com.mfon.rmhi.model;

import jakarta.persistence.*;

@Entity
@Table(name = "technology_aliases")
public class TechnologyAlias {

    @Id
    private String alias;

    @ManyToOne
    @JoinColumn(name = "technology_id", nullable = false)
    private Technology technology;

    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
    public Technology getTechnology() { return technology; }
    public void setTechnology(Technology technology) { this.technology = technology; }
}
