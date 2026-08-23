//用AI梭的一个tool示例
package org.example.vocalchat.service.agent.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.example.vocalchat.common.context.UserContext;
import org.example.vocalchat.entity.KnowledgeBase;
import org.example.vocalchat.entity.KnowledgeBaseFile;
import org.example.vocalchat.mapper.KnowledgeBaseFileMapper;
import org.example.vocalchat.mapper.KnowledgeBaseMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 知识库检索工具。
 *
 * <p>当前切片服务（Easy RAG）尚未接入，故先按知识库名称/文件名做关键词匹配，
 * 返回 LLM 可理解的文本摘要。待切片服务接入后，方法内部可升级为向量检索，
 * 方法签名与 {@code @Tool} 描述保持不变——工具封装让切片接入对 Agent 层完全透明。</p>
 */
@Component
@RequiredArgsConstructor
public class KnowledgeBaseSearchTool {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeBaseFileMapper fileMapper;

    /**
     * 注意：@Tool 的 value（工具描述）是 LLM 判断「何时用、怎么用」的唯一依据，必须清晰具体。
     * 1.17.2 中 value 是 String[]（多行描述），@P 的 value/description 二选一作参数描述。
     */
    @Tool(name = "search_knowledge_base",
          value = "在用户的个人知识库中按关键词搜索文档，返回匹配的知识库与文件名摘要。当用户提到'我的文档/知识库/资料'时使用。")
    public String searchKnowledgeBase(@P(value = "搜索关键词") String keyword) {
        String userId = UserContext.requireUserId();
        if (keyword == null || keyword.isBlank()) {
            return "搜索关键词不能为空";
        }

        List<KnowledgeBase> bases = knowledgeBaseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getUserId, userId)
                        .like(KnowledgeBase::getName, keyword));

        if (bases.isEmpty()) {
            return "未找到名称包含 '" + keyword + "' 的知识库";
        }

        StringBuilder sb = new StringBuilder();
        for (KnowledgeBase kb : bases) {
            sb.append("知识库[").append(kb.getName()).append("]")
              .append(" (文档数=").append(kb.getDocumentCount()).append(")\n");
            List<KnowledgeBaseFile> files = fileMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeBaseFile>()
                            .eq(KnowledgeBaseFile::getKnowledgeBaseId, kb.getId()));
            for (KnowledgeBaseFile f : files) {
                sb.append("  - ").append(f.getFileName())
                  .append(" (").append(f.getFileType()).append(", ").append(f.getStatus()).append(")\n");
            }
        }
        return sb.toString();
    }
}
