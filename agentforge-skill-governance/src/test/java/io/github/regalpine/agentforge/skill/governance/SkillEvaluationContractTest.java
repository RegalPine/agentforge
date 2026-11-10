package io.github.regalpine.agentforge.skill.governance;

import io.github.regalpine.agentforge.skill.governance.SkillEvaluationContract;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SkillEvaluationContract 测试.
 * T-S4.1: 契约绑定 / 不可变性 /
 * 阈值校验.
 */
class SkillEvaluationContractTest {

    @Test
    void shouldCreateNoneContract() {
        SkillEvaluationContract c =
            SkillEvaluationContract.none();

        assertTrue(
            c.datasetReferences().isEmpty()
        );
        assertTrue(
            c.criteriaReferences().isEmpty()
        );
        assertTrue(
            c.requiredEvaluationModes()
                .isEmpty()
        );
        assertEquals(0.7, c.gateThreshold());
        assertFalse(
            c.requiresEvaluation()
        );
    }

    @Test
    void shouldCreateWithDatasets() {
        SkillEvaluationContract c =
            SkillEvaluationContract.of(
                List.of("ds-1", "ds-2"),
                List.of("c1"),
                Set.of("EXACT"),
                0.8
            );

        assertEquals(2,
            c.datasetReferences().size()
        );
        assertEquals(1,
            c.criteriaReferences()
                .size()
        );
        assertTrue(
            c.requiresEvaluation()
        );
        assertEquals(0.8, c.gateThreshold());
    }

    @Test
    void shouldRejectInvalidThreshold() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SkillEvaluationContract(
                List.of(), List.of(),
                Set.of(), -0.1
            )
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new SkillEvaluationContract(
                List.of(), List.of(),
                Set.of(), 1.1
            )
        );
    }

    @Test
    void shouldDefensivelyCopyCollections() {
        var datasets =
            new java.util.ArrayList<String>();
        datasets.add("ds-1");

        var modes =
            new java.util.HashSet<String>();
        modes.add("EXACT");

        SkillEvaluationContract c =
            new SkillEvaluationContract(
                datasets, List.of(),
                modes, 0.5
            );

        // Mutate originals
        datasets.add("ds-2");
        modes.add("SEMANTIC");

        // Contract should not change
        assertEquals(1,
            c.datasetReferences().size()
        );
        assertEquals(1,
            c.requiredEvaluationModes()
                .size()
        );
    }

    @Test
    void shouldHandleNullCollections() {
        SkillEvaluationContract c =
            new SkillEvaluationContract(
                null, null, null, 0.5
            );

        assertTrue(
            c.datasetReferences().isEmpty()
        );
        assertTrue(
            c.criteriaReferences()
                .isEmpty()
        );
        assertTrue(
            c.requiredEvaluationModes()
                .isEmpty()
        );
    }
}
