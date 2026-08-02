package org.example.vocalchat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_assistant")
public class AIAssistant {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String userId;
    private String name;
    private String description;
    private String assistantCharacter;
    private String knowledgeBaseId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
