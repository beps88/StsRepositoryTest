package com.docflow.approval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.docflow.TestClock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApprovalInstanceTest {

    private TestClock clock;

    @BeforeEach
    void setUp() {
        clock = new TestClock();
    }

    private ApprovalInstance start(Map<String, String> metadata, StepDefinition... steps) {
        return new ApprovalInstance(new ApprovalDefinition("d", List.of(steps)), "doc1", 1, "hash", metadata,
                "ann", clock);
    }

    @Test
    void sequentialStepsApproveInOrder() {
        ApprovalInstance a = start(Map.of(),
                StepDefinition.any("manager", Set.of("mia")),
                StepDefinition.any("finance", Set.of("fred")));
        assertEquals("manager", a.currentStepName());
        assertThrows(ApprovalException.class, () -> a.approve("fred", null)); // not his step yet
        a.approve("mia", "ok");
        assertEquals("finance", a.currentStepName());
        a.approve("fred", null);
        assertEquals(Status.APPROVED, a.status());
        assertNull(a.currentStepName());
    }

    @Test
    void anyModeNeedsOneApproval() {
        ApprovalInstance a = start(Map.of(), StepDefinition.any("s", Set.of("x", "y")));
        a.approve("y", null);
        assertEquals(Status.APPROVED, a.status());
    }

    @Test
    void allModeNeedsEveryone() {
        ApprovalInstance a = start(Map.of(), StepDefinition.all("s", Set.of("x", "y")));
        a.approve("x", null);
        assertEquals(Status.IN_PROGRESS, a.status());
        assertEquals(Set.of("y"), a.pendingApprovers());
        a.approve("y", null);
        assertEquals(Status.APPROVED, a.status());
    }

    @Test
    void quorumModeNeedsN() {
        ApprovalInstance a = start(Map.of(), StepDefinition.quorum("s", Set.of("x", "y", "z"), 2));
        a.approve("x", null);
        assertEquals(Status.IN_PROGRESS, a.status());
        a.approve("z", null);
        assertEquals(Status.APPROVED, a.status());
    }

    @Test
    void cannotVoteTwiceOrWhenNotApprover() {
        ApprovalInstance a = start(Map.of(), StepDefinition.all("s", Set.of("x", "y")));
        a.approve("x", null);
        assertThrows(ApprovalException.class, () -> a.approve("x", null));
        assertThrows(ApprovalException.class, () -> a.approve("stranger", null));
    }

    @Test
    void rejectEndsApprovalAndNeedsComment() {
        ApprovalInstance a = start(Map.of(), StepDefinition.all("s", Set.of("x", "y")));
        assertThrows(ApprovalException.class, () -> a.reject("x", " "));
        a.reject("x", "wrong amount");
        assertEquals(Status.REJECTED, a.status());
        assertThrows(ApprovalException.class, () -> a.approve("y", null));
    }

    @Test
    void requestChangesEndsApprovalWithDedicatedStatus() {
        ApprovalInstance a = start(Map.of(), StepDefinition.any("s", Set.of("x")));
        assertThrows(ApprovalException.class, () -> a.requestChanges("x", null));
        a.requestChanges("x", "attach the contract");
        assertEquals(Status.CHANGES_REQUESTED, a.status());
    }

    @Test
    void conditionalStepIsSkippedWhenRuleDoesNotMatch() {
        StepDefinition cfo = StepDefinition.any("cfo", Set.of("carla"))
                .when(new Condition("amount", Operator.GT, "5000"));
        StepDefinition manager = StepDefinition.any("manager", Set.of("mia"));

        ApprovalInstance small = start(Map.of("amount", "100"), manager, cfo);
        small.approve("mia", null);
        assertEquals(Status.APPROVED, small.status());
        assertTrue(small.audit().stream().anyMatch(e -> e.type().equals("STEP_SKIPPED")));

        ApprovalInstance big = start(Map.of("amount", "9000"), manager, cfo);
        big.approve("mia", null);
        assertEquals("cfo", big.currentStepName());
    }

    @Test
    void allStepsSkippedApprovesImmediately() {
        ApprovalInstance a = start(Map.of("amount", "1"),
                StepDefinition.any("cfo", Set.of("carla")).when(new Condition("amount", Operator.GT, "5000")));
        assertEquals(Status.APPROVED, a.status());
    }

    @Test
    void delegationTransfersTheVote() {
        ApprovalInstance a = start(Map.of(), StepDefinition.any("s", Set.of("mia")));
        a.delegate("mia", "dan");
        assertEquals(Set.of("dan"), a.actionableBy());
        assertThrows(ApprovalException.class, () -> a.approve("mia", null)); // delegator gave it up
        a.approve("dan", "covering");
        assertEquals(Status.APPROVED, a.status());
        assertTrue(a.audit().stream().anyMatch(e -> e.detail().contains("on behalf of mia")));
    }

    @Test
    void delegationCannotGiveOnePersonTwoVotes() {
        ApprovalInstance a = start(Map.of(), StepDefinition.all("s", Set.of("mia", "max", "mo")));
        assertThrows(ApprovalException.class, () -> a.delegate("mia", "max")); // already an approver
        a.delegate("mia", "dan");
        assertThrows(ApprovalException.class, () -> a.delegate("max", "dan")); // already a delegate
        assertThrows(ApprovalException.class, () -> a.delegate("mia", "eve")); // already delegated
        assertThrows(ApprovalException.class, () -> a.delegate("stranger", "eve"));
        assertThrows(ApprovalException.class, () -> a.delegate("max", "max"));
    }

    @Test
    void delegationDoesNotCarryOverToNextStep() {
        ApprovalInstance a = start(Map.of(),
                StepDefinition.any("one", Set.of("mia")), StepDefinition.any("two", Set.of("mia")));
        a.delegate("mia", "dan");
        a.approve("dan", null);
        assertEquals(Set.of("mia"), a.actionableBy());
        assertThrows(ApprovalException.class, () -> a.approve("dan", null));
    }

    @Test
    void overdueFollowsSlaOfCurrentStep() {
        ApprovalInstance a = start(Map.of(),
                StepDefinition.any("s", Set.of("mia")).withSla(Duration.ofHours(24)));
        assertFalse(a.isOverdue());
        clock.advance(Duration.ofHours(25));
        assertTrue(a.isOverdue());
        a.approve("mia", null);
        assertFalse(a.isOverdue());
    }

    @Test
    void cancelNeedsReasonAndEndsApproval() {
        ApprovalInstance a = start(Map.of(), StepDefinition.any("s", Set.of("mia")));
        assertThrows(ApprovalException.class, () -> a.cancel("ann", ""));
        a.cancel("ann", "sent by mistake");
        assertEquals(Status.CANCELLED, a.status());
        assertTrue(a.pendingApprovers().isEmpty());
        assertThrows(ApprovalException.class, () -> a.cancel("ann", "again"));
    }

    @Test
    void auditTrailRecordsEveryTransitionInOrder() {
        ApprovalInstance a = start(Map.of(), StepDefinition.any("s", Set.of("mia")));
        a.approve("mia", "fine");
        List<String> types = a.audit().stream().map(AuditEvent::type).toList();
        assertEquals(List.of("STARTED", "STEP_STARTED", "APPROVE", "STEP_COMPLETED", "COMPLETED"), types);
    }

    @Test
    void stepDefinitionValidatesInput() {
        assertThrows(IllegalArgumentException.class, () -> StepDefinition.any("s", Set.of()));
        assertThrows(IllegalArgumentException.class, () -> StepDefinition.quorum("s", Set.of("a"), 2));
        assertThrows(IllegalArgumentException.class,
                () -> StepDefinition.any("s", Set.of("a")).withSla(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new ApprovalDefinition("d", List.of()));
    }
}
