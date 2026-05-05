package org.javaup.ai.chatagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.chat.crag")
public class ChatCragProperties {

    private boolean enabled = false;

    private boolean correctiveRetrievalEnabled = false;

    private int minReferenceCount = 1;

    private int minCoveredSubQuestionCount = 1;

    private int maxCorrectiveRounds = 1;

    private String evaluatorMode = "rule";

    private String correctiveQuerySuffix = "请扩大同义词、章节标题和关键实体范围重新检索。";
}
