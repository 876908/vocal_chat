package org.example.vocalchat.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StreamRequest {
    @NotBlank(message = "问题不能为空")
    private String question;

    @NotBlank(message = "助手ID不能为空")
    private String aiAssistantId;

    private boolean enableOnlineSearch;
    private boolean enableDeepThinking;
}
