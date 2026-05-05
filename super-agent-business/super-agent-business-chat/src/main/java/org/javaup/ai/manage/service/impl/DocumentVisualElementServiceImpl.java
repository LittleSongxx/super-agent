package org.javaup.ai.manage.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baidu.fsg.uid.UidGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.javaup.ai.manage.data.SuperAgentDocumentVisualElement;
import org.javaup.ai.manage.mapper.SuperAgentDocumentVisualElementMapper;
import org.javaup.ai.manage.service.DocumentVisualElementService;
import org.javaup.ai.manage.support.DocumentVisualElementCandidate;
import org.javaup.enums.BusinessStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentVisualElementServiceImpl implements DocumentVisualElementService {

    private final SuperAgentDocumentVisualElementMapper visualElementMapper;

    @Resource
    private UidGenerator uidGenerator;

    public DocumentVisualElementServiceImpl(SuperAgentDocumentVisualElementMapper visualElementMapper) {
        this.visualElementMapper = visualElementMapper;
    }

    @Override
    public List<SuperAgentDocumentVisualElement> replaceVisualElements(Long documentId,
                                                                       Long parseTaskId,
                                                                       List<DocumentVisualElementCandidate> candidates) {
        if (documentId == null || parseTaskId == null) {
            return List.of();
        }
        visualElementMapper.delete(new LambdaQueryWrapper<SuperAgentDocumentVisualElement>()
            .eq(SuperAgentDocumentVisualElement::getDocumentId, documentId)
            .eq(SuperAgentDocumentVisualElement::getParseTaskId, parseTaskId));
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        List<SuperAgentDocumentVisualElement> savedElements = new ArrayList<>();
        int elementNo = 1;
        for (DocumentVisualElementCandidate candidate : candidates) {
            if (candidate == null || (StrUtil.isBlank(candidate.getOcrText()) && StrUtil.isBlank(candidate.getCaptionText()))) {
                continue;
            }
            SuperAgentDocumentVisualElement element = new SuperAgentDocumentVisualElement();
            element.setId(uidGenerator.getUid());
            element.setDocumentId(documentId);
            element.setParseTaskId(parseTaskId);
            element.setElementNo(candidate.getElementNo() == null ? elementNo : candidate.getElementNo());
            element.setElementType(StrUtil.blankToDefault(candidate.getElementType(), "IMAGE"));
            element.setPageNo(candidate.getPageNo());
            element.setStructureNodeId(candidate.getStructureNodeId());
            element.setSectionPath(candidate.getSectionPath());
            element.setAnchorText(candidate.getAnchorText());
            element.setOcrText(candidate.getOcrText());
            element.setCaptionText(candidate.getCaptionText());
            element.setBboxJson(StrUtil.blankToDefault(candidate.getBboxJson(), "{}"));
            element.setConfidence(candidate.getConfidence() == null ? BigDecimal.ZERO : candidate.getConfidence());
            element.setMetadataJson(StrUtil.blankToDefault(candidate.getMetadataJson(), "{}"));
            element.setStatus(BusinessStatus.YES.getCode());
            visualElementMapper.insert(element);
            savedElements.add(element);
            elementNo++;
        }
        return savedElements;
    }
}
