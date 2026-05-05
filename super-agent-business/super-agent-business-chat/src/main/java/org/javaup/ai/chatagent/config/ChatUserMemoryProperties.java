package org.javaup.ai.chatagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.chat.user-memory")
public class ChatUserMemoryProperties {

    private boolean enabled = true;

    private boolean vectorRecallEnabled = true;

    private boolean extractionEnabled = true;

    private String defaultTenantId = "default";

    private String defaultUserId = "admin";

    private int recallTopK = 5;

    private int profileMaxChars = 1000;

    private int memoryMaxChars = 1500;

    private int extractionMaxMemories = 6;

    private int extractionMaxAnswerChars = 1600;
}
