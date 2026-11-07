package com.docflow.approval;

import java.time.Instant;

public record AuditEvent(Instant at, String actor, String type, String detail) {
}
