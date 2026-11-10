package io.github.regalpine.agentforge.capability.http;

/**
 * HTTP authentication type.
 * AF-ENHANCE-02 §8.
 */
public enum HttpAuthType {

    NONE,

    BEARER_TOKEN,

    API_KEY,

    BASIC
}
