package org.example.vocalchat.service;

import org.example.vocalchat.dto.request.LoginRequest;
import org.example.vocalchat.dto.request.RegisterRequest;

public interface AuthService {

    String register(RegisterRequest request);

    String login(LoginRequest request);

    void sendVerificationCode(String email);

    void logout(String userId);
}
