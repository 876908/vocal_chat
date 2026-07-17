package org.example.vocalchat.controller;

import lombok.RequiredArgsConstructor;
import org.example.vocalchat.common.context.UserContext;
import org.example.vocalchat.common.result.BaseResult;
import org.example.vocalchat.dto.response.UserInfoVO;
import org.example.vocalchat.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/info")
    public BaseResult<UserInfoVO> getInfo() {
        return userService.getUserInfo(UserContext.getUserId());
    }
}
