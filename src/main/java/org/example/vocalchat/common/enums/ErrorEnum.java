package org.example.vocalchat.common.enums;

import lombok.Getter;

@Getter
public enum ErrorEnum {

    // === 通用 ===
    SUCCESS(0, "success"),
    SYSTEM_ERROR(5000, "系统内部错误"),

    // === Token 1xxx ===
    TOKEN_EXPIRED(1001, "Token 已失效或不存在"),
    TOKEN_INVALID(1002, "Token 无效"),
    TOKEN_MISSING(1003, "未提供 Token"),

    // === 用户 2xxx ===
    USER_NOT_FOUND(2001, "用户不存在"),
    PASSWORD_ERROR(2002, "密码错误"),
    USERNAME_EXISTS(2003, "用户名已存在"),
    EMAIL_EXISTS(2004, "邮箱已注册"),

    // === 参数校验 3xxx ===
    PARAM_ERROR(3001, "参数错误"),
    PARAM_MISSING(3002, "缺少必要参数"),

    // === 业务 4xxx ===
    AI_ASSISTANT_NOT_FOUND(4001, "AI 助手不存在"),
    KNOWLEDGE_BASE_NOT_FOUND(4002, "知识库不存在"),
    FILE_UPLOAD_FAILED(4003, "文件上传失败"),
    AGENT_LOOP_EXCEEDED(4004, "Agent 执行轮次超限"),
    ;

    private final Integer code;
    private final String msg;

    ErrorEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
