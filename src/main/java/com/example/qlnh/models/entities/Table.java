package com.example.qlnh.models.entities;

import com.example.qlnh.models.enums.TableStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.sql.Timestamp;

@Entity
// SỬA DÒNG NÀY: Viết rõ đích danh jakarta.persistence.Table
@jakarta.persistence.Table(name = "tables")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor // Bắt buộc phải có cho JPA
@AllArgsConstructor
public class Table extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer capacity;

    // 1. Khai báo Enum và ép kiểu lưu xuống DB là dạng Chuỗi (String)
    // 2. Gán sẵn mặc định: Khi tạo bàn mới, trạng thái tự động là AVAILABLE
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TableStatus status = TableStatus.AVAILABLE;

    private String location;

    @Column(name = "deleted_at")
    private Timestamp deletedAt;
}