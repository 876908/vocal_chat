package org.example.vocalchat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
//指明映射的数据表名
@TableName("dialogue")
public class Dialogue {
    //表示id是表的主键，并且由mybatis-plus直接分配
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String aiAssistantId;
    private String contexts; //存上下文所有信息
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
