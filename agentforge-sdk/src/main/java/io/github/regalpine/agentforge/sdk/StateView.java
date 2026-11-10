package io.github.regalpine.agentforge.sdk;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Read-only view of execution state.
 * <p>
 * State modifications must go through the Runtime State Transition mechanism.
 * SDK does not allow direct state mutation.
 */
public interface StateView {

    /**
     * Get a value by key.
     *
     * @param key the state key
     * @param <T> the expected type
     * @return the value, or null if not present
     */
    <T> T get(String key);

    /**
     * Check if a key exists in the state.
     */
    boolean contains(String key);

    /**
     * Get an immutable snapshot of the current state.
     */
    Map<String, Object> snapshot();

    /**
     * Create a StateView from a map (for testing and simple use cases).
     */
    static StateView ofMap(Map<String, Object> state) {
        Objects.requireNonNull(state, "state cannot be null");
        Map<String, Object> copy = Map.copyOf(state);
        return new StateView() {
            @SuppressWarnings("unchecked")
            @Override
            public <T> T get(String key) {
                return (T) copy.get(key);
            }

            @Override
            public boolean contains(String key) {
                return copy.containsKey(key);
            }

            @Override
            public Map<String, Object> snapshot() {
                return copy;
            }
        };
    }
}
