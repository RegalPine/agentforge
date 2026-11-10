package io.github.regalpine.agentforge.multiagent.team;

/**
 * CollaborationTopology —
 * AF-ENHANCE-08 §20.
 *
 * Defines how agents in a team
 * communicate and cooperate.
 *
 * <p>STAR: One coordinator,
 *   many workers.
 * <p>SEQUENTIAL: A → B → C
 * <p>PARALLEL: Fan-out, fan-in.
 * <p>PIPELINE: Sequential stages.
 * <p>HIERARCHICAL: Tree structure.
 * <p>PEER: All-to-all.
 */
public enum CollaborationTopology {

    /**
     * Star: one coordinator.
     *    A
     *   /|\
     *  B C D
     */
    STAR,

    /**
     * Sequential chain.
     * A → B → C
     */
    SEQUENTIAL,

    /**
     * Parallel fan-out/fan-in.
     *   ┌→ B
     * A─┼→ C
     *   └→ D
     */
    PARALLEL,

    /**
     * Pipeline stages.
     * Stage1 → Stage2 → Stage3
     */
    PIPELINE,

    /**
     * Hierarchical tree.
     */
    HIERARCHICAL,

    /**
     * Peer-to-peer.
     * A ↔ B ↔ C
     */
    PEER
}
