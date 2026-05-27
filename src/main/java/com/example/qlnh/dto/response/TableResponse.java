package com.example.qlnh.dto.response;

import com.example.qlnh.models.entities.Table;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TableResponse {
    private Long id;
    private String name;
    private Integer capacity;
    private String status;
    private String location;

    public static TableResponse fromEntity(Table table) {
        if (table == null) return null;
        return TableResponse.builder()
                .id(table.getId())
                .name(table.getName())
                .capacity(table.getCapacity())
                .status(table.getStatus())
                .location(table.getLocation())
                .build();
    }
}
