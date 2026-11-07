package com.docflow.document;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.docflow.TestClock;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentEngineTest {

    private ContentStore store;
    private DocumentEngine engine;

    @BeforeEach
    void setUp() {
        store = new InMemoryContentStore();
        engine = new DocumentEngine(store, new TestClock());
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void createStoresFirstVersionWithHash() {
        Document d = engine.create("Fattura 1", "INVOICE", Map.of("amount", "100"), "f.pdf", bytes("abc"), "ann");
        assertEquals(1, d.versions().size());
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", d.currentVersion().sha256());
        assertEquals(3, d.currentVersion().size());
        assertArrayEquals(bytes("abc"), engine.content(d.id(), 1));
    }

    @Test
    void addVersionKeepsHistoryImmutable() {
        Document d = engine.create("T", "C", Map.of(), "a.txt", bytes("v1"), "ann");
        Document d2 = engine.addVersion(d.id(), "a.txt", bytes("v2"), "bob");
        assertEquals(2, d2.versions().size());
        assertEquals(1, d.versions().size());
        assertNotEquals(d2.versions().get(0).sha256(), d2.versions().get(1).sha256());
        assertArrayEquals(bytes("v1"), engine.content(d.id(), 1));
        assertArrayEquals(bytes("v2"), engine.content(d.id(), 2));
        assertEquals("bob", d2.currentVersion().createdBy());
    }

    @Test
    void integrityDetectsMissingOrTamperedContent() {
        Document d = engine.create("T", "C", Map.of(), "a.txt", bytes("v1"), "ann");
        assertTrue(engine.verifyIntegrity(d.id(), 1));

        ContentStore tampered = new ContentStore() {
            @Override public void put(String sha256, byte[] content) { }
            @Override public java.util.Optional<byte[]> get(String sha256) {
                return java.util.Optional.of(bytes("evil"));
            }
        };
        DocumentEngine other = new DocumentEngine(tampered, new TestClock());
        Document d2 = other.create("T", "C", Map.of(), "a.txt", bytes("v1"), "ann");
        assertFalse(other.verifyIntegrity(d2.id(), 1));
    }

    @Test
    void updateMetadataReplacesMetadata() {
        Document d = engine.create("T", "C", Map.of("a", "1"), "a.txt", bytes("x"), "ann");
        Document u = engine.updateMetadata(d.id(), Map.of("a", "2"));
        assertEquals("2", u.metadata().get("a"));
        assertEquals("2", engine.get(d.id()).metadata().get("a"));
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> engine.create(" ", "C", Map.of(), "a", bytes("x"), "ann"));
        assertThrows(IllegalArgumentException.class, () -> engine.create("T", "C", Map.of(), "a", bytes("x"), ""));
        Document d = engine.create("T", "C", Map.of(), "a", bytes("x"), "ann");
        assertThrows(IllegalArgumentException.class, () -> engine.content(d.id(), 2));
        assertThrows(DocumentNotFoundException.class, () -> engine.get("nope"));
    }
}
