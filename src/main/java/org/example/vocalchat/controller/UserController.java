package org.example.vocalchat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.vocalchat.common.context.UserContext;
import org.example.vocalchat.common.result.BaseResult;
import org.example.vocalchat.dto.request.ChangePasswordRequest;
import org.example.vocalchat.dto.request.UpdateUserRequest;
import org.example.vocalchat.dto.response.UserVO;
import org.example.vocalchat.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/info")
    public BaseResult<UserVO> getUserInfo() {
        Long userId = Long.parseLong(UserContext.getUserId());
        return userService.getUserInfo(userId);
    }

    @PutMapping("/info")
    public BaseResult<UserVO> updateUserInfo(@Valid @RequestBody UpdateUserRequest request) {
        Long userId = Long.parseLong(UserContext.getUserId());
        return userService.updateUserInfo(userId, request.getNickname(),
                request.getAvatarUrl(), request.getGender());
    }

    @PutMapping("/password")
    public BaseResult<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = Long.parseLong(UserContext.getUserId());
        return userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
    }
}
