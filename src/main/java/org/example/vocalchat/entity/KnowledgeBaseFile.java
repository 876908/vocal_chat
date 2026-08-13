package org.example.vocalchat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("knowledge_base_file")
public class KnowledgeBaseFile {

    @TableId
    private String id;

    private String knowledgeBaseId;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private String storageKey;

    private String status;

    private Integer chunkCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
