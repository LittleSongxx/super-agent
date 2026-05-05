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
public class CragEvaluationResult {

    private boolean enabled;

    private boolean passed;

    private boolean correctionRequired;

    private int referenceCount;

    private int coveredSubQuestionCount;

    @Builder.Default
    private List<String> weakReasons = new ArrayList<>();

    private String correctiveQuery;
}
