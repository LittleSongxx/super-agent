package org.javaup.ai.chatagent.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgenticRagPlan {

    private boolean enabled;

    private String plannerMode;

    @Builder.Default
    private List<String> steps = new ArrayList<>();

    private String summary;
}
