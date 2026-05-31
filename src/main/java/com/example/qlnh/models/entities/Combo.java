package com.example.qlnh.models.entities;

import jakarta.persistence.Table;
import com.example.qlnh.models.enums.ComboStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "combos")
@Getter
@Setter
// THÊM 2 DÒNG NÀY ĐỂ BẢO VỆ DỮ LIỆU HÓA ĐƠN
@SQLDelete(sql = "UPDATE combos SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor // Bắt buộc phải có cho JPA
@AllArgsConstructor
public class Combo extends BaseEntity {
    // Mẹo: Nếu bạn muốn lưu vết ai là người tạo Combo này, hãy đổi BaseEntity thành
    // AuditableEntity giống như bảng Food nhé!

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Float price;

    private String description;

    // 1. Ép kiểu Enum an toàn, lưu xuống DB dưới dạng Chuỗi (String)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComboStatus status = ComboStatus.AVAILABLE;

    @Column(name = "image_url")
    private String imageUrl;

    // 2. Thiết lập mối quan hệ 1-Nhiều với bảng trung gian ComboFood
    // CascadeType.ALL giúp khi bạn xóa Combo, các record trong bảng trung gian cũng
    // tự động bay màu
    // THÊM orphanRemoval = true VÀO ĐÂY:
    @OneToMany(mappedBy = "combo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ComboFood> comboFoods;
}