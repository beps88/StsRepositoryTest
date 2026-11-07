package com.docflow.document;

import java.time.Instant;

/** Immutable snapshot of a document's content. The hash binds approvals and audits to exact bytes. */
public record DocumentVersion(int number, String fileName, long size, String sha256,
                              String createdBy, Instant createdAt) {
}
