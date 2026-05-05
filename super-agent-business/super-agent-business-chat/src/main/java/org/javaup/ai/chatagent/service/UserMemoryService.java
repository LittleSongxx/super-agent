package org.javaup.ai.chatagent.service;

import cn.hutool.core.util.StrUtil;
import com.baidu.fsg.uid.UidGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.javaup.ai.chatagent.data.SuperAgentUserMemory;
import org.javaup.ai.chatagent.mapper.SuperAgentUserMemoryMapper;
import org.javaup.ai.chatagent.model.memory.UserMemoryItem;
import org.javaup.enums.BusinessStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class UserMemoryService {

    private final SuperAgentUserMemoryMapper memoryMapper;
    private final UserMemoryVectorGateway vectorGateway;

    @Resource
    private UidGenerator uidGenerator;

    public UserMemoryService(SuperAgentUserMemoryMapper memoryMapper,
                             UserMemoryVectorGateway vectorGateway) {
        this.memoryMapper = memoryMapper;
        this.vectorGateway = vectorGateway;
    }

    public SuperAgentUserMemory saveMemory(String tenantId,
                                           String userId,
                                           String memoryType,
                                           String memoryKey,
                                           String memoryText,
                                           String memoryJson,
                                           String sourceConversationId,
                                           Long sourceExchangeId,
                                           Integer importance,
                                           BigDecimal confidence) {
        if (StrUtil.isBlank(tenantId) || StrUtil.isBlank(userId) || StrUtil.isBlank(memoryType) || StrUtil.isBlank(memoryText)) {
            return null;
        }
        SuperAgentUserMemory existing = findExisting(tenantId, userId, memoryType, memoryKey, memoryText);
        if (existing == null) {
            SuperAgentUserMemory memory = new SuperAgentUserMemory();
            memory.setId(uidGenerator.getUid());
            memory.setTenantId(tenantId);
            memory.setUserId(userId);
            memory.setMemoryType(memoryType.trim().toUpperCase());
            memory.setMemoryKey(StrUtil.blankToDefault(memoryKey, "").trim());
            memory.setMemoryText(memoryText.trim());
            memory.setMemoryJson(StrUtil.blankToDefault(memoryJson, "{}"));
            memory.setSourceConversationId(sourceConversationId);
            memory.setSourceExchangeId(sourceExchangeId);
            memory.setImportance(importance == null ? 50 : importance);
            memory.setConfidence(confidence == null ? BigDecimal.valueOf(0.7D) : confidence);
            memory.setVisibility("PRIVATE");
            memory.setEffectiveFrom(new Date());
            memory.setAccessCount(0);
            memory.setMemoryStatus(1);
            memory.setStatus(BusinessStatus.YES.getCode());
            memoryMapper.insert(memory);
            safeUpsertVector(memory);
            return memory;
        }
        SuperAgentUserMemory update = new SuperAgentUserMemory();
        update.setId(existing.getId());
        update.setMemoryText(memoryText.trim());
        update.setMemoryJson(StrUtil.blankToDefault(memoryJson, "{}"));
        update.setSourceConversationId(sourceConversationId);
        update.setSourceExchangeId(sourceExchangeId);
        update.setImportance(Math.max(existing.getImportance() == null ? 0 : existing.getImportance(), importance == null ? 50 : importance));
        update.setConfidence(confidence == null ? existing.getConfidence() : confidence.max(existing.getConfidence() == null ? BigDecimal.ZERO : existing.getConfidence()));
        update.setMemoryStatus(1);
        memoryMapper.updateById(update);
        existing.setMemoryText(update.getMemoryText());
        existing.setMemoryJson(update.getMemoryJson());
        existing.setSourceConversationId(update.getSourceConversationId());
        existing.setSourceExchangeId(update.getSourceExchangeId());
        existing.setImportance(update.getImportance());
        existing.setConfidence(update.getConfidence());
        existing.setMemoryStatus(update.getMemoryStatus());
        safeUpsertVector(existing);
        return existing;
    }

    public List<UserMemoryItem> listHighPriorityMemories(String tenantId, String userId, int limit) {
        if (StrUtil.isBlank(tenantId) || StrUtil.isBlank(userId) || limit <= 0) {
            return List.of();
        }
        List<SuperAgentUserMemory> memories = memoryMapper.selectList(new LambdaQueryWrapper<SuperAgentUserMemory>()
            .eq(SuperAgentUserMemory::getTenantId, tenantId)
            .eq(SuperAgentUserMemory::getUserId, userId)
            .eq(SuperAgentUserMemory::getMemoryStatus, 1)
            .eq(SuperAgentUserMemory::getStatus, BusinessStatus.YES.getCode())
            .in(SuperAgentUserMemory::getMemoryType, List.of("PROFILE", "PREFERENCE", "TASK"))
            .orderByDesc(SuperAgentUserMemory::getImportance)
            .orderByDesc(SuperAgentUserMemory::getId)
            .last("LIMIT " + Math.max(1, limit)));
        if (memories == null || memories.isEmpty()) {
            return List.of();
        }
        return memories.stream().map(this::toItem).toList();
    }

    public void markAccessed(List<Long> memoryIds) {
        if (memoryIds == null || memoryIds.isEmpty()) {
            return;
        }
        for (Long memoryId : memoryIds) {
            if (memoryId == null) {
                continue;
            }
            SuperAgentUserMemory memory = memoryMapper.selectById(memoryId);
            if (memory == null) {
                continue;
            }
            SuperAgentUserMemory update = new SuperAgentUserMemory();
            update.setId(memoryId);
            update.setLastAccessTime(new Date());
            update.setAccessCount((memory.getAccessCount() == null ? 0 : memory.getAccessCount()) + 1);
            memoryMapper.updateById(update);
        }
    }

    private SuperAgentUserMemory findExisting(String tenantId, String userId, String memoryType, String memoryKey, String memoryText) {
        LambdaQueryWrapper<SuperAgentUserMemory> wrapper = new LambdaQueryWrapper<SuperAgentUserMemory>()
            .eq(SuperAgentUserMemory::getTenantId, tenantId)
            .eq(SuperAgentUserMemory::getUserId, userId)
            .eq(SuperAgentUserMemory::getMemoryType, memoryType.trim().toUpperCase())
            .eq(SuperAgentUserMemory::getMemoryStatus, 1)
            .eq(SuperAgentUserMemory::getStatus, BusinessStatus.YES.getCode())
            .orderByDesc(SuperAgentUserMemory::getId)
            .last("LIMIT 1");
        if (StrUtil.isNotBlank(memoryKey)) {
            wrapper.eq(SuperAgentUserMemory::getMemoryKey, memoryKey.trim());
        }
        else {
            wrapper.eq(SuperAgentUserMemory::getMemoryText, memoryText.trim());
        }
        return memoryMapper.selectOne(wrapper);
    }

    private UserMemoryItem toItem(SuperAgentUserMemory memory) {
        return UserMemoryItem.builder()
            .memoryId(memory.getId())
            .memoryType(memory.getMemoryType())
            .memoryKey(memory.getMemoryKey())
            .memoryText(memory.getMemoryText())
            .sourceConversationId(memory.getSourceConversationId())
            .sourceExchangeId(memory.getSourceExchangeId())
            .importance(memory.getImportance())
            .confidence(memory.getConfidence())
            .build();
    }

    private void safeUpsertVector(SuperAgentUserMemory memory) {
        try {
            vectorGateway.upsertMemory(memory);
        }
        catch (RuntimeException exception) {
            log.warn("用户记忆向量写入失败, memoryId={}: {}", memory == null ? null : memory.getId(), exception.getMessage());
        }
    }
}
