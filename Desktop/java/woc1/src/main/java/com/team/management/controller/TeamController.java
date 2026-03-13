package com.team.management.controller;

import com.team.management.entity.Team;
import com.team.management.enums.RoleEnum;
import com.team.management.service.TeamService;
import com.team.management.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/team")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final JwtUtil jwtUtil;

    @PostMapping("/audit/submit")
    public Boolean submitAudit(@RequestBody Team team) {
        return teamService.submitAudit(team);
    }

    @PostMapping("/audit/handle")
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public Boolean handleAudit(@RequestParam Long teamId,
                               @RequestParam String auditStatus,
                               @RequestParam String auditRemark,
                               HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        String username = jwtUtil.extractUsername(token);
        Long auditorId = 1L;
        return teamService.handleAudit(teamId, auditStatus, auditRemark, auditorId);
    }

    @GetMapping("/list")
    public List<Team> getTeamList(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Integer role = jwtUtil.extractRole(token);
        String username = jwtUtil.extractUsername(token);

        Long userId = 1L;
        return teamService.getTeamListByRole(userId, role);
    }
}