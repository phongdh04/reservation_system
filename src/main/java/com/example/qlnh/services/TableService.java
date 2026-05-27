package com.example.qlnh.services;

import com.example.qlnh.exception.ResourceNotFoundException;
import com.example.qlnh.models.entities.Table;
import com.example.qlnh.repositories.TableRepository;
import com.example.qlnh.services.interfaces.ITableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TableService implements ITableService {

    private final TableRepository tableRepository;

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public List<Table> getAllTables() {
        return tableRepository.findAll(Sort.by(Sort.Order.asc("capacity"), Sort.Order.asc("name")));
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public Page<Table> getTablesByPage(int page, int itemsPerPage) {
        return tableRepository.findAll(PageRequest.of(page - 1, itemsPerPage));
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public long getTotalTables() {
        return tableRepository.count();
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public Table getTableById(Long id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Table", "id", id));
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional
    public Table createTable(Table table) {
        return tableRepository.save(table);
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional
    public Table updateTable(Table table) {
        if (!tableRepository.existsById(table.getId())) {
            throw new ResourceNotFoundException("Table", "id", table.getId());
        }
        return tableRepository.save(table);
    }

    // TODO: VIET LOGIC - soft delete (set deletedAt)
    @Override
    @Transactional
    public void deleteTable(Long id) {
        throw new UnsupportedOperationException("TODO: Implement deleteTable (soft delete) logic");
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public List<Table> getTablesByStatus(String status) {
        return tableRepository.findByStatus(status, PageRequest.of(0, Integer.MAX_VALUE)).getContent();
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public Page<Table> getTablesByPageAndStatus(int page, int itemsPerPage, String status) {
        return tableRepository.findByStatus(status, PageRequest.of(page - 1, itemsPerPage));
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public long getTableCountByStatus(String status) {
        return tableRepository.countByStatus(status);
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public Page<Table> findByKeyword(String keyword, int page, int itemsPerPage) {
        return tableRepository.findByKeyword(keyword, PageRequest.of(page - 1, itemsPerPage));
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public long getTotalTablesByKeyword(String keyword) {
        return tableRepository.countByStatus(keyword);
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public Page<Table> findByKeywordAndStatus(String keyword, String status, int page, int itemsPerPage) {
        return tableRepository.findByKeywordAndStatus(keyword, status, PageRequest.of(page - 1, itemsPerPage));
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public long getTotalTablesByKeywordAndStatus(String keyword, String status) {
        return tableRepository.countByStatus(status);
    }
}
