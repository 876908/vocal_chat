package org.example.vocalchat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vocalchat.common.enums.ErrorEnum;
import org.example.vocalchat.common.exception.BaseException;
import org.example.vocalchat.common.result.BaseResult;
import org.example.vocalchat.common.util.JwtUtil;
import org.example.vocalchat.dto.request.LoginRequest;
import org.example.vocalchat.dto.request.RegisterRequest;
import org.example.vocalchat.dto.request.ResendVerificationRequest;
import org.example.vocalchat.dto.response.LoginResponse;
import org.example.vocalchat.entity.User;
import org.example.vocalchat.infrastructure.service.EmailService;
import org.example.vocalchat.mapper.UserMapper;
import org.example.vocalchat.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int MAX_LOGIN_FAIL_COUNT = 5;
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-zA-Z])(?=.*\\d).{8,20}$");

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    @Override
    @Transactional
    public BaseResult<Void> register(RegisterRequest request) {

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BaseException(ErrorEnum.PASSWORD_NOT_MATCH);
        }


        if (!PASSWORD_PATTERN.matcher(request.getPassword()).matches()) {
            throw new BaseException(ErrorEnum.PASSWORD_WEAK);
        }


        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail()));
        if (existing != null) {
            throw new BaseException(ErrorEnum.EMAIL_EXISTS);
        }


        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getEmail().split("@")[0])
                .gender(0)
                .emailStatus("UNVERIFIED")
                .status("ACTIVE")
                .loginFailCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userMapper.insert(user);


        try {
            emailService.sendVerificationCode(request.getEmail());
        } catch (Exception e) {
            log.warn("注册后发送验证邮件失败: {}", request.getEmail(), e);

        }

        log.info("用户注册成功: email={}, id={}", request.getEmail(), user.getId());
        return BaseResult.success();
    }

    @Override
    public BaseResult<Void> verifyEmail(String email, String code) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new BaseException(ErrorEnum.USER_NOT_FOUND);
        }

        if ("VERIFIED".equals(user.getEmailStatus())) {
            return BaseResult.success();
        }

        boolean valid = emailService.verifyCode(email, code);
        if (!valid) {
            throw new BaseException(ErrorEnum.VERIFICATION_CODE_ERROR);
        }

        userMapper.updateEmailStatus(user.getId(), "VERIFIED");
        log.info("邮箱验证成功: email={}", email);
        return BaseResult.success();
    }

    @Override
    public BaseResult<Void> resendVerification(ResendVerificationRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail()));
        if (user == null) {
            throw new BaseException(ErrorEnum.USER_NOT_FOUND);
        }

        if ("VERIFIED".equals(user.getEmailStatus())) {
            return BaseResult.success();
        }

        emailService.sendVerificationCode(request.getEmail());
        log.info("重新发送验证邮件: email={}", request.getEmail());
        return BaseResult.success();
    }

    @Override
    public BaseResult<LoginResponse> login(LoginRequest request) {

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail()));
        if (user == null) {
            throw new BaseException(ErrorEnum.USER_NOT_FOUND);
        }


        if ("LOCKED".equals(user.getStatus())) {
            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
                throw new BaseException(ErrorEnum.ACCOUNT_LOCKED);
            }

            userMapper.updateLockStatus(user.getId(), "ACTIVE", null);
            userMapper.updateLoginFailCount(user.getId(), 0);
            user.setStatus("ACTIVE");
            user.setLoginFailCount(0);
        }
        if ("DELETED".equals(user.getStatus())) {
            throw new BaseException(ErrorEnum.ACCOUNT_DISABLED);
        }


        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            int failCount = user.getLoginFailCount() + 1;
            userMapper.updateLoginFailCount(user.getId(), failCount);
            if (failCount >= MAX_LOGIN_FAIL_COUNT) {
                userMapper.updateLockStatus(user.getId(), "LOCKED",
                        LocalDateTime.now().plusHours(24));
                log.warn("用户登录失败达{}次，锁定24小时: email={}", failCount, request.getEmail());
                throw new BaseException(ErrorEnum.ACCOUNT_LOCKED);
            }
            throw new BaseException(ErrorEnum.PASSWORD_ERROR);
        }


        userMapper.updateLoginFailCount(user.getId(), 0);


        String token = jwtUtil.generateToken(Map.of("userId", user.getId().toString()));


        LoginResponse response = LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .build();

        log.info("用户登录成功: email={}, id={}", request.getEmail(), user.getId());
        return BaseResult.success(response);
    }

    @Override
    public BaseResult<Void> logout(String token) {
        if (token != null && !token.isBlank()) {
            jwtUtil.invalidateToken(token);
        }
        return BaseResult.success();
    }
}
