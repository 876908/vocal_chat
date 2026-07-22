package org.example.vocalchat.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vocalchat.common.enums.ErrorEnum;
import org.example.vocalchat.common.exception.BaseException;
import org.example.vocalchat.dto.response.UserInfoResponse;
import org.example.vocalchat.entity.User;
import org.example.vocalchat.mapper.UserMapper;
import org.example.vocalchat.service.UserService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public UserInfoResponse getUserInfo(String userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BaseException(ErrorEnum.USER_NOT_FOUND);
        }
        UserInfoResponse vo = UserInfoResponse.builder()
                .id(user.getId())
                .nickName(user.getNickName())
                .email(user.getEmail())
                .build();
        return vo;
    }
}
