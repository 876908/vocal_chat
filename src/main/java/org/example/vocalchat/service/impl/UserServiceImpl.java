package org.example.vocalchat.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vocalchat.common.enums.ErrorEnum;
import org.example.vocalchat.common.exception.BaseException;
import org.example.vocalchat.common.result.BaseResult;
import org.example.vocalchat.dto.response.UserVO;
import org.example.vocalchat.entity.User;
import org.example.vocalchat.mapper.UserMapper;
import org.example.vocalchat.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-zA-Z])(?=.*\\d).{8,20}$");

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public BaseResult<UserVO> getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BaseException(ErrorEnum.USER_NOT_FOUND);
        }
        return BaseResult.success(toUserVO(user));
    }

    @Override
    public BaseResult<UserVO> updateUserInfo(Long userId, String nickname, String avatarUrl, Integer gender) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BaseException(ErrorEnum.USER_NOT_FOUND);
        }

        if (nickname != null && !nickname.isBlank()) {
            user.setNickname(nickname);
        }
        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl);
        }
        if (gender != null) {
            user.setGender(gender);
        }
        userMapper.updateById(user);

        log.info("用户信息更新成功: id={}", userId);
        return BaseResult.success(toUserVO(user));
    }

    @Override
    public BaseResult<Void> changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BaseException(ErrorEnum.USER_NOT_FOUND);
        }


        if (user.getPassword() != null && !passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BaseException(ErrorEnum.PASSWORD_ERROR);
        }


        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new BaseException(ErrorEnum.PASSWORD_WEAK);
        }

        userMapper.updatePassword(userId, passwordEncoder.encode(newPassword));
        log.info("密码修改成功: id={}", userId);
        return BaseResult.success();
    }

    private UserVO toUserVO(User user) {
        return UserVO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .gender(user.getGender())
                .emailStatus(user.getEmailStatus())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
