package org.javaup.ai.chatagent.model.memory;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMemoryContext {

    private String tenantId;

    private String userId;

    private String profileSummary;

    @Builder.Default
    private List<UserMemoryItem> retrievedMemories = new ArrayList<>();

    @Builder.Default
    private List<String> preferences = new ArrayList<>();

    @Builder.Default
    private List<String> activeTasks = new ArrayList<>();

    private String memoryPromptText;

    @Builder.Default
    private List<Long> sourceMemoryIds = new ArrayList<>();

    public boolean isEmpty() {
        return StrUtil.isBlank(profileSummary)
            && StrUtil.isBlank(memoryPromptText)
            && (retrievedMemories == null || retrievedMemories.isEmpty())
            && (preferences == null || preferences.isEmpty())
            && (activeTasks == null || activeTasks.isEmpty());
    }
}
