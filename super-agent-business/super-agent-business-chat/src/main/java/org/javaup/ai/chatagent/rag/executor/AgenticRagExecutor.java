package org.javaup.ai.chatagent.rag.executor;

import cn.hutool.core.util.StrUtil;
import org.javaup.ai.chatagent.rag.model.AgenticRagPlan;
import org.javaup.ai.chatagent.rag.model.ConversationExecutionPlan;
import org.javaup.ai.chatagent.rag.model.ExecutionMode;
import org.javaup.ai.chatagent.rag.service.AgenticRagPlanner;
import org.javaup.ai.chatagent.rag.support.ExecutorEventSupport;
import org.javaup.ai.chatagent.service.TaskInfo;
import org.javaup.ai.chatagent.support.StreamEventWriter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class AgenticRagExecutor implements ConversationExecutor {

    private final AgenticRagPlanner agenticRagPlanner;
    private final RagChatExecutor ragChatExecutor;
    private final StreamEventWriter streamEventWriter;

    public AgenticRagExecutor(AgenticRagPlanner agenticRagPlanner,
                              RagChatExecutor ragChatExecutor,
                              StreamEventWriter streamEventWriter) {
        this.agenticRagPlanner = agenticRagPlanner;
        this.ragChatExecutor = ragChatExecutor;
        this.streamEventWriter = streamEventWriter;
    }

    @Override
    public ExecutionMode mode() {
        return ExecutionMode.AGENTIC_RAG;
    }

    @Override
    public Flux<String> execute(TaskInfo taskInfo) {
        ConversationExecutionPlan executionPlan = taskInfo.executionPlan();
        AgenticRagPlan agenticPlan = agenticRagPlanner.plan(executionPlan);
        if (agenticPlan.isEnabled() && StrUtil.isNotBlank(agenticPlan.getSummary())) {
            ExecutorEventSupport.publishThinking(taskInfo, streamEventWriter, "Agentic RAG 计划：" + agenticPlan.getSummary());
            if (taskInfo.debugTrace() != null && taskInfo.debugTrace().getRetrievalNotes() != null) {
                taskInfo.debugTrace().getRetrievalNotes().add("Agentic RAG 计划：" + agenticPlan.getSummary());
            }
        }
        return ragChatExecutor.execute(taskInfo);
    }
}
