package org.javaup.ai.manage.support;

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
public class DocumentVisualAnalysisResult {

    @Builder.Default
    private List<DocumentVisualElementCandidate> elements = new ArrayList<>();

    private String mergedVisualText;

    public boolean isEmpty() {
        return (elements == null || elements.isEmpty())
            && (mergedVisualText == null || mergedVisualText.isBlank());
    }
}
