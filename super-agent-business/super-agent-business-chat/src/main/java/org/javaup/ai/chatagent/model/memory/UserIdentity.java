package org.javaup.ai.chatagent.model.memory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserIdentity {

    private String tenantId;

    private String userId;
}
