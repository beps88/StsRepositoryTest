package com.docflow.document;

import java.util.List;
import java.util.Map;

public record Document(String id, String title, String documentClass, Map<String, String> metadata,
                       List<DocumentVersion> versions) {

    public Document {
        metadata = Map.copyOf(metadata);
        versions = List.copyOf(versions);
    }

    public DocumentVersion currentVersion() {
        return versions.get(versions.size() - 1);
    }
}
