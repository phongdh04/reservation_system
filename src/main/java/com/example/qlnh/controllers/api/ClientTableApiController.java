package com.example.qlnh.controllers.api;

import com.example.qlnh.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/client/tables")
public class ClientTableApiController {


    @GetMapping("/availability")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAvailability(
            @RequestParam String date,
            @RequestParam String time) {
        // TODO: Lay so cho trong cho 1 khung gio cu the
        throw new UnsupportedOperationException("TODO: Implement getAvailability logic");
    }
}
