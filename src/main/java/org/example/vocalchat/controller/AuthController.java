package org.example.vocalchat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.vocalchat.common.annotation.AutoResult;
import org.example.vocalchat.common.annotation.LogOperation;
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

    @LogOperation("用户注册")
    @SkipToken
    @PostMapping("/register")
    public String register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }
    @LogOperation("用户登录")
    @SkipToken
    @PostMapping("/login")
    public String login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @LogOperation("获取验证码")
    @SkipToken
    @PostMapping("/getVerificationCode")
    public void getVerificationCode(@RequestParam String email) {
        authService.sendVerificationCode(email);
    }

    @LogOperation("用户登出")
    @PostMapping("/logout")
    public void logout() {
        authService.logout(UserContext.getUserId());
    }
}
