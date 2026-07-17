package org.example.vocalchat.dto.request;

import lombok.Data;

@Data
public class UpdateUserRequest {

    private String nickname;

    private String avatarUrl;

    private Integer gender;
}
