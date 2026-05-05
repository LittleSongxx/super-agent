package org.javaup.ai.chatagent.service;

import cn.hutool.core.util.StrUtil;
import com.baidu.fsg.uid.UidGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.javaup.ai.chatagent.config.ChatUserMemoryProperties;
import org.javaup.ai.chatagent.data.SuperAgentUserMemory;
import org.javaup.ai.chatagent.data.SuperAgentUserProfile;
import org.javaup.ai.chatagent.mapper.SuperAgentUserMemoryMapper;
import org.javaup.ai.chatagent.mapper.SuperAgentUserProfileMapper;
import org.javaup.enums.BusinessStatus;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class UserProfileService {

    private final SuperAgentUserProfileMapper profileMapper;
    private final SuperAgentUserMemoryMapper memoryMapper;
    private final ChatUserMemoryProperties properties;

    @Resource
    private UidGenerator uidGenerator;

    public UserProfileService(SuperAgentUserProfileMapper profileMapper,
                              SuperAgentUserMemoryMapper memoryMapper,
                              ChatUserMemoryProperties properties) {
        this.profileMapper = profileMapper;
        this.memoryMapper = memoryMapper;
        this.properties = properties;
    }

    public String loadProfileSummary(String tenantId, String userId) {
        if (!properties.isEnabled() || StrUtil.isBlank(tenantId) || StrUtil.isBlank(userId)) {
            return "";
        }
        SuperAgentUserProfile profile = findProfile(tenantId, userId);
        return profile == null ? "" : clip(profile.getProfileSummary(), Math.max(1, properties.getProfileMaxChars()));
    }

    public void refreshProfileSummaryFromMemories(String tenantId, String userId) {
        if (!properties.isEnabled() || StrUtil.isBlank(tenantId) || StrUtil.isBlank(userId)) {
            return;
        }
        List<SuperAgentUserMemory> memories = memoryMapper.selectList(new LambdaQueryWrapper<SuperAgentUserMemory>()
            .eq(SuperAgentUserMemory::getTenantId, tenantId)
            .eq(SuperAgentUserMemory::getUserId, userId)
            .eq(SuperAgentUserMemory::getMemoryStatus, 1)
            .eq(SuperAgentUserMemory::getStatus, BusinessStatus.YES.getCode())
            .in(SuperAgentUserMemory::getMemoryType, List.of("PROFILE", "PREFERENCE", "TASK"))
            .orderByDesc(SuperAgentUserMemory::getImportance)
            .orderByDesc(SuperAgentUserMemory::getId)
            .last("LIMIT 12"));
        if (memories == null || memories.isEmpty()) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (SuperAgentUserMemory memory : memories) {
            if (memory == null || StrUtil.isBlank(memory.getMemoryText())) {
                continue;
            }
            builder.append("- ")
                .append(memory.getMemoryText().trim())
                .append("\n");
        }
        String summary = clip(builder.toString().trim(), Math.max(1, properties.getProfileMaxChars()));
        if (StrUtil.isBlank(summary)) {
            return;
        }
        SuperAgentUserProfile existing = findProfile(tenantId, userId);
        if (existing == null) {
            SuperAgentUserProfile profile = new SuperAgentUserProfile();
            profile.setId(uidGenerator.getUid());
            profile.setTenantId(tenantId);
            profile.setUserId(userId);
            profile.setProfileVersion(1);
            profile.setProfileSummary(summary);
            profile.setProfileStatus(1);
            profile.setLastMemoryUpdateTime(new Date());
            profile.setStatus(BusinessStatus.YES.getCode());
            profileMapper.insert(profile);
            return;
        }
        SuperAgentUserProfile update = new SuperAgentUserProfile();
        update.setId(existing.getId());
        update.setProfileVersion(existing.getProfileVersion() == null ? 1 : existing.getProfileVersion() + 1);
        update.setProfileSummary(summary);
        update.setProfileStatus(1);
        update.setLastMemoryUpdateTime(new Date());
        profileMapper.updateById(update);
    }

    private SuperAgentUserProfile findProfile(String tenantId, String userId) {
        return profileMapper.selectOne(new LambdaQueryWrapper<SuperAgentUserProfile>()
            .eq(SuperAgentUserProfile::getTenantId, tenantId)
            .eq(SuperAgentUserProfile::getUserId, userId)
            .eq(SuperAgentUserProfile::getStatus, BusinessStatus.YES.getCode())
            .orderByDesc(SuperAgentUserProfile::getId)
            .last("LIMIT 1"));
    }

    private String clip(String value, int maxChars) {
        if (StrUtil.isBlank(value)) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxChars ? trimmed : trimmed.substring(0, maxChars);
    }
}
