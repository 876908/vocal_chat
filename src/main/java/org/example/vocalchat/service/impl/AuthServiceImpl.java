package org.example.vocalchat.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vocalchat.common.enums.ErrorEnum;
import org.example.vocalchat.common.exception.BaseException;
import org.example.vocalchat.common.result.BaseResult;
import org.example.vocalchat.common.util.JwtUtil;
import org.example.vocalchat.dto.request.LoginRequest;
import org.example.vocalchat.dto.request.RegisterRequest;
import org.example.vocalchat.entity.User;
import org.example.vocalchat.infrastructure.service.EmailService;
import org.example.vocalchat.mapper.UserMapper;
import org.example.vocalchat.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-zA-Z])(?=.*\\d).{8,20}$");

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    @Override
    public BaseResult<String> register(RegisterRequest request) {
        if (!PASSWORD_PATTERN.matcher(request.getPassword()).matches()) {
            throw new BaseException(ErrorEnum.PASSWORD_WEAK);
        }

        User existing = userMapper.selectByEmail(request.getEmail());
        if (existing != null) {
            throw new BaseException(ErrorEnum.EMAIL_EXISTS);
        }

        boolean valid = emailService.verifyCode(request.getEmail(), request.getVerificationCode());
        if (!valid) {
            throw new BaseException(ErrorEnum.VERIFICATION_CODE_ERROR);
        }

        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .nickName(request.getNickName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        userMapper.insert(user);

        String token = jwtUtil.generateToken(Map.of("userId", user.getId()));

        log.info("用户注册成功: email={}, id={}", request.getEmail(), user.getId());
        return BaseResult.success(token);
    }

    @Override
    public BaseResult<String> login(LoginRequest request) {
        User user = userMapper.selectByEmail(request.getEmail());
        if (user == null) {
            throw new BaseException(ErrorEnum.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BaseException(ErrorEnum.PASSWORD_ERROR);
        }

        String token = jwtUtil.generateToken(Map.of("userId", user.getId()));

        log.info("用户登录成功: email={}, id={}", request.getEmail(), user.getId());
        return BaseResult.success(token);
    }

    @Override
    public BaseResult<Void> sendVerificationCode(String email) {
        User user = userMapper.selectByEmail(email);
        if (user != null) {
            throw new BaseException(ErrorEnum.EMAIL_EXISTS);
        }
        emailService.sendVerificationCode(email);
        return BaseResult.success();
    }

    @Override
    public BaseResult<Void> logout(String userId) {
        jwtUtil.invalidateToken(userId);
        return BaseResult.success();
    }
}
