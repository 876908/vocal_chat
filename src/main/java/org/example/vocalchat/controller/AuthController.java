package org.example.vocalchat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.vocalchat.common.annotation.AutoResult;
import org.example.vocalchat.common.annotation.SkipToken;
import org.example.vocalchat.common.context.UserContext;
import org.example.vocalchat.dto.request.LoginRequest;
import org.example.vocalchat.dto.request.RegisterRequest;
import org.example.vocalchat.service.AuthService;
import org.springframework.web.bind.annotation.*;

@AutoResult
@RestController
@RequestMapping("/api/public/user")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @SkipToken
    @PostMapping("/register")
    public String register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @SkipToken
    @PostMapping("/login")
    public String login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @SkipToken
    @PostMapping("/getVerificationCode")
    public void getVerificationCode(@RequestParam String email) {
        authService.sendVerificationCode(email);
    }

    @PostMapping("/logout")
    public void logout() {
        authService.logout(UserContext.getUserId());
    }
}
