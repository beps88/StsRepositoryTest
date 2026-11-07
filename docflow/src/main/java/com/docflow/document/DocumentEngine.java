package com.docflow.document;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Versioned document store with typed metadata and content-integrity checks. */
public class DocumentEngine {

    private final ContentStore store;
    private final Clock clock;
    private final Map<String, Document> documents = new HashMap<>();

    public DocumentEngine(ContentStore store, Clock clock) {
        this.store = Objects.requireNonNull(store);
        this.clock = Objects.requireNonNull(clock);
    }

    public synchronized Document create(String title, String documentClass, Map<String, String> metadata,
                                        String fileName, byte[] content, String actor) {
        requireText(title, "title");
        requireText(documentClass, "documentClass");
        requireText(actor, "actor");
        String id = UUID.randomUUID().toString();
        DocumentVersion v = storeVersion(1, fileName, content, actor);
        Document doc = new Document(id, title, documentClass, metadata, List.of(v));
        documents.put(id, doc);
        return doc;
    }

    public synchronized Document addVersion(String documentId, String fileName, byte[] content, String actor) {
        requireText(actor, "actor");
        Document doc = get(documentId);
        List<DocumentVersion> versions = new ArrayList<>(doc.versions());
        versions.add(storeVersion(versions.size() + 1, fileName, content, actor));
        Document updated = new Document(doc.id(), doc.title(), doc.documentClass(), doc.metadata(), versions);
        documents.put(documentId, updated);
        return updated;
    }

    public synchronized Document updateMetadata(String documentId, Map<String, String> metadata) {
        Document doc = get(documentId);
        Document updated = new Document(doc.id(), doc.title(), doc.documentClass(), metadata, doc.versions());
        documents.put(documentId, updated);
        return updated;
    }

    public synchronized Document get(String documentId) {
        Document doc = documents.get(documentId);
        if (doc == null) {
            throw new DocumentNotFoundException(documentId);
        }
        return doc;
    }

    public byte[] content(String documentId, int versionNumber) {
        DocumentVersion v = version(get(documentId), versionNumber);
        return store.get(v.sha256()).orElseThrow(() -> new IllegalStateException("Missing content " + v.sha256()));
    }

    /** True if the stored bytes still match the hash recorded when the version was created. */
    public boolean verifyIntegrity(String documentId, int versionNumber) {
        DocumentVersion v = version(get(documentId), versionNumber);
        return store.get(v.sha256()).map(b -> sha256(b).equals(v.sha256())).orElse(false);
    }

    private DocumentVersion version(Document doc, int number) {
        if (number < 1 || number > doc.versions().size()) {
            throw new IllegalArgumentException("No version " + number + " for document " + doc.id());
        }
        return doc.versions().get(number - 1);
    }

    private DocumentVersion storeVersion(int number, String fileName, byte[] content, String actor) {
        requireText(fileName, "fileName");
        Objects.requireNonNull(content, "content");
        String hash = sha256(content);
        store.put(hash, content);
        return new DocumentVersion(number, fileName, content.length, hash, actor, Instant.now(clock));
    }

    static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
