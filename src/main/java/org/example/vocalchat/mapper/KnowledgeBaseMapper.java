package org.example.vocalchat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.example.vocalchat.entity.KnowledgeBase;

@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {

    @Update("UPDATE knowledge_base SET document_count = document_count + #{delta}, updated_at = NOW() WHERE id = #{id}")
    int incrementDocumentCount(@Param("id") String id, @Param("delta") int delta);

    @Update("UPDATE knowledge_base SET chunk_count = chunk_count + #{delta}, updated_at = NOW() WHERE id = #{id}")
    int incrementChunkCount(@Param("id") String id, @Param("delta") int delta);
}
