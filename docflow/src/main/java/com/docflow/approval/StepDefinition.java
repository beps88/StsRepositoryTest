package com.docflow.approval;

import java.time.Duration;
import java.util.Set;

/**
 * One approval step. {@code condition} null means always applicable; {@code sla} null means no deadline.
 */
public record StepDefinition(String name, Set<String> approvers, ApprovalMode mode, int quorum,
                             Duration sla, Condition condition) {

    public StepDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("step name required");
        }
        if (approvers == null || approvers.isEmpty()) {
            throw new IllegalArgumentException("step needs at least one approver");
        }
        approvers = Set.copyOf(approvers);
        if (mode == null) {
            throw new IllegalArgumentException("mode required");
        }
        if (mode == ApprovalMode.QUORUM && (quorum < 1 || quorum > approvers.size())) {
            throw new IllegalArgumentException("quorum must be between 1 and " + approvers.size());
        }
        if (sla != null && (sla.isNegative() || sla.isZero())) {
            throw new IllegalArgumentException("sla must be positive");
        }
    }

    public static StepDefinition any(String name, Set<String> approvers) {
        return new StepDefinition(name, approvers, ApprovalMode.ANY, 0, null, null);
    }

    public static StepDefinition all(String name, Set<String> approvers) {
        return new StepDefinition(name, approvers, ApprovalMode.ALL, 0, null, null);
    }

    public static StepDefinition quorum(String name, Set<String> approvers, int quorum) {
        return new StepDefinition(name, approvers, ApprovalMode.QUORUM, quorum, null, null);
    }

    public StepDefinition withSla(Duration newSla) {
        return new StepDefinition(name, approvers, mode, quorum, newSla, condition);
    }

    public StepDefinition when(Condition newCondition) {
        return new StepDefinition(name, approvers, mode, quorum, sla, newCondition);
    }

    boolean applies(java.util.Map<String, String> metadata) {
        return condition == null || condition.matches(metadata);
    }

    boolean isSatisfiedBy(int approvals) {
        return switch (mode) {
            case ANY -> approvals >= 1;
            case ALL -> approvals >= approvers.size();
            case QUORUM -> approvals >= quorum;
        };
    }
}
