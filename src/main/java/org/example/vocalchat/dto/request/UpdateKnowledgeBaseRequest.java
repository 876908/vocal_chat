package org.example.vocalchat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateKnowledgeBaseRequest {

    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 100, message = "知识库名称最长100字符")
    private String name;

    @Size(max = 200, message = "知识库描述最长200字符")
    private String description;
}
