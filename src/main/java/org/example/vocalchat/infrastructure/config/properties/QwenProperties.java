package org.example.vocalchat.infrastructure.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "vocal-chat.llm.qwen")
public class QwenProperties {

    private boolean enabled = true;

    private String apiKey;

    private String baseUrl;

    private String modelName = "qwen-plus";

    private String searchModelName = "qwen-plus";

    private String deepThinkingModelName = "qwen-plus";

    private Float temperature = 0.7F;

    private Double topP = 0.8D;

    private Integer topK;

    private Integer maxTokens = 2048;

    private Integer thinkingBudget = 1024;

    private Boolean supportIncrementalOutput = true;

    private Boolean enableSanitizeMessages = true;
}
