package org.javaup.ai.manage.data;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.javaup.database.data.BaseTableData;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("super_agent_document_visual_element")
@EqualsAndHashCode(callSuper = true)
public class SuperAgentDocumentVisualElement extends BaseTableData {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    private Long documentId;

    private Long parseTaskId;

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
