package io.github.regalpine.agentforge.graph;

public enum NodeType {

    START,
    END,

    ACTION,
    AGENT,
    CAPABILITY,
    TOOL,
    LLM,

    CONDITION,
    SWITCH,

    PARALLEL,
    JOIN,

    LOOP,
    ITERATOR,

    WAIT,
    HUMAN,

    SUBGRAPH,

    SKILL
}
