package io.github.regalpine.agentforge.capability;

import java.util.Map;

public record CapabilitySuccess(
    Map<String, Object> output
) implements CapabilityResult {

    public CapabilitySuccess {
        output =
            output == null
                ? Map.of()
                : Map.copyOf(output);
    }
}
