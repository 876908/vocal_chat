package com.team.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("team_info")
public class Team {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String teamName;
    private String teamDesc;
    private Long ownerId;
    private String auditStatus;
    private String auditRemark;
    private Long auditorId;
    private LocalDateTime createTime;
    private LocalDateTime auditTime;
}