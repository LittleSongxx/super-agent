package org.javaup.ai.chatagent.rag.service;

import cn.hutool.core.util.StrUtil;
import org.javaup.ai.chatagent.config.ChatAgenticRagProperties;
import org.javaup.ai.chatagent.rag.model.AgenticRagPlan;
import org.javaup.ai.chatagent.rag.model.ConversationExecutionPlan;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AgenticRagPlanner {

    private final ChatAgenticRagProperties properties;

    public AgenticRagPlanner(ChatAgenticRagProperties properties) {
        this.properties = properties;
    }

    public AgenticRagPlan plan(ConversationExecutionPlan executionPlan) {
        if (!properties.isEnabled()) {
            return AgenticRagPlan.builder().enabled(false).plannerMode("disabled").build();
        }
        List<String> steps = new ArrayList<>();
        if (executionPlan != null && StrUtil.isNotBlank(executionPlan.getRewriteQuestion())
            && !executionPlan.getRewriteQuestion().equals(executionPlan.getOriginalQuestion())) {
            steps.add("基于会话历史确认独立问题表达");
        }
        steps.add("围绕检索问题执行知识召回");
        if (properties.isIncludeEvidenceEvaluationStep()) {
            steps.add("评估证据覆盖度并在必要时触发纠偏");
        }
        steps.add("基于证据和用户长期记忆生成回答");
        int maxSteps = Math.max(1, properties.getMaxSteps());
        if (steps.size() > maxSteps) {
            steps = steps.subList(0, maxSteps);
        }
        return AgenticRagPlan.builder()
            .enabled(true)
            .plannerMode("rule")
            .steps(steps)
            .summary(String.join(" -> ", steps))
            .build();
    }
}
