package io.github.regalpine.agentforge.capability;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory adapter registry.
 * AF-ENHANCE-02 §18: thread-safe
 * implementation backed by
 * ConcurrentHashMap.
 */
public final class InMemoryAdapterRegistry
    implements AdapterRegistry {

    private final Map<
        String,
        CapabilityAdapter
    > adapters = new ConcurrentHashMap<>();

    @Override
    public void register(
        CapabilityAdapter adapter
    ) {
        String id = adapter.metadata().adapterId();

        CapabilityAdapter previous =
            adapters.putIfAbsent(id, adapter);

        if (previous != null) {
            throw new IllegalStateException(
                "Adapter already registered: " + id
            );
        }
    }

    @Override
    public void unregister(
        String adapterId
    ) {
        CapabilityAdapter removed =
            adapters.remove(adapterId);

        if (removed != null) {
            removed.close();
        }
    }

    @Override
    public Optional<CapabilityAdapter> find(
        String adapterId
    ) {
        return Optional.ofNullable(
            adapters.get(adapterId)
        );
    }

    @Override
    public List<CapabilityAdapter> list() {
        return List.copyOf(
            new ArrayList<>(adapters.values())
        );
    }

    @Override
    public List<CapabilityAdapter> findByProtocol(
        String protocol
    ) {
        if (protocol == null || protocol.isBlank()) {
            return List.of();
        }

        List<CapabilityAdapter> result =
            new ArrayList<>();

        for (CapabilityAdapter adapter :
            adapters.values()
        ) {
            if (protocol.equals(
                adapter.metadata().protocol()
            )) {
                result.add(adapter);
            }
        }

        return List.copyOf(result);
    }

    @Override
    public Map<String, AdapterHealth> healthSummary() {
        Map<String, AdapterHealth> summary =
            new LinkedHashMap<>();

        for (var entry : adapters.entrySet()) {
            try {
                summary.put(
                    entry.getKey(),
                    entry.getValue().health()
                );
            } catch (Exception e) {
                summary.put(
                    entry.getKey(),
                    AdapterHealth.unavailable(
                        e.getMessage()
                    )
                );
            }
        }

        return Map.copyOf(summary);
    }

    @Override
    public void initialiseAll(
        Map<String, AdapterConfiguration> configs
    ) {
        for (var entry : adapters.entrySet()) {
            AdapterConfiguration config =
                configs.get(entry.getKey());

            if (config != null) {
                entry.getValue().initialise(config);
            }
        }
    }

    @Override
    public void closeAll() {
        for (CapabilityAdapter adapter :
            adapters.values()
        ) {
            try {
                adapter.close();
            } catch (Exception ignored) {
                // best-effort shutdown
            }
        }

        adapters.clear();
    }
}
