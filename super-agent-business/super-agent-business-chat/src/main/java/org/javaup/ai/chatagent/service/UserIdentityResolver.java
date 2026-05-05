package org.javaup.ai.chatagent.service;

import cn.hutool.core.util.StrUtil;
import org.javaup.ai.chatagent.config.ChatUserMemoryProperties;
import org.javaup.ai.chatagent.dto.ChatRequestDto;
import org.javaup.ai.chatagent.model.memory.UserIdentity;
import org.springframework.stereotype.Service;

@Service
public class UserIdentityResolver {

    private final ChatUserMemoryProperties properties;

    public UserIdentityResolver(ChatUserMemoryProperties properties) {
        this.properties = properties;
    }

    public UserIdentity resolve(ChatRequestDto request) {
        String tenantId = normalize(request == null ? null : request.getTenantId(), properties.getDefaultTenantId());
        String userId = normalize(request == null ? null : request.getUserId(), properties.getDefaultUserId());
        return new UserIdentity(tenantId, userId);
    }

    private String normalize(String value, String fallback) {
        if (StrUtil.isNotBlank(value)) {
            return value.trim();
        }
        if (StrUtil.isNotBlank(fallback)) {
            return fallback.trim();
        }
        return "default";
    }
}
