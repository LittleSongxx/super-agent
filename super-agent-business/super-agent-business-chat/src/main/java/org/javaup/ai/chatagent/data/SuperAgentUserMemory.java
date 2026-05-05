package org.javaup.ai.chatagent.data;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.javaup.database.data.BaseTableData;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("super_agent_user_memory")
@EqualsAndHashCode(callSuper = true)
public class SuperAgentUserMemory extends BaseTableData {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    private String tenantId;

    private String userId;

    private String memoryType;

    private String memoryKey;

    private String memoryText;

    private String memoryJson;

    private String sourceConversationId;

    private Long sourceExchangeId;

    private Integer importance;

    private BigDecimal confidence;

    private String visibility;

    private Date effectiveFrom;

    private Date expiresAt;

    private Date lastAccessTime;

    private Integer accessCount;

    private Integer memoryStatus;
}
