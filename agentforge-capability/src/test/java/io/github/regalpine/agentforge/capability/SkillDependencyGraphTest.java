package io.github.regalpine.agentforge.capability;

import io.github.regalpine.agentforge.kernel.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SkillDependencyGraph 测试.
 * T-S1.17: 依赖图构建 / Cycle Detection /
 * 拓扑排序 / 传递依赖.
 */
class SkillDependencyGraphTest {

    @Test
    void shouldDetectNoCycleInAcyclicGraph() {
        SkillDependencyGraph graph =
            SkillDependencyGraph.builder()
                .addSkill(
                    new SkillId("A"),
                    SkillDependencies.of(
                        Set.of(),
                        Set.of(),
                        Set.of(
                            SkillReference.of(
                                "B",
                                "1.0.0"
                            ),
                            SkillReference.of(
                                "C",
                                "1.0.0"
                            )
                        )
                    )
                )
                .addSkill(
                    new SkillId("B"),
                    SkillDependencies.of(
                        Set.of(),
                        Set.of(),
                        Set.of(
                            SkillReference.of(
                                "D",
                                "1.0.0"
                            )
                        )
                    )
                )
                .addSkill(
                    new SkillId("C"),
                    SkillDependencies.empty()
                )
                .addSkill(
                    new SkillId("D"),
                    SkillDependencies.empty()
                )
                .build();

        // should not throw
        graph.validate();

        List<SkillId> cycle =
            graph.detectCycle();
        assertTrue(cycle.isEmpty());
    }

    @Test
    void shouldDetectDirectCycle() {
        SkillDependencyGraph graph =
            SkillDependencyGraph.builder()
                .addSkill(
                    new SkillId("X"),
                    SkillDependencies.of(
                        Set.of(),
                        Set.of(),
                        Set.of(
                            SkillReference.of(
                                "Y",
                                "1.0.0"
                            )
                        )
                    )
                )
                .addSkill(
                    new SkillId("Y"),
                    SkillDependencies.of(
                        Set.of(),
                        Set.of(),
                        Set.of(
                            SkillReference.of(
                                "X",
                                "1.0.0"
                            )
                        )
                    )
                )
                .build();

        assertThrows(
            IllegalStateException.class,
            graph::validate
        );

        List<SkillId> cycle =
            graph.detectCycle();
        assertFalse(cycle.isEmpty());
    }

    @Test
    void shouldDetectTransitiveCycle() {
        SkillDependencyGraph graph =
            SkillDependencyGraph.builder()
                .addSkill(
                    new SkillId("A"),
                    SkillDependencies.of(
                        Set.of(),
                        Set.of(),
                        Set.of(
                            SkillReference.of(
                                "B",
                                "1.0.0"
                            )
                        )
                    )
                )
                .addSkill(
                    new SkillId("B"),
                    SkillDependencies.of(
                        Set.of(),
                        Set.of(),
                        Set.of(
                            SkillReference.of(
                                "C",
                                "1.0.0"
                            )
                        )
                    )
                )
                .addSkill(
                    new SkillId("C"),
                    SkillDependencies.of(
                        Set.of(),
                        Set.of(),
                        Set.of(
                            SkillReference.of(
                                "A",
                                "1.0.0"
                            )
                        )
                    )
                )
                .build();

        assertThrows(
            IllegalStateException.class,
            graph::validate
        );
    }

    @Test
    void shouldReturnDirectDependencies() {
        SkillDependencyGraph graph =
            SkillDependencyGraph.builder()
                .addSkill(
                    new SkillId("parent"),
                    SkillDependencies.of(
                        Set.of(),
                        Set.of(),
                        Set.of(
                            SkillReference.of(
                                "child1",
                                "1.0.0"
                            ),
                            SkillReference.of(
                                "child2",
                                "1.0.0"
                            )
                        )
                    )
                )
                .addSkill(
                    new SkillId("child1"),
                    SkillDependencies.empty()
                )
                .addSkill(
                    new SkillId("child2"),
                    SkillDependencies.empty()
                )
                .build();

        Set<SkillId> deps =
            graph.dependenciesOf(
                new SkillId("parent")
            );

        assertEquals(2, deps.size());
    }

    @Test
    void shouldReturnTransitiveDeps() {
        SkillDependencyGraph graph =
            SkillDependencyGraph.builder()
                .addSkill(
                    new SkillId("A"),
                    SkillDependencies.of(
                        Set.of(),
                        Set.of(),
                        Set.of(
                            SkillReference.of(
                                "B",
                                "1.0.0"
                            )
                        )
                    )
                )
                .addSkill(
                    new SkillId("B"),
                    SkillDependencies.of(
                        Set.of(),
                        Set.of(),
                        Set.of(
                            SkillReference.of(
                                "C",
                                "1.0.0"
                            )
                        )
                    )
                )
                .addSkill(
                    new SkillId("C"),
                    SkillDependencies.empty()
                )
                .build();

        Set<SkillId> trans =
            graph.transitiveDeps(
                new SkillId("A")
            );

        assertTrue(
            trans.contains(new SkillId("B"))
        );
        assertTrue(
            trans.contains(new SkillId("C"))
        );
        assertEquals(2, trans.size());
    }

    @Test
    void shouldComputeTopologicalOrder() {
        SkillDependencyGraph graph =
            SkillDependencyGraph.builder()
                .addSkill(
                    new SkillId("A"),
                    SkillDependencies.of(
                        Set.of(),
                        Set.of(),
                        Set.of(
                            SkillReference.of(
                                "B",
                                "1.0.0"
                            )
                        )
                    )
                )
                .addSkill(
                    new SkillId("B"),
                    SkillDependencies.of(
                        Set.of(),
                        Set.of(),
                        Set.of(
                            SkillReference.of(
                                "C",
                                "1.0.0"
                            )
                        )
                    )
                )
                .addSkill(
                    new SkillId("C"),
                    SkillDependencies.empty()
                )
                .build();

        List<SkillId> order =
            graph.topologicalOrder();

        assertEquals(3, order.size());
        // C must come before B,
        // B before A
        int idxC = order.indexOf(
            new SkillId("C"));
        int idxB = order.indexOf(
            new SkillId("B"));
        int idxA = order.indexOf(
            new SkillId("A"));
        assertTrue(idxC < idxB);
        assertTrue(idxB < idxA);
    }

    @Test
    void emptyGraphShouldBeValid() {
        SkillDependencyGraph graph =
            SkillDependencyGraph.builder()
                .build();

        assertDoesNotThrow(
            graph::validate
        );
    }

    @Test
    void singleNodeShouldBeValid() {
        SkillDependencyGraph graph =
            SkillDependencyGraph.builder()
                .addSkill(
                    new SkillId("solo"),
                    SkillDependencies.empty()
                )
                .build();

        assertDoesNotThrow(
            graph::validate
        );

        List<SkillId> order =
            graph.topologicalOrder();
        assertEquals(1, order.size());
    }
}
