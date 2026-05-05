package org.javaup.ai.chatagent.model.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMemoryItem {

    private Long memoryId;

    private String memoryType;

    private String memoryKey;

    private String memoryText;

    private String sourceConversationId;

    private Long sourceExchangeId;

    private Integer importance;

    private BigDecimal confidence;

    private Double similarity;
}
