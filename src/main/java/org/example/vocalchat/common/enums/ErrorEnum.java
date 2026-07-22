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
    ACCOUNT_LOCKED(2005, "账户已被锁定，请24小时后重试"),
    ACCOUNT_DISABLED(2006, "账户已被禁用"),
    PASSWORD_WEAK(2007, "密码需8-20位且包含字母和数字"),
    PASSWORD_NOT_MATCH(2008, "两次输入的密码不一致"),
    EMAIL_NOT_VERIFIED(2009, "邮箱未验证，请先验证邮箱"),
    VERIFICATION_CODE_ERROR(2010, "验证码错误"),
    VERIFICATION_CODE_EXPIRED(2011, "验证码已过期"),

    // === 参数校验 3xxx ===
    PARAM_ERROR(3001, "参数错误"),
    PARAM_MISSING(3002, "缺少必要参数"),

    // === 业务 4xxx ===
    AI_ASSISTANT_NOT_FOUND(4001, "AI 助手不存在"),
    KNOWLEDGE_BASE_NOT_FOUND(4002, "知识库不存在"),
    FILE_UPLOAD_FAILED(4003, "文件上传失败"),
    AGENT_LOOP_EXCEEDED(4004, "Agent 执行轮次超限"),
    OBJECT_STORAGE_ERROR(4005, "对象存储操作失败"),
    LLM_CALL_FAILED(4006, "LLM 调用失败"),
    ;

    private final Integer code;
    private final String msg;

    ErrorEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
