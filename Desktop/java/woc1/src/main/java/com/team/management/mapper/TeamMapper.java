package com.team.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.team.management.entity.Team;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TeamMapper extends BaseMapper<Team> {
}