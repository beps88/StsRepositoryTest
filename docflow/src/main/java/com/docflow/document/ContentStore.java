package com.docflow.document;

import java.util.Optional;

/** Content-addressed blob storage. Production implementations: S3-compatible, filesystem, etc. */
public interface ContentStore {

    void put(String sha256, byte[] content);

    Optional<byte[]> get(String sha256);
}
