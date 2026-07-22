package org.example.vocalchat.service;

import org.example.vocalchat.dto.response.UserInfoResponse;

public interface UserService {

    UserInfoResponse getUserInfo(String userId);
}
