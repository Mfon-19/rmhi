package com.mfon.rmhi.model;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "categories")
@Getter
public class Category {

    @Id
    @GeneratedValue
    private Integer id;

    @Column(unique = true, nullable = false)
    private String name;
}