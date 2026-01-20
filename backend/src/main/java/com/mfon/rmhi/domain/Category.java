package com.mfon.rmhi.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "categories")
@Getter
@Setter
public class Category {

    @Id
    @GeneratedValue
    private Integer id;

    @Column(unique = true, nullable = false)
    private String name;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public Category(String name) {
        this.name = name;
    }

    public Category() {

    }
}