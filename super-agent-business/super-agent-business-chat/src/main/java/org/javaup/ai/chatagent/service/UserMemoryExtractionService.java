package org.javaup.ai.chatagent.service;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.javaup.ai.chatagent.config.ChatUserMemoryProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class UserMemoryExtractionService {

    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("\\{[\\s\\S]*}");

    private static final String SYSTEM_PROMPT = """
        你是用户长期记忆抽取器，只抽取对后续跨会话帮助明显的信息。
        不要抽取寒暄、一次性临时信息、未经确认的猜测、密钥、手机号、身份证、账号密码等敏感信息。
        只返回 JSON，不要输出解释。
        """;

    private final ChatUserMemoryProperties properties;
    private final ObservedChatModelService observedChatModelService;
    private final UserMemoryService userMemoryService;
    private final UserProfileService userProfileService;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;

    public UserMemoryExtractionService(ChatUserMemoryProperties properties,
                                       ObservedChatModelService observedChatModelService,
                                       UserMemoryService userMemoryService,
                                       UserProfileService userProfileService,
                                       ObjectMapper objectMapper,
                                       @Qualifier("chatMemorySummaryExecutorService") ExecutorService executorService) {
        this.properties = properties;
        this.observedChatModelService = observedChatModelService;
        this.userMemoryService = userMemoryService;
        this.userProfileService = userProfileService;
        this.objectMapper = objectMapper;
        this.executorService = executorService;
    }

    public void extractAfterCompletion(TaskInfo taskInfo, String answer) {
        if (!properties.isEnabled() || !properties.isExtractionEnabled() || taskInfo == null || StrUtil.isBlank(taskInfo.userId())) {
            return;
        }
        executorService.execute(() -> safeExtract(taskInfo, answer));
    }

    private void safeExtract(TaskInfo taskInfo, String answer) {
        try {
            String raw = observedChatModelService.callText(
                "user_memory_extract",
                SYSTEM_PROMPT,
                buildPrompt(taskInfo, answer),
                null
            );
            int savedCount = parseAndSave(taskInfo, raw);
            if (savedCount > 0) {
                userProfileService.refreshProfileSummaryFromMemories(taskInfo.tenantId(), taskInfo.userId());
            }
            log.info("用户长期记忆抽取完成, tenantId={}, userId={}, conversationId={}, exchangeId={}, savedCount={}",
                taskInfo.tenantId(), taskInfo.userId(), taskInfo.conversationId(), taskInfo.exchangeId(), savedCount);
        }
        catch (RuntimeException exception) {
            log.warn("用户长期记忆抽取失败, tenantId={}, userId={}, conversationId={}, exchangeId={}: {}",
                taskInfo.tenantId(), taskInfo.userId(), taskInfo.conversationId(), taskInfo.exchangeId(), exception.getMessage());
        }
    }

    private String buildPrompt(TaskInfo taskInfo, String answer) {
        return """
            请从下面这一轮问答中抽取用户长期记忆，输出 JSON：
            {
              "memories": [
                {
                  "type": "PROFILE|PREFERENCE|TASK|FACT|EPISODIC",
                  "key": "可选的稳定键名",
                  "text": "可直接注入 Prompt 的中文记忆，不超过80字",
                  "importance": 0,
                  "confidence": 0.0
                }
              ]
            }

            要求：
            1. 最多抽取 %s 条。
            2. type 只能使用 PROFILE、PREFERENCE、TASK、FACT、EPISODIC。
            3. importance 取 0~100；confidence 取 0~1。
            4. 如果没有值得长期保存的信息，返回 {"memories":[]}。

            用户问题：
            %s

            助手回答：
            %s
            """.formatted(
            Math.max(1, properties.getExtractionMaxMemories()),
            StrUtil.blankToDefault(taskInfo.question(), ""),
            clip(answer, Math.max(1, properties.getExtractionMaxAnswerChars()))
        );
    }

    private int parseAndSave(TaskInfo taskInfo, String raw) {
        if (StrUtil.isBlank(raw)) {
            return 0;
        }
        try {
            JsonNode root = objectMapper.readTree(extractJsonObject(raw));
            JsonNode memories = root.path("memories");
            if (!memories.isArray()) {
                return 0;
            }
            int savedCount = 0;
            int limit = Math.max(1, properties.getExtractionMaxMemories());
            for (JsonNode node : memories) {
                if (savedCount >= limit) {
                    break;
                }
                String type = normalizeType(node.path("type").asText(""));
                String text = node.path("text").asText("");
                if (StrUtil.isBlank(type) || StrUtil.isBlank(text)) {
                    continue;
                }
                userMemoryService.saveMemory(
                    taskInfo.tenantId(),
                    taskInfo.userId(),
                    type,
                    node.path("key").asText(""),
                    text,
                    objectMapper.writeValueAsString(node),
                    taskInfo.conversationId(),
                    taskInfo.exchangeId(),
                    clamp(node.path("importance").asInt(50), 0, 100),
                    BigDecimal.valueOf(clamp(node.path("confidence").asDouble(0.7D), 0D, 1D))
                );
                savedCount++;
            }
            return savedCount;
        }
        catch (Exception exception) {
            log.warn("解析用户长期记忆 JSON 失败: {}", exception.getMessage());
            return 0;
        }
    }

    private String normalizeType(String value) {
        if (StrUtil.isBlank(value)) {
            return "";
        }
        String type = value.trim().toUpperCase();
        return switch (type) {
            case "PROFILE", "PREFERENCE", "TASK", "FACT", "EPISODIC" -> type;
            default -> "";
        };
    }

    private String extractJsonObject(String raw) {
        Matcher matcher = JSON_OBJECT_PATTERN.matcher(raw.trim());
        if (matcher.find()) {
            return matcher.group();
        }
        return raw.trim();
    }

    private String clip(String value, int maxChars) {
        if (StrUtil.isBlank(value)) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxChars ? trimmed : trimmed.substring(0, maxChars);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
