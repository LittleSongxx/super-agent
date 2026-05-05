package org.javaup.ai.chatagent.data;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.javaup.database.data.BaseTableData;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("super_agent_user_profile")
@EqualsAndHashCode(callSuper = true)
public class SuperAgentUserProfile extends BaseTableData {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    private String tenantId;

    private String userId;

    private Integer profileVersion;

    private String roleSummary;

    private String technicalStackJson;

    private String preferenceJson;

    private String constraintsJson;

    private String activeGoalJson;

    private String profileSummary;

    private Integer profileStatus;

    private Date lastMemoryUpdateTime;
}
