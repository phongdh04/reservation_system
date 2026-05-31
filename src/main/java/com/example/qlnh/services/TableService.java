package com.example.qlnh.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.example.qlnh.dto.request.TableRequest;
import com.example.qlnh.dto.response.TableResponse;
import com.example.qlnh.exception.BusinessValidationException;
import com.example.qlnh.exception.ResourceNotFoundException;
import com.example.qlnh.models.entities.Table;
import com.example.qlnh.models.enums.TableStatus;
import com.example.qlnh.repositories.TableRepository;
import com.example.qlnh.services.interfaces.ITableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TableService implements ITableService {

    private final TableRepository tableRepository;

    @Override
    @Transactional
    public Table createTable(TableRequest request) {

        // 1. Kiểm tra nghiệp vụ (Ví dụ: Chống tạo trùng tên bàn)
        if (tableRepository.existsByName(request.getName())) {
            throw new BusinessValidationException("Tên bàn '" + request.getName() + "' đã tồn tại!");
        }

        // 2. Chuyển đổi DTO -> Entity (Chính là hàm buildTable cũ của bạn)
        Table table = new Table();
        table.setName(request.getName());
        table.setCapacity(request.getCapacity());

        // Mặc định bàn mới tạo ra luôn ở trạng thái TRỐNG (Nếu bạn dùng Enum)
        table.setStatus(TableStatus.AVAILABLE);

        // 3. Lưu xuống Database
        return tableRepository.save(table);
    }

    @Override
    @Transactional
    public Table updateTable(Long id, TableRequest request) {
        // 1. Tìm Bàn (Nếu không có thì văng lỗi 404 cho GlobalExceptionHandler tự bắt)
        Table table = tableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bàn với ID: " + id));

        // 2. Validate và Cập nhật Tên (Logic hóc búa nhất)
        // Chỉ check trùng lặp nếu người dùng thực sự muốn đổi sang một tên khác
        if (request.getName() != null && !request.getName().equals(table.getName())) {
            if (tableRepository.existsByName(request.getName())) {
                throw new BusinessValidationException("Tên bàn '" + request.getName() + "' đã tồn tại!");
            }
            table.setName(request.getName());
        }

        // 3. Cập nhật các trường thông tin cơ bản khác (Ví dụ: Số ghế)
        if (request.getCapacity() != null) {
            table.setCapacity(request.getCapacity());
        }

        // 3.1 Cập nhật Trạng thái (Status) an toàn với Enum
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            try {
                // Ép chuỗi thành viết hoa và chuyển sang Enum
                table.setStatus(TableStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Chặn đứng nếu Frontend gửi sai trạng thái
                throw new BusinessValidationException(
                        "Trạng thái bàn không hợp lệ! Chỉ chấp nhận: AVAILABLE, OCCUPIED, RESERVED.");
            }
        }

        // 4. Lưu xuống DB
        return tableRepository.save(table);
    }

    @Override
    @Transactional
    public void deleteTable(Long id) {

        // 1. Tìm bàn (Nếu không có văng lỗi 404)
        Table table = tableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bàn với ID: " + id));

        // 2. Lính gác nghiệp vụ: Chống xóa bàn đang hoạt động
        if (table.getStatus() != TableStatus.AVAILABLE) {
            throw new BusinessValidationException(
                    "Hành động bị từ chối: Không thể xóa bàn đang có khách hoặc đã được đặt trước!");
        }

        // 3. Thực hiện xóa (Sẽ tự động thành Xóa mềm nếu đã cấu hình Entity)
        tableRepository.delete(table);
    }

    @Override
    @Transactional(readOnly = true) // Tối ưu tốc độ đọc dữ liệu
    public Page<TableResponse> getAllTables(String keyword, String status, int page, int itemsPerPage) {

        // --- 1. CHỐT CHẶN AN TOÀN PHÂN TRANG ---
        int safePage = Math.max(page - 1, 0); // JPA đếm trang từ 0
        int safeItemsPerPage = Math.min(itemsPerPage, 100); // Tối đa 100 record/trang

        // Sắp xếp bàn mới tạo hoặc bàn có ID nhỏ lên đầu
        Pageable pageable = PageRequest.of(safePage, safeItemsPerPage, Sort.by("id").ascending());

        // --- 2. XỬ LÝ LỌC ENUM (An toàn) ---
        TableStatus tableStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                // Ép Frontend gửi gì cũng thành IN HOA để so sánh với Enum
                tableStatus = TableStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessValidationException(
                        "Trạng thái lọc không hợp lệ! Chỉ dùng: AVAILABLE, OCCUPIED, RESERVED.");
            }
        }

        // --- 3. GỌI SIÊU HÀM TÌM KIẾM TRONG REPOSITORY ---
        Page<Table> tables = tableRepository.searchTables(keyword, tableStatus, pageable);

        // --- 4. MAP SANG DTO VÀ TRẢ VỀ ---
        return tables.map(TableResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true) // Thêm dòng này để tăng tốc độ lấy dữ liệu từ DB
    public Table getTableById(Long id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bàn với ID: " + id));
    }
}
