package org.example.vocalchat.dto.response;

import lombok.Data;

@Data
public class AIAssistantVO {
    private String id;
    private String name;
    private String description;
    private String character;
    private String knowledgeBaseId;
}
