package io.github.regalpine.agentforge.service;

/**
 * Channel type.
 * AF-ENHANCE-11 §16, §61: Channel.
 */
public enum ChannelType {

    HTTP,

    SSE,

    WEBSOCKET,

    CLI,

    WEBHOOK,

    SDK,

    MQ
}
