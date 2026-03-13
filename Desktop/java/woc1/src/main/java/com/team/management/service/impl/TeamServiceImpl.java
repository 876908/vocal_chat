package com.team.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.team.management.entity.Team;
import com.team.management.enums.AuditStatusEnum;
import com.team.management.enums.RoleEnum;
import com.team.management.mapper.TeamMapper;
import com.team.management.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team> implements TeamService {

    @Override
    public Boolean submitAudit(Team team) {
        team.setAuditStatus(AuditStatusEnum.PENDING.getCode());
        team.setCreateTime(LocalDateTime.now());
        return this.save(team);
    }

    @Override
    public Boolean handleAudit(Long teamId, String auditStatus, String auditRemark, Long auditorId) {
        Team team = this.getById(teamId);
        if (team == null) {
            throw new RuntimeException("队伍不存在");
        }
        team.setAuditStatus(auditStatus);
        team.setAuditRemark(auditRemark);
        team.setAuditorId(auditorId);
        team.setAuditTime(LocalDateTime.now());
        return this.updateById(team);
    }

    @Override
    public List<Team> getTeamListByRole(Long userId, Integer role) {
        LambdaQueryWrapper<Team> wrapper = new LambdaQueryWrapper<>();
        RoleEnum roleEnum = RoleEnum.getByCode(role);
        switch (roleEnum) {
            case ADMIN:
                break;
            case AUDITOR:
                wrapper.eq(Team::getAuditStatus, AuditStatusEnum.PENDING.getCode());
                break;
            case MEMBER:
                wrapper.eq(Team::getOwnerId, userId);
                break;
            default:
                throw new RuntimeException("角色不合法");
        }
        return this.list(wrapper);
    }
}