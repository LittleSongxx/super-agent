package org.javaup.ai.manage.service;

import org.javaup.ai.manage.data.SuperAgentDocument;
import org.javaup.ai.manage.support.DocumentAnalysisResult;
import org.javaup.ai.manage.support.DocumentVisualAnalysisResult;

public interface DocumentVisualAnalysisService {

    DocumentVisualAnalysisResult analyze(byte[] bytes,
                                         SuperAgentDocument document,
                                         DocumentAnalysisResult analysisResult);
}
