package org.example.vocalchat.dto.response;

import lombok.Data;

/**
 * Agent 执行步骤（对应 api.md 4.3 的 steps 数组元素）。
 *
 * <p>type 取值：thinking | tool_call | tool_result。</p>
 */
@Data
public class AgentStepVO {
    /** thinking | tool_call | tool_result */
    private String type;
    /** 思考内容（thinking 时） */
    private String content;
    /** 工具名（tool_call / tool_result 时） */
    private String tool;
    /** 工具参数（tool_call 时） */
    private Object args;
    /** 工具结果摘要（tool_result 时） */
    private String result;
    /** 工具是否执行成功 */
    private boolean success;
    /** 时间戳（毫秒） */
    private long timestamp;
}
