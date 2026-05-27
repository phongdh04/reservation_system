package com.example.qlnh.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.sql.Timestamp;

@Entity
@Table(name = "tables")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@RequiredArgsConstructor
public class Table extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private String status;

    private String location;

    @Column(name = "deleted_at")
    private Timestamp deletedAt;
}
