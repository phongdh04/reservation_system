package com.example.qlnh.services.interfaces;

import com.example.qlnh.dto.request.LoginRequestDTO;
import com.example.qlnh.dto.response.LoginResponseDTO;

public interface IAuthService {
    LoginResponseDTO loginUser(LoginRequestDTO request);
}
