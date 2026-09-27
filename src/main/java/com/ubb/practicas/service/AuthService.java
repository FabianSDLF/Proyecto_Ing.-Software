package com.ubb.practicas.service;

import com.ubb.practicas.dto.AuthResponse;
import com.ubb.practicas.dto.LoginRequest;

import java.util.Map;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    Map<String, String> forgotPassword(String email);
    Map<String, String> resetPassword(String tokenStr, String newPassword);
}
