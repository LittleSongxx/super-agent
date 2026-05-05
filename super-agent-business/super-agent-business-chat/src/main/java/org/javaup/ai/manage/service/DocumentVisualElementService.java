package org.javaup.ai.manage.service;

import org.javaup.ai.manage.data.SuperAgentDocumentVisualElement;
import org.javaup.ai.manage.support.DocumentVisualElementCandidate;

import java.util.List;

public interface DocumentVisualElementService {

    List<SuperAgentDocumentVisualElement> replaceVisualElements(Long documentId,
                                                                Long parseTaskId,
                                                                List<DocumentVisualElementCandidate> candidates);
}
