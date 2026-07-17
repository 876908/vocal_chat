package org.example.vocalchat.service;

import org.example.vocalchat.common.result.BaseResult;
import org.example.vocalchat.dto.response.UserInfoVO;

public interface UserService {

    BaseResult<UserInfoVO> getUserInfo(String userId);
}
