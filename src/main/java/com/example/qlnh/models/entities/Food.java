package com.example.qlnh.models.entities;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;

import java.sql.Timestamp;

@Entity
@Table(
    name = "foods",
    indexes = {
        @Index(name = "idx_foods_status_mealtype", columnList = "status, meal_type"),
        @Index(name = "idx_foods_name",            columnList = "name")
    }
)
@Getter
@Setter
@RequiredArgsConstructor
public class Food extends AuditableEntity {

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private Float price;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(nullable = false)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "meal_type")
    private String mealType;
}
