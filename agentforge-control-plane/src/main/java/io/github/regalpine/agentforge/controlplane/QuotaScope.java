package io.github.regalpine.agentforge.controlplane;

/**
 * Quota scope levels.
 * AF-ENHANCE-14 §25: Resource Budget
 * Hierarchy.
 */
public enum QuotaScope {

    TENANT,

    APPLICATION,

    DEPLOYMENT,

    EXECUTION
}
