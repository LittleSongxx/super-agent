package org.javaup.ai.manage.service.impl;

import org.javaup.ai.manage.data.SuperAgentDocument;
import org.javaup.ai.manage.service.DocumentVisualAnalysisService;
import org.javaup.ai.manage.support.DocumentAnalysisResult;
import org.javaup.ai.manage.support.DocumentVisualAnalysisResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(DocumentVisualAnalysisService.class)
public class NoopDocumentVisualAnalysisService implements DocumentVisualAnalysisService {

    @Override
    public DocumentVisualAnalysisResult analyze(byte[] bytes,
                                                SuperAgentDocument document,
                                                DocumentAnalysisResult analysisResult) {
        return DocumentVisualAnalysisResult.builder().build();
    }
}
