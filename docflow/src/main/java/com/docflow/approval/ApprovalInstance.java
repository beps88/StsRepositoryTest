package com.docflow.approval;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Running approval of one document version. Steps run in order; steps whose condition does not match the
 * document metadata are skipped. Thread-safe via method-level locking.
 */
public class ApprovalInstance {

    private final String id = UUID.randomUUID().toString();
    private final String documentId;
    private final int versionNumber;
    private final String versionHash;
    private final ApprovalDefinition definition;
    private final Map<String, String> metadata;
    private final Clock clock;
    private final List<AuditEvent> audit = new ArrayList<>();

    private Status status = Status.IN_PROGRESS;
    private int stepIndex = -1;
    private Instant stepStartedAt;
    /** approver (on whose behalf) -> actor who actually voted */
    private final Map<String, String> approvals = new HashMap<>();
    /** delegator -> delegate, for the current step only */
    private final Map<String, String> delegations = new HashMap<>();

    public ApprovalInstance(ApprovalDefinition definition, String documentId, int versionNumber,
                            String versionHash, Map<String, String> metadata, String startedBy, Clock clock) {
        this.definition = Objects.requireNonNull(definition);
        this.documentId = Objects.requireNonNull(documentId);
        this.versionNumber = versionNumber;
        this.versionHash = Objects.requireNonNull(versionHash);
        this.metadata = Map.copyOf(metadata);
        this.clock = Objects.requireNonNull(clock);
        if (startedBy == null || startedBy.isBlank()) {
            throw new IllegalArgumentException("startedBy required");
        }
        record(startedBy, "STARTED", definition.id());
        advanceFrom(0);
    }

    public synchronized void approve(String actor, String comment) {
        decide(actor, Action.APPROVE, comment);
    }

    public synchronized void reject(String actor, String comment) {
        decide(actor, Action.REJECT, comment);
    }

    public synchronized void requestChanges(String actor, String comment) {
        decide(actor, Action.REQUEST_CHANGES, comment);
    }

    public synchronized void decide(String actor, Action action, String comment) {
        requireInProgress();
        String approver = resolveApprover(actor);
        if (approvals.containsKey(approver)) {
            throw new ApprovalException(approver + " has already approved this step");
        }
        if (action != Action.APPROVE && (comment == null || comment.isBlank())) {
            throw new ApprovalException(action + " requires a comment");
        }
        String onBehalf = approver.equals(actor) ? "" : " (on behalf of " + approver + ")";
        record(actor, action.name(), step().name() + onBehalf + (comment == null ? "" : ": " + comment));
        switch (action) {
            case REJECT -> finish(Status.REJECTED, actor);
            case REQUEST_CHANGES -> finish(Status.CHANGES_REQUESTED, actor);
            case APPROVE -> {
                approvals.put(approver, actor);
                if (step().isSatisfiedBy(approvals.size())) {
                    record(actor, "STEP_COMPLETED", step().name());
                    advanceFrom(stepIndex + 1);
                }
            }
        }
    }

    /** Hand the current vote of {@code from} to {@code to}. The delegator can no longer act on this step. */
    public synchronized void delegate(String from, String to) {
        requireInProgress();
        if (to == null || to.isBlank() || to.equals(from)) {
            throw new ApprovalException("invalid delegate");
        }
        if (!step().approvers().contains(from)) {
            throw new ApprovalException(from + " is not an approver of the current step");
        }
        if (approvals.containsKey(from) || delegations.containsKey(from)) {
            throw new ApprovalException(from + " has already voted or delegated");
        }
        // One person must never hold two votes in the same step (four-eyes principle).
        if (step().approvers().contains(to) || delegations.containsValue(to)) {
            throw new ApprovalException(to + " already holds a vote in this step");
        }
        delegations.put(from, to);
        record(from, "DELEGATED", "to " + to);
    }

    public synchronized void cancel(String actor, String reason) {
        requireInProgress();
        if (reason == null || reason.isBlank()) {
            throw new ApprovalException("cancel requires a reason");
        }
        record(actor, "CANCELLED", reason);
        status = Status.CANCELLED;
    }

    public synchronized boolean isOverdue() {
        if (status != Status.IN_PROGRESS || step().sla() == null) {
            return false;
        }
        return Instant.now(clock).isAfter(stepStartedAt.plus(step().sla()));
    }

    /** Approvers (original assignees) who have not voted yet in the current step. */
    public synchronized Set<String> pendingApprovers() {
        if (status != Status.IN_PROGRESS) {
            return Set.of();
        }
        Set<String> pending = new LinkedHashSet<>(step().approvers());
        pending.removeAll(approvals.keySet());
        return pending;
    }

    /** People who can act right now: pending approvers, with delegators replaced by their delegates. */
    public synchronized Set<String> actionableBy() {
        Set<String> actors = new LinkedHashSet<>();
        for (String p : pendingApprovers()) {
            actors.add(delegations.getOrDefault(p, p));
        }
        return actors;
    }

    public synchronized Status status() {
        return status;
    }

    public synchronized String currentStepName() {
        return status == Status.IN_PROGRESS ? step().name() : null;
    }

    public synchronized List<AuditEvent> audit() {
        return List.copyOf(audit);
    }

    public String id() {
        return id;
    }

    public String documentId() {
        return documentId;
    }

    public int versionNumber() {
        return versionNumber;
    }

    public String versionHash() {
        return versionHash;
    }

    private String resolveApprover(String actor) {
        if (step().approvers().contains(actor) && !delegations.containsKey(actor)) {
            return actor;
        }
        return delegations.entrySet().stream()
                .filter(e -> e.getValue().equals(actor))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new ApprovalException(actor + " cannot act on step " + step().name()));
    }

    private StepDefinition step() {
        return definition.steps().get(stepIndex);
    }

    private void advanceFrom(int from) {
        List<StepDefinition> steps = definition.steps();
        for (int i = from; i < steps.size(); i++) {
            if (steps.get(i).applies(metadata)) {
                stepIndex = i;
                stepStartedAt = Instant.now(clock);
                approvals.clear();
                delegations.clear();
                record("system", "STEP_STARTED", steps.get(i).name());
                return;
            }
            record("system", "STEP_SKIPPED", steps.get(i).name());
        }
        finish(Status.APPROVED, "system");
    }

    private void finish(Status finalStatus, String actor) {
        status = finalStatus;
        record(actor, "COMPLETED", finalStatus.name());
    }

    private void requireInProgress() {
        if (status != Status.IN_PROGRESS) {
            throw new ApprovalException("approval is already " + status);
        }
    }

    private void record(String actor, String type, String detail) {
        audit.add(new AuditEvent(Instant.now(clock), actor, type, detail));
    }
}
