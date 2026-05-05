package org.javaup.ai.chatagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.chat.agentic-rag")
public class ChatAgenticRagProperties {

    private boolean enabled = false;

    private int maxSteps = 3;

    private boolean includeEvidenceEvaluationStep = true;
}
