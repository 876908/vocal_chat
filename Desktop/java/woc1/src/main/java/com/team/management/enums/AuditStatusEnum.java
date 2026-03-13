package com.team.management.enums;

import lombok.Getter;

@Getter
public enum AuditStatusEnum {
    PENDING("PENDING", "待审核"),
    PASS("PASS", "审核通过"),
    REJECT("REJECT", "审核拒绝");

    private final String code;
    private final String desc;

    AuditStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
