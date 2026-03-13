package com.team.management.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.team.management.entity.Team;

import java.util.List;

public interface TeamService extends IService<Team> {
    Boolean submitAudit(Team team);

    Boolean handleAudit(Long teamId, String auditStatus, String auditRemark, Long auditorId);

    List<Team> getTeamListByRole(Long userId, Integer role);
}