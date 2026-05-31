package com.example.qlnh.models.entities;

import com.example.qlnh.models.enums.FoodStatus;
import com.example.qlnh.models.enums.MealType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// Hoặc viết ngắn gọn lại thành 1 dòng duy nhất là bao trọn gói:
// import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "foods", indexes = {
        @Index(name = "idx_foods_status_mealtype", columnList = "status, meal_type"),
        @Index(name = "idx_foods_name", columnList = "name")
})
// 1. Kích hoạt Xóa mềm (Bảo vệ dữ liệu lịch sử)
@SQLDelete(sql = "UPDATE foods SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Food extends AuditableEntity {

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private Float price;

    @Column(name = "image_url")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FoodStatus status = FoodStatus.AVAILABLE;

    // 2. FetchType.LAZY giúp tối ưu hiệu suất, không query User nếu không cần thiết
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false)
    private MealType mealType;
}