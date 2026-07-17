package org.example.vocalchat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    private Long id;

    private String email;

    private String nickname;

    private String avatarUrl;

    private Integer gender;

    private String emailStatus;

    private String status;

    private LocalDateTime createdAt;
}
