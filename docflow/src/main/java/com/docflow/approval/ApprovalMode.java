package com.docflow.approval;

public enum ApprovalMode {
    /** One approval from any listed approver completes the step. */
    ANY,
    /** Every listed approver must approve. */
    ALL,
    /** At least {@code quorum} approvers must approve. */
    QUORUM
}
