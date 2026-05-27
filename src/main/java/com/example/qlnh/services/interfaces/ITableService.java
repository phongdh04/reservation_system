package com.example.qlnh.services.interfaces;

import com.example.qlnh.models.entities.Table;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ITableService {

    List<Table> getAllTables();
    Page<Table> getTablesByPage(int page, int itemsPerPage);
    long getTotalTables();
    Table getTableById(Long id);
    Table createTable(Table table);
    Table updateTable(Table table);
    void deleteTable(Long id);
    List<Table> getTablesByStatus(String status);
    Page<Table> getTablesByPageAndStatus(int page, int itemsPerPage, String status);
    long getTableCountByStatus(String status);
    Page<Table> findByKeyword(String keyword, int page, int itemsPerPage);
    long getTotalTablesByKeyword(String keyword);
    Page<Table> findByKeywordAndStatus(String keyword, String status, int page, int itemsPerPage);
    long getTotalTablesByKeywordAndStatus(String keyword, String status);
}
