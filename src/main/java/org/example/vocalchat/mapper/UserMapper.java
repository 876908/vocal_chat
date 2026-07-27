package org.example.vocalchat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.vocalchat.entity.User;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    User selectByEmail(String email);
}
