package com.team.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.team.management.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}