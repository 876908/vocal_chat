package org.example.vocalchat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dialogue")
public class Dialogue {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String aiAssistantId;
    private String contexts;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
