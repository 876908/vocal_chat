package org.example.vocalchat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAssistantRequest {
    @NotBlank(message = "助手名称不能为空")
    @Size(max = 50, message = "名称最长50字符")
    private String name;

    @Size(max = 200, message = "描述最长200字符")
    private String description;

    @Size(max = 500, message = "角色设定最长500字符")
    private String character;

    private String knowledgeBaseId;
}
