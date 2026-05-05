package org.javaup.ai.manage.support;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVisualElementCandidate {

    private Integer elementNo;

    private String elementType;

    private Integer pageNo;

    private Long structureNodeId;

    private String sectionPath;

    private String anchorText;

    private String ocrText;

    private String captionText;

    private String bboxJson;

    private BigDecimal confidence;

    private String metadataJson;
}
