package com.docflow.approval;

public enum Status {
    IN_PROGRESS, APPROVED, REJECTED, CHANGES_REQUESTED, CANCELLED;

    public boolean isFinal() {
        return this != IN_PROGRESS;
    }
}
