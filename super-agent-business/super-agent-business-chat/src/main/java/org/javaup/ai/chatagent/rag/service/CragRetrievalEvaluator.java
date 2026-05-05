package org.javaup.ai.chatagent.rag.service;

import cn.hutool.core.util.StrUtil;
import org.javaup.ai.chatagent.config.ChatCragProperties;
import org.javaup.ai.chatagent.rag.model.ConversationExecutionPlan;
import org.javaup.ai.chatagent.rag.model.CragEvaluationResult;
import org.javaup.ai.chatagent.rag.model.RagRetrievalContext;
import org.javaup.ai.chatagent.rag.model.SubQuestionEvidence;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CragRetrievalEvaluator {

    private final ChatCragProperties properties;

    public CragRetrievalEvaluator(ChatCragProperties properties) {
        this.properties = properties;
    }

    public CragEvaluationResult evaluate(ConversationExecutionPlan plan, RagRetrievalContext context) {
        if (!properties.isEnabled()) {
            return CragEvaluationResult.builder().enabled(false).passed(true).build();
        }
        int referenceCount = context == null ? 0 : context.flattenReferences().size();
        int coveredSubQuestionCount = coveredSubQuestionCount(context);
        List<String> weakReasons = new ArrayList<>();
        if (referenceCount < Math.max(1, properties.getMinReferenceCount())) {
            weakReasons.add("有效证据数量不足");
        }
        if (coveredSubQuestionCount < Math.max(1, properties.getMinCoveredSubQuestionCount())) {
            weakReasons.add("覆盖到的子问题数量不足");
        }
        boolean passed = weakReasons.isEmpty();
        String baseQuestion = plan == null ? "" : StrUtil.blankToDefault(plan.getRetrievalQuestion(), plan.getOriginalQuestion());
        return CragEvaluationResult.builder()
            .enabled(true)
            .passed(passed)
            .correctionRequired(!passed)
            .referenceCount(referenceCount)
            .coveredSubQuestionCount(coveredSubQuestionCount)
            .weakReasons(weakReasons)
            .correctiveQuery(StrUtil.blankToDefault(baseQuestion, "") + " " + StrUtil.blankToDefault(properties.getCorrectiveQuerySuffix(), ""))
            .build();
    }

    private int coveredSubQuestionCount(RagRetrievalContext context) {
        if (context == null || context.getSubQuestionEvidenceList() == null) {
            return 0;
        }
        int count = 0;
        for (SubQuestionEvidence evidence : context.getSubQuestionEvidenceList()) {
            if (evidence != null && evidence.getReferences() != null && !evidence.getReferences().isEmpty()) {
                count++;
            }
        }
        return count;
    }
}
