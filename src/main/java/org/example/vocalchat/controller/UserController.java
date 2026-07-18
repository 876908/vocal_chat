package org.example.vocalchat.controller;

import lombok.RequiredArgsConstructor;
import org.example.vocalchat.common.annotation.AutoResult;
import org.example.vocalchat.common.context.UserContext;
import org.example.vocalchat.dto.response.UserInfoResponse;
import org.example.vocalchat.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AutoResult
@RestController
@RequestMapping("/api/public/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/info")
    public UserInfoResponse getInfo() {
        return userService.getUserInfo(UserContext.getUserId());
    }
}
