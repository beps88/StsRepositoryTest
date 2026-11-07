package com.docflow.approval;

import java.util.List;

public record ApprovalDefinition(String id, List<StepDefinition> steps) {

    public ApprovalDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("definition id required");
        }
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("definition needs at least one step");
        }
        steps = List.copyOf(steps);
    }
}
