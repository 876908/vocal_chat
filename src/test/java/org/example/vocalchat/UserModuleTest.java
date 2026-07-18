package org.example.vocalchat;

import org.example.vocalchat.common.exception.BaseException;
import org.example.vocalchat.common.util.JwtUtil;
import org.example.vocalchat.dto.request.LoginRequest;
import org.example.vocalchat.dto.request.RegisterRequest;
import org.example.vocalchat.dto.response.UserInfoResponse;
import org.example.vocalchat.entity.User;
import org.example.vocalchat.infrastructure.service.EmailService;
import org.example.vocalchat.mapper.UserMapper;
import org.example.vocalchat.service.AuthService;
import org.example.vocalchat.service.UserService;
import org.example.vocalchat.service.impl.AuthServiceImpl;
import org.example.vocalchat.service.impl.UserServiceImpl;
import org.junit.jupiter.api.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserModuleTest {

    private static final String TEST_EMAIL = "test@vocalchat.com";
    private static final String TEST_PASSWORD = "Test1234";
    private static final String TEST_NICKNAME = "TestUser";
    private static final String TEST_USER_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";

    private final UserMapper userMapper = mock(UserMapper.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final EmailService emailService = mock(EmailService.class);
    private final AuthService authService = new AuthServiceImpl(userMapper, passwordEncoder, jwtUtil, emailService);
    private final UserService userService = new UserServiceImpl(userMapper);

    @BeforeEach
    void setUp() {
        reset(userMapper, jwtUtil, emailService);
    }

    @Test
    @Order(1)
    @DisplayName("sendVerificationCode - 发送验证码成功")
    void testSendVerificationCodeSuccess() {
        when(userMapper.selectByEmail(TEST_EMAIL)).thenReturn(null);

        authService.sendVerificationCode(TEST_EMAIL);

        verify(emailService).sendVerificationCode(TEST_EMAIL);
    }

    @Test
    @Order(2)
    @DisplayName("sendVerificationCode - 邮箱已注册则抛异常")
    void testSendVerificationCodeEmailExists() {
        User existing = User.builder().id(TEST_USER_ID).email(TEST_EMAIL).build();
        when(userMapper.selectByEmail(TEST_EMAIL)).thenReturn(existing);

        assertThrows(BaseException.class, () -> authService.sendVerificationCode(TEST_EMAIL));
        verify(emailService, never()).sendVerificationCode(anyString());
    }

    @Test
    @Order(3)
    @DisplayName("register - 注册成功返回 JWT Token")
    void testRegisterSuccess() {
        when(userMapper.selectByEmail(TEST_EMAIL)).thenReturn(null);
        when(emailService.verifyCode(TEST_EMAIL, "123456")).thenReturn(true);
        when(jwtUtil.generateToken(anyMap())).thenReturn("jwt-token-xxx");

        RegisterRequest req = new RegisterRequest();
        req.setNickName(TEST_NICKNAME);
        req.setEmail(TEST_EMAIL);
        req.setPassword(TEST_PASSWORD);
        req.setVerificationCode("123456");

        String token = authService.register(req);

        assertEquals("jwt-token-xxx", token);
        verify(userMapper).insert(any(User.class));
    }

    @Test
    @Order(4)
    @DisplayName("register - 验证码错误")
    void testRegisterWrongCode() {
        when(userMapper.selectByEmail(TEST_EMAIL)).thenReturn(null);
        when(emailService.verifyCode(TEST_EMAIL, "999999")).thenReturn(false);

        RegisterRequest req = new RegisterRequest();
        req.setNickName(TEST_NICKNAME);
        req.setEmail(TEST_EMAIL);
        req.setPassword(TEST_PASSWORD);
        req.setVerificationCode("999999");

        assertThrows(BaseException.class, () -> authService.register(req));
    }

    @Test
    @Order(5)
    @DisplayName("register - 密码太弱")
    void testRegisterWeakPassword() {
        RegisterRequest req = new RegisterRequest();
        req.setNickName(TEST_NICKNAME);
        req.setEmail(TEST_EMAIL);
        req.setPassword("123");
        req.setVerificationCode("123456");

        assertThrows(BaseException.class, () -> authService.register(req));
    }

    @Test
    @Order(6)
    @DisplayName("register - 邮箱已注册")
    void testRegisterEmailExists() {
        User existing = User.builder().id(TEST_USER_ID).email(TEST_EMAIL).build();
        when(userMapper.selectByEmail(TEST_EMAIL)).thenReturn(existing);

        RegisterRequest req = new RegisterRequest();
        req.setNickName(TEST_NICKNAME);
        req.setEmail(TEST_EMAIL);
        req.setPassword(TEST_PASSWORD);
        req.setVerificationCode("123456");

        assertThrows(BaseException.class, () -> authService.register(req));
    }

    @Test
    @Order(7)
    @DisplayName("login - 登录成功返回 JWT Token")
    void testLoginSuccess() {
        String hashedPassword = passwordEncoder.encode(TEST_PASSWORD);
        User user = User.builder()
                .id(TEST_USER_ID).email(TEST_EMAIL)
                .password(hashedPassword).nickName(TEST_NICKNAME)
                .build();
        when(userMapper.selectByEmail(TEST_EMAIL)).thenReturn(user);
        when(jwtUtil.generateToken(anyMap())).thenReturn("jwt-login-token");

        LoginRequest req = new LoginRequest();
        req.setEmail(TEST_EMAIL);
        req.setPassword(TEST_PASSWORD);

        String token = authService.login(req);

        assertEquals("jwt-login-token", token);
    }

    @Test
    @Order(8)
    @DisplayName("login - 密码错误")
    void testLoginWrongPassword() {
        String hashedPassword = passwordEncoder.encode(TEST_PASSWORD);
        User user = User.builder()
                .id(TEST_USER_ID).email(TEST_EMAIL)
                .password(hashedPassword).build();
        when(userMapper.selectByEmail(TEST_EMAIL)).thenReturn(user);

        LoginRequest req = new LoginRequest();
        req.setEmail(TEST_EMAIL);
        req.setPassword("WrongPass1");

        assertThrows(BaseException.class, () -> authService.login(req));
    }

    @Test
    @Order(9)
    @DisplayName("login - 用户不存在")
    void testLoginUserNotFound() {
        when(userMapper.selectByEmail("ghost@test.com")).thenReturn(null);

        LoginRequest req = new LoginRequest();
        req.setEmail("ghost@test.com");
        req.setPassword(TEST_PASSWORD);

        assertThrows(BaseException.class, () -> authService.login(req));
    }

    @Test
    @Order(10)
    @DisplayName("logout - 退出登录清理 Redis")
    void testLogout() {
        authService.logout(TEST_USER_ID);

        verify(jwtUtil).invalidateToken(TEST_USER_ID);
    }

    @Test
    @Order(11)
    @DisplayName("getUserInfo - 获取用户信息成功")
    void testGetUserInfo() {
        User user = User.builder()
                .id(TEST_USER_ID).nickName(TEST_NICKNAME)
                .email(TEST_EMAIL).build();
        when(userMapper.selectById(TEST_USER_ID)).thenReturn(user);

        UserInfoResponse info = userService.getUserInfo(TEST_USER_ID);

        assertEquals(TEST_USER_ID, info.getId());
        assertEquals(TEST_NICKNAME, info.getNickName());
        assertEquals(TEST_EMAIL, info.getEmail());
    }

    @Test
    @Order(12)
    @DisplayName("getUserInfo - 用户不存在")
    void testGetUserInfoNotFound() {
        when(userMapper.selectById("nonexistent")).thenReturn(null);

        assertThrows(BaseException.class, () -> userService.getUserInfo("nonexistent"));
    }
}
