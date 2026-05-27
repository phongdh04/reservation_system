package com.example.qlnh.models.entities;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;

@Entity
@Table(name = "combos")
@Getter
@Setter
@RequiredArgsConstructor
public class Combo extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Float price;

    private String description;

    @Column(nullable = false)
    private String status;

    @Column(name = "image_url")
    private String imageUrl;
}
