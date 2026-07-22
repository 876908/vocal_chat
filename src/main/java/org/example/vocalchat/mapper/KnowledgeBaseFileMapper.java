package org.example.vocalchat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.vocalchat.entity.KnowledgeBaseFile;

import java.util.List;

@Mapper
public interface KnowledgeBaseFileMapper extends BaseMapper<KnowledgeBaseFile> {

    @Select("SELECT * FROM knowledge_base_file WHERE knowledge_base_id = #{knowledgeBaseId} ORDER BY created_at DESC")
    List<KnowledgeBaseFile> selectByKnowledgeBaseId(@Param("knowledgeBaseId") String knowledgeBaseId);

    @Select("SELECT * FROM knowledge_base_file WHERE knowledge_base_id = #{knowledgeBaseId} AND id = #{fileId}")
    KnowledgeBaseFile selectByKbIdAndFileId(@Param("knowledgeBaseId") String knowledgeBaseId, @Param("fileId") String fileId);

    @Update("UPDATE knowledge_base_file SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") String id, @Param("status") String status);

    @Select("SELECT COUNT(*) FROM knowledge_base_file WHERE knowledge_base_id = #{knowledgeBaseId}")
    int countByKnowledgeBaseId(@Param("knowledgeBaseId") String knowledgeBaseId);
}
