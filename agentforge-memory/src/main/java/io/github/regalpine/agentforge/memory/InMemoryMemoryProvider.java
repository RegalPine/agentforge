package io.github.regalpine.agentforge.memory;

/**
 * In-memory memory provider.
 * AE-08 §69: Development/testing provider.
 */
public final class InMemoryMemoryProvider
    implements MemoryProvider {

    private final InMemoryMemoryStore store;
    private final InMemoryMemoryRetriever retriever;
    private final MemoryCapabilities capabilities;

    public InMemoryMemoryProvider(
        MemoryPolicy policy,
        MemoryEventSink eventSink
    ) {
        this.store = new InMemoryMemoryStore();
        this.retriever =
            new InMemoryMemoryRetriever(
                store, policy, eventSink
            );
        this.capabilities =
            MemoryCapabilities.basic();
    }

    public InMemoryMemoryProvider(
        MemoryPolicy policy
    ) {
        this(policy, MemoryEventSink.noop());
    }

    public InMemoryMemoryProvider() {
        this(MemoryPolicy.permissive());
    }

    @Override
    public String providerType() {
        return "inmemory";
    }

    @Override
    public MemoryStore store() {
        return store;
    }

    @Override
    public MemoryRetriever retriever() {
        return retriever;
    }

    @Override
    public MemoryCapabilities capabilities() {
        return capabilities;
    }

    /**
     * Direct access to the underlying
     * store for testing.
     */
    public InMemoryMemoryStore rawStore() {
        return store;
    }
}
