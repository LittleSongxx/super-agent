package org.javaup.ai.chatagent.service;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.javaup.ai.chatagent.config.ChatUserMemoryProperties;
import org.javaup.ai.chatagent.model.memory.UserMemoryContext;
import org.javaup.ai.chatagent.model.memory.UserMemoryItem;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class UserMemoryRecallService {

    private final ChatUserMemoryProperties properties;
    private final UserProfileService userProfileService;
    private final UserMemoryService userMemoryService;
    private final UserMemoryVectorGateway vectorGateway;

    public UserMemoryRecallService(ChatUserMemoryProperties properties,
                                   UserProfileService userProfileService,
                                   UserMemoryService userMemoryService,
                                   UserMemoryVectorGateway vectorGateway) {
        this.properties = properties;
        this.userProfileService = userProfileService;
        this.userMemoryService = userMemoryService;
        this.vectorGateway = vectorGateway;
    }

    public UserMemoryContext recall(String tenantId, String userId, String question) {
        if (!properties.isEnabled() || StrUtil.isBlank(tenantId) || StrUtil.isBlank(userId)) {
            return UserMemoryContext.builder().tenantId(tenantId).userId(userId).build();
        }
        try {
            String profileSummary = userProfileService.loadProfileSummary(tenantId, userId);
            List<UserMemoryItem> priorityMemories = userMemoryService.listHighPriorityMemories(tenantId, userId, Math.max(1, properties.getRecallTopK()));
            List<UserMemoryItem> vectorMemories = properties.isVectorRecallEnabled()
                ? vectorGateway.recall(tenantId, userId, question, Math.max(1, properties.getRecallTopK()))
                : List.of();
            List<UserMemoryItem> merged = merge(priorityMemories, vectorMemories);
            List<Long> sourceIds = merged.stream().map(UserMemoryItem::getMemoryId).filter(id -> id != null).toList();
            if (!sourceIds.isEmpty()) {
                userMemoryService.markAccessed(sourceIds);
            }
            return UserMemoryContext.builder()
                .tenantId(tenantId)
                .userId(userId)
                .profileSummary(profileSummary)
                .retrievedMemories(merged)
                .preferences(merged.stream()
                    .filter(item -> "PREFERENCE".equalsIgnoreCase(item.getMemoryType()))
                    .map(UserMemoryItem::getMemoryText)
                    .filter(StrUtil::isNotBlank)
                    .toList())
                .activeTasks(merged.stream()
                    .filter(item -> "TASK".equalsIgnoreCase(item.getMemoryType()))
                    .map(UserMemoryItem::getMemoryText)
                    .filter(StrUtil::isNotBlank)
                    .toList())
                .memoryPromptText(buildPromptText(profileSummary, merged))
                .sourceMemoryIds(sourceIds)
                .build();
        }
        catch (RuntimeException exception) {
            log.warn("用户记忆召回失败, tenantId={}, userId={}: {}", tenantId, userId, exception.getMessage());
            return UserMemoryContext.builder().tenantId(tenantId).userId(userId).build();
        }
    }

    private List<UserMemoryItem> merge(List<UserMemoryItem> priorityMemories, List<UserMemoryItem> vectorMemories) {
        Map<Long, UserMemoryItem> merged = new LinkedHashMap<>();
        append(merged, priorityMemories);
        append(merged, vectorMemories);
        return new ArrayList<>(merged.values()).stream()
            .limit(Math.max(1, properties.getRecallTopK() * 2L))
            .toList();
    }

    private void append(Map<Long, UserMemoryItem> merged, List<UserMemoryItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (UserMemoryItem item : items) {
            if (item == null || item.getMemoryId() == null || StrUtil.isBlank(item.getMemoryText())) {
                continue;
            }
            merged.putIfAbsent(item.getMemoryId(), item);
        }
    }

    private String buildPromptText(String profileSummary, List<UserMemoryItem> memories) {
        StringBuilder builder = new StringBuilder();
        if (StrUtil.isNotBlank(profileSummary)) {
            builder.append("用户画像摘要：\n")
                .append(clip(profileSummary, Math.max(1, properties.getProfileMaxChars())))
                .append("\n");
        }
        if (memories != null && !memories.isEmpty()) {
            builder.append("跨会话相关记忆：\n");
            for (UserMemoryItem memory : memories) {
                if (memory == null || StrUtil.isBlank(memory.getMemoryText())) {
                    continue;
                }
                builder.append("- ")
                    .append(memory.getMemoryText().trim())
                    .append("\n");
            }
        }
        return clip(builder.toString().trim(), Math.max(1, properties.getMemoryMaxChars()));
    }

    private String clip(String value, int maxChars) {
        if (StrUtil.isBlank(value)) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxChars ? trimmed : trimmed.substring(0, maxChars);
    }
}
