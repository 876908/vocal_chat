package org.example.vocalchat.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ModifyAssistantConfigRequest {
    @NotBlank(message = "助手ID不能为空")
    private String aiAssistantId;

    private String name;
    private String description;
    private String character;
    private String knowledgeBaseId;
}
