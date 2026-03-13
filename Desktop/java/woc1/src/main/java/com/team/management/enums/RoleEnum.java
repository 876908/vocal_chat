package com.team.management.enums;

import lombok.Getter;

@Getter
public enum RoleEnum {
    ADMIN(1, "管理员"),
    MEMBER(2, "普通成员"),
    AUDITOR(3, "审核员");

    private final Integer code;
    private final String desc;

    RoleEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static RoleEnum getByCode(Integer code) {
        for (RoleEnum role : values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        return null;
    }
}