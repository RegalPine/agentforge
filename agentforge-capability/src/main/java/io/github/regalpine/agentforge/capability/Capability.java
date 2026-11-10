package io.github.regalpine.agentforge.capability;

import io.github.regalpine.agentforge.kernel.Entity;
import io.github.regalpine.agentforge.kernel.Schema;

public interface Capability extends Entity {

    String name();

    Schema inputSchema();

    Schema outputSchema();
}
