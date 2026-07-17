package org.example.vocalchat.service;

import org.example.vocalchat.common.result.BaseResult;
import org.example.vocalchat.dto.request.LoginRequest;
import org.example.vocalchat.dto.request.RegisterRequest;

public interface AuthService {

    BaseResult<String> register(RegisterRequest request);

    BaseResult<String> login(LoginRequest request);

    BaseResult<Void> sendVerificationCode(String email);

    BaseResult<Void> logout(String userId);
}
