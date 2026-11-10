package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.hardening.*;
import io.github.regalpine.agentforge.memory.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-memory: Memory Store
 * / Scope / Ranking / Security.
 * AF-RELEASE-02 §4, T-H6.7.
 */
class MemoryConformanceTest {

    @Test
    void afMem001_saveAndRetrieve() {
        var store = new InMemoryMemoryStore();
        var record = MemoryRecord.builder()
            .memoryId("mem-001")
            .tenantId("t1")
            .subjectId("s1")
            .type(MemoryType.FACT)
            .scope(MemoryScope.USER)
            .content("Test fact")
            .build();

        store.save(record);

        var loaded = store.get("mem-001");
        assertTrue(loaded.isPresent());
        assertEquals("Test fact", loaded.get().content());
    }

    @Test
    void afMem002_softDelete() {
        var store = new InMemoryMemoryStore();
        store.save(MemoryRecord.builder()
            .memoryId("mem-del")
            .tenantId("t1")
            .subjectId("s1")
            .type(MemoryType.FACT)
            .scope(MemoryScope.USER)
            .content("To delete")
            .build()
        );

        store.delete("mem-del");

        var loaded = store.get("mem-del");
        assertTrue(loaded.isPresent());
        assertEquals(
            MemoryStatus.DELETED,
            loaded.get().status()
        );
    }

    @Test
    void afMem003_update() {
        var store = new InMemoryMemoryStore();
        var r1 = MemoryRecord.builder()
            .memoryId("mem-upd")
            .tenantId("t1")
            .subjectId("s1")
            .type(MemoryType.FACT)
            .scope(MemoryScope.USER)
            .content("v1")
            .build();

        store.save(r1);

        var r2 = MemoryRecord.builder()
            .memoryId("mem-upd")
            .tenantId("t1")
            .subjectId("s1")
            .type(MemoryType.FACT)
            .scope(MemoryScope.USER)
            .content("v2")
            .build();
        store.update(r2);

        assertEquals(
            "v2",
            store.get("mem-upd").orElseThrow().content()
        );
    }

    @Test
    void afMem004_exists() {
        var store = new InMemoryMemoryStore();
        store.save(MemoryRecord.builder()
            .memoryId("mem-ex")
            .tenantId("t1")
            .subjectId("s1")
            .type(MemoryType.FACT)
            .scope(MemoryScope.USER)
            .content("exists")
            .build()
        );

        assertTrue(store.exists("mem-ex"));
        assertFalse(store.exists("mem-no"));
    }

    @Test
    void afMem005_memoryTypes() {
        assertNotNull(MemoryType.CONVERSATION);
        assertNotNull(MemoryType.FACT);
        assertNotNull(MemoryType.PREFERENCE);
        assertNotNull(MemoryType.TASK);
        assertNotNull(MemoryType.EXPERIENCE);
        assertNotNull(MemoryType.SUMMARY);
    }

    @Test
    void afMem006_memoryScopes() {
        assertNotNull(MemoryScope.EXECUTION);
        assertNotNull(MemoryScope.CONVERSATION);
        assertNotNull(MemoryScope.USER);
        assertNotNull(MemoryScope.AGENT);
        assertNotNull(MemoryScope.TENANT);
    }
}
