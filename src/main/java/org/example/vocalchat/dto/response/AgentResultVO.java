package org.example.vocalchat.dto.response;

import lombok.Data;

import java.util.List;

/**
 * Agent 同步执行结果（对应 api.md 4.3 的 AgentResultVO）。
 */
@Data
public class AgentResultVO {
    private String finalAnswer;
    private boolean success;
    private List<AgentStepVO> steps;
}
