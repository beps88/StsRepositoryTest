package com.docflow.document;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryContentStore implements ContentStore {

    private final Map<String, byte[]> blobs = new ConcurrentHashMap<>();

    @Override
    public void put(String sha256, byte[] content) {
        blobs.putIfAbsent(sha256, content.clone());
    }

    @Override
    public Optional<byte[]> get(String sha256) {
        return Optional.ofNullable(blobs.get(sha256)).map(byte[]::clone);
    }
}
