package org.example.vocalchat.service.agent.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vocalchat.common.context.UserContext;
import org.example.vocalchat.infrastructure.service.EmailService;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class EmailTool {

    private final EmailService emailService;

    /**
     * 注意：@Tool 的 value（工具描述）是 LLM 判断「何时用、怎么用」的唯一依据，必须清晰具体。
     */
    @Tool(name = "send_email",
          value = "向指定邮箱地址发送一封邮件，需要收件人地址、主题和正文。当用户要求'发送邮件、给某人发邮件、邮件通知'时使用，邮件正文使用纯文本。")
    public String sendEmail(@P(value = "收件人邮箱地址") String to,
                            @P(value = "邮件主题") String subject,
                            @P(value = "邮件正文（纯文本）") String content) {
        String userId = UserContext.requireUserId();
        log.info("Agent 发送邮件: userId={}, to={}, subject={}", userId, to, subject);
        try {
            emailService.sendEmail(to, subject, content);
            return "邮件已成功发送至 " + to;
        } catch (Exception e) {
            log.warn("send_email 工具调用失败: {}", e.getMessage());
            return "邮件发送失败: " + e.getMessage();
        }
    }
}
