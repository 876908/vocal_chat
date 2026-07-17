package org.example.vocalchat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.vocalchat.common.annotation.SkipToken;
import org.example.vocalchat.common.context.UserContext;
import org.example.vocalchat.common.result.BaseResult;
import org.example.vocalchat.dto.request.LoginRequest;
import org.example.vocalchat.dto.request.RegisterRequest;
import org.example.vocalchat.dto.request.ResendVerificationRequest;
import org.example.vocalchat.dto.response.LoginResponse;
import org.example.vocalchat.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @SkipToken
    @PostMapping("/register")
    public BaseResult<Void> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @SkipToken
    @GetMapping("/verify-email")
    public BaseResult<Void> verifyEmail(@RequestParam String email, @RequestParam String code) {
        return authService.verifyEmail(email, code);
    }

    @SkipToken
    @PostMapping("/resend-verification")
    public BaseResult<Void> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        return authService.resendVerification(request);
    }

    @SkipToken
    @PostMapping("/login")
    public BaseResult<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public BaseResult<Void> logout() {
        return authService.logout(UserContext.getToken());
    }
}
