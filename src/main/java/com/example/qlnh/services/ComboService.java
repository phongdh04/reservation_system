package com.example.qlnh.services;

import com.example.qlnh.dto.request.ComboFoodDto;
import com.example.qlnh.dto.request.ComboRequest;
import com.example.qlnh.dto.response.ComboResponse;
import com.example.qlnh.exception.BusinessValidationException;
import com.example.qlnh.exception.ResourceNotFoundException;
import com.example.qlnh.models.entities.Combo;
import com.example.qlnh.models.entities.ComboFood;
import com.example.qlnh.models.entities.Food;
import com.example.qlnh.models.entities.User;
import com.example.qlnh.models.enums.ComboStatus;
import com.example.qlnh.repositories.ComboRepository;
import com.example.qlnh.repositories.FoodRepository;
import com.example.qlnh.services.interfaces.IComboService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComboService implements IComboService {

    private final ComboRepository comboRepository;
    private final FoodRepository foodRepository;

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true) // Tối ưu tốc độ đọc dữ liệu
    public Page<ComboResponse> getAllCombos(String keyword, String status, int page, int itemsPerPage) {

        // 1. Phân trang an toàn (Giới hạn tối đa 100 record/trang để chống tràn RAM)
        int safePage = Math.max(page - 1, 0);
        int safeItemsPerPage = Math.min(itemsPerPage, 100);

        // Sắp xếp Combo mới tạo lên đầu
        Pageable pageable = PageRequest.of(safePage, safeItemsPerPage, Sort.by("id").descending());

        // 2. Xử lý Enum an toàn cho Status
        ComboStatus comboStatus = null;
        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("all")) {
            try {
                comboStatus = ComboStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessValidationException(
                        "Trạng thái lọc không hợp lệ! (Chỉ dùng: AVAILABLE, UNAVAILABLE, STOP_SELLING)");
            }
        }

        // 3. Gọi siêu hàm tìm kiếm trong Repository
        // (Hàm searchCombos đã được tích hợp @EntityGraph chống N+1 Query)
        Page<Combo> combos = comboRepository.searchCombos(keyword, comboStatus, pageable);

        // 4. Map Entity sang DTO và trả về
        return combos.map(ComboResponse::fromEntity);
    }

    @Override
    @Transactional
    public Combo createCombo(ComboRequest request, User currentUser) {

        // 1. Kiểm tra lính gác (Tùy chọn: Chống trùng tên Combo)
        if (comboRepository.existsByName(request.getName().trim())) {
            throw new BusinessValidationException("Tên Combo '" + request.getName() + "' đã tồn tại!");
        }

        // 2. Kiểm tra điều kiện tiên quyết: Combo phải có ít nhất 1 món ăn
        if (request.getFoodItems() == null || request.getFoodItems().isEmpty()) {
            throw new BusinessValidationException("Một Combo phải bao gồm ít nhất 1 món ăn!");
        }

        // 3. Khởi tạo Combo và map dữ liệu cơ bản
        Combo combo = new Combo();
        combo.setName(request.getName().trim());
        combo.setPrice(request.getPrice());
        combo.setDescription(request.getDescription());
        combo.setImageUrl(request.getImageUrl());

        // (Tùy chọn) Gắn thông tin người tạo nếu Combo của bạn kế thừa AuditableEntity
        // combo.setCreatedBy(currentUser);

        // 4. Ép kiểu Enum an toàn cho Status
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            try {
                combo.setStatus(ComboStatus.valueOf(request.getStatus().trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BusinessValidationException(
                        "Trạng thái Combo không hợp lệ! (Chỉ dùng: AVAILABLE, UNAVAILABLE, STOP_SELLING)");
            }
        }

        // 5. Xử lý danh sách các món ăn (Bảng trung gian ComboFood)
        List<ComboFood> comboFoods = new ArrayList<>();

        for (ComboFoodDto item : request.getFoodItems()) {
            // Truy vấn lấy Food từ DB (Ném lỗi nếu truyền sai ID món ăn)
            Food food = foodRepository.findById(item.getFoodId())
                    .orElseThrow(
                            () -> new BusinessValidationException("Không tìm thấy món ăn với ID: " + item.getFoodId()));

            // Kiểm tra số lượng hợp lệ
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessValidationException("Số lượng món ăn trong Combo phải lớn hơn 0!");
            }

            // Tạo record trung gian
            ComboFood comboFood = new ComboFood();
            comboFood.setCombo(combo); // Chi tiết CỰC KỲ QUAN TRỌNG: Gắn Combo cha vào để JPA biết đường map khóa
                                       // ngoại
            comboFood.setFood(food);
            comboFood.setQuantity(item.getQuantity());

            comboFoods.add(comboFood);
        }

        // Gắn danh sách món vào Combo
        combo.setComboFoods(comboFoods);

        // 6. Lưu xuống Database (Lưu 1 phát ăn luôn cả bảng combo lẫn combo_foods nhờ
        // CascadeType.ALL)
        return comboRepository.save(combo);
    }

    @Override
    @Transactional
    public Combo updateCombo(Long id, ComboRequest request, User currentUser) {

        // 1. Tìm Combo (Ném lỗi 404 nếu không thấy)
        Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Combo với ID: " + id));

        // 2. Validate và Cập nhật Tên Combo (Chống trùng lặp nếu đổi tên)
        if (request.getName() != null && !request.getName().trim().equals(combo.getName())) {
            if (comboRepository.existsByName(request.getName().trim())) {
                throw new BusinessValidationException("Tên Combo '" + request.getName() + "' đã tồn tại!");
            }
            combo.setName(request.getName().trim());
        }

        // 3. Cập nhật các thông tin cơ bản
        if (request.getPrice() != null)
            combo.setPrice(request.getPrice());
        if (request.getDescription() != null)
            combo.setDescription(request.getDescription());
        if (request.getImageUrl() != null)
            combo.setImageUrl(request.getImageUrl());

        // 4. Cập nhật Enum Status an toàn
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            try {
                combo.setStatus(ComboStatus.valueOf(request.getStatus().trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BusinessValidationException("Trạng thái Combo không hợp lệ!");
            }
        }

        // 5. CẬP NHẬT DANH SÁCH MÓN ĂN TRONG COMBO
        if (request.getFoodItems() != null && !request.getFoodItems().isEmpty()) {

            // Xóa sạch bộ nhớ tạm các món cũ (Hibernate sẽ tự sinh lệnh DELETE dưới DB)
            combo.getComboFoods().clear();

            // Nạp lại danh sách món ăn mới vào
            for (ComboFoodDto item : request.getFoodItems()) {
                Food food = foodRepository.findById(item.getFoodId())
                        .orElseThrow(() -> new BusinessValidationException(
                                "Không tìm thấy món ăn với ID: " + item.getFoodId()));

                if (item.getQuantity() == null || item.getQuantity() <= 0) {
                    throw new BusinessValidationException("Số lượng món ăn phải lớn hơn 0!");
                }

                ComboFood comboFood = new ComboFood();
                comboFood.setCombo(combo); // Bắt buộc gắn Combo cha
                comboFood.setFood(food);
                comboFood.setQuantity(item.getQuantity());

                combo.getComboFoods().add(comboFood); // Thêm lại vào list
            }
        }

        // (Tùy chọn) combo.setUpdatedBy(currentUser);

        // 6. Lưu xuống DB
        return comboRepository.save(combo);
    }

    @Override
    @Transactional
    public void deleteCombo(Long id) {

        // 1. Tìm Combo (Ném lỗi 404 nếu không tồn tại)
        Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Combo với ID: " + id));

        // 2. Thực hiện xóa
        // (Sẽ tự động biến thành UPDATE Xóa mềm nếu đã cấu hình @SQLDelete ở Entity)
        comboRepository.delete(combo);
    }

    @Override
    @Transactional(readOnly = true)
    public ComboResponse getComboById(Long id) {
        Combo combo = comboRepository.findWithFoodsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Combo với ID: " + id));
        return ComboResponse.fromEntity(combo);
    }
}
