package org.example.vocalchat.service;

import org.example.vocalchat.common.result.BaseResult;
import org.example.vocalchat.dto.request.LoginRequest;
import org.example.vocalchat.dto.request.RegisterRequest;
import org.example.vocalchat.dto.request.ResendVerificationRequest;
import org.example.vocalchat.dto.response.LoginResponse;

public interface AuthService {

    BaseResult<Void> register(RegisterRequest request);

    BaseResult<Void> verifyEmail(String email, String code);

    BaseResult<Void> resendVerification(ResendVerificationRequest request);

    BaseResult<LoginResponse> login(LoginRequest request);

    BaseResult<Void> logout(String token);
}
