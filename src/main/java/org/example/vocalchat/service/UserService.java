package org.example.vocalchat.service;

import org.example.vocalchat.common.result.BaseResult;
import org.example.vocalchat.dto.response.UserVO;

public interface UserService {

    BaseResult<UserVO> getUserInfo(Long userId);

    BaseResult<UserVO> updateUserInfo(Long userId, String nickname, String avatarUrl, Integer gender);

    BaseResult<Void> changePassword(Long userId, String oldPassword, String newPassword);
}
