package io.github.regalpine.agentforge.capability;

/**
 * Pipeline execution stage.
 * AE-02 §24: Invocation Pipeline stages.
 */
public enum InvocationStage {

    RESOLVE,
    VALIDATE,
    AUTHORIZE,
    BUDGET,
    OBSERVE,
    EXECUTE,
    VALIDATE_RESULT,
    AUDIT,
    RETURN
}
