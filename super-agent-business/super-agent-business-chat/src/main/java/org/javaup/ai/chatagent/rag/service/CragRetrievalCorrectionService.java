package org.javaup.ai.chatagent.rag.service;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.javaup.ai.chatagent.config.ChatCragProperties;
import org.javaup.ai.chatagent.rag.model.ConversationExecutionPlan;
import org.javaup.ai.chatagent.rag.model.CragEvaluationResult;
import org.javaup.ai.chatagent.rag.model.RagRetrievalContext;
import org.javaup.ai.chatagent.service.ConversationTraceRecorder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class CragRetrievalCorrectionService {

    private final ChatCragProperties properties;
    private final CragRetrievalEvaluator evaluator;
    private final RagRetrievalEngine retrievalEngine;

    public CragRetrievalCorrectionService(ChatCragProperties properties,
                                          CragRetrievalEvaluator evaluator,
                                          RagRetrievalEngine retrievalEngine) {
        this.properties = properties;
        this.evaluator = evaluator;
        this.retrievalEngine = retrievalEngine;
    }

    public RagRetrievalContext evaluateAndCorrect(ConversationExecutionPlan plan,
                                                  RagRetrievalContext context,
                                                  ConversationTraceRecorder traceRecorder) {
        CragEvaluationResult evaluation = evaluator.evaluate(plan, context);
        if (!evaluation.isEnabled()) {
            return context;
        }
        appendCragNote(context, evaluation);
        if (evaluation.isPassed() || !properties.isCorrectiveRetrievalEnabled() || properties.getMaxCorrectiveRounds() <= 0) {
            return context;
        }
        try {
            ConversationExecutionPlan correctivePlan = buildCorrectivePlan(plan, evaluation.getCorrectiveQuery());
            RagRetrievalContext correctedContext = retrievalEngine.retrieve(correctivePlan, traceRecorder);
            if (correctedContext == null || correctedContext.isEmpty()) {
                appendNote(context, "CRAG 纠偏检索未找到更强证据，保留原始检索结果。");
                return context;
            }
            List<String> mergedNotes = new ArrayList<>();
            if (context != null && context.getRetrievalNotes() != null) {
                mergedNotes.addAll(context.getRetrievalNotes());
            }
            mergedNotes.add("CRAG 已触发纠偏检索，并采用纠偏后的证据集合。");
            if (correctedContext.getRetrievalNotes() != null) {
                mergedNotes.addAll(correctedContext.getRetrievalNotes());
            }
            correctedContext.setRetrievalNotes(mergedNotes);
            return correctedContext;
        }
        catch (RuntimeException exception) {
            log.warn("CRAG 纠偏检索失败，保留原始检索结果: {}", exception.getMessage());
            appendNote(context, "CRAG 纠偏检索失败，已保留原始检索结果。");
            return context;
        }
    }

    private ConversationExecutionPlan buildCorrectivePlan(ConversationExecutionPlan plan, String correctiveQuery) {
        String query = StrUtil.blankToDefault(correctiveQuery, plan == null ? "" : plan.getRetrievalQuestion());
        return ConversationExecutionPlan.builder()
            .mode(plan.getMode())
            .chatMode(plan.getChatMode())
            .tenantId(plan.getTenantId())
            .userId(plan.getUserId())
            .originalQuestion(plan.getOriginalQuestion())
            .agentQuestion(plan.getAgentQuestion())
            .rewriteQuestion(plan.getRewriteQuestion())
            .rewriteSubQuestions(plan.getRewriteSubQuestions())
            .retrievalQuestion(query)
            .retrievalSubQuestions(List.of(query))
            .historySummary(plan.getHistorySummary())
            .longTermSummary(plan.getLongTermSummary())
            .historyPlanningContext(plan.getHistoryPlanningContext())
            .recentHistoryTranscript(plan.getRecentHistoryTranscript())
            .answerRecentTranscript(plan.getAnswerRecentTranscript())
            .answerHistoryContext(plan.getAnswerHistoryContext())
            .userMemoryContext(plan.getUserMemoryContext())
            .navigationDecision(plan.getNavigationDecision())
            .historyCompressionApplied(plan.isHistoryCompressionApplied())
            .historyCoveredExchangeId(plan.getHistoryCoveredExchangeId())
            .historyCoveredExchangeCount(plan.getHistoryCoveredExchangeCount())
            .historyCompressionCount(plan.getHistoryCompressionCount())
            .currentDate(plan.getCurrentDate())
            .currentDateText(plan.getCurrentDateText())
            .requiresFreshSearch(plan.isRequiresFreshSearch())
            .requiresCurrentDateAnchoring(plan.isRequiresCurrentDateAnchoring())
            .selectedDocumentId(plan.getSelectedDocumentId())
            .selectedDocumentName(plan.getSelectedDocumentName())
            .selectedTaskId(plan.getSelectedTaskId())
            .retrievalDocumentIds(plan.getRetrievalDocumentIds())
            .retrievalTaskIds(plan.getRetrievalTaskIds())
            .clarificationReply(plan.getClarificationReply())
            .clarificationOptions(plan.getClarificationOptions())
            .clarificationReason(plan.getClarificationReason())
            .noEvidenceReply(plan.getNoEvidenceReply())
            .build();
    }

    private void appendCragNote(RagRetrievalContext context, CragEvaluationResult evaluation) {
        if (evaluation.isPassed()) {
            appendNote(context, "CRAG 证据评估通过：referenceCount=" + evaluation.getReferenceCount() + "，coveredSubQuestionCount=" + evaluation.getCoveredSubQuestionCount());
            return;
        }
        appendNote(context, "CRAG 证据评估偏弱：" + String.join("；", evaluation.getWeakReasons()));
    }

    private void appendNote(RagRetrievalContext context, String note) {
        if (context == null || StrUtil.isBlank(note)) {
            return;
        }
        if (context.getRetrievalNotes() == null) {
            context.setRetrievalNotes(new ArrayList<>());
        }
        context.getRetrievalNotes().add(note);
    }
}
