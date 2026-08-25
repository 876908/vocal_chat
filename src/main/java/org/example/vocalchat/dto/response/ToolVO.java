package org.example.vocalchat.dto.response;

import lombok.Data;

/**
 * 可用工具项（对应 api.md 4.3 的 ToolVO）。
 */
@Data
public class ToolVO {
    /** 固定为 "tool" */
    private String type;
    /** 工具名 */
    private String tool;
    /** 工具功能描述 */
    private String content;
}
