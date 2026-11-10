package io.github.regalpine.agentforge.agent;

/**
 * Decision type enum.
 * AF-06 §12: DecisionType.
 *
 * <p>CONTINUE is not included as a standalone
 * type per spec — it can be expressed through
 * subsequent decisions.
 */
public enum DecisionType {

    CALL_CAPABILITY,
    CALL_SKILL,
    FINISH,
    WAIT,
    FAIL
}
