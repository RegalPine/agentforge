package io.github.regalpine.agentforge.agent;

import io.github.regalpine.agentforge.kernel.Entity;
import io.github.regalpine.agentforge.kernel.Reference;

import java.util.Set;

public interface Agent extends Entity {

    String name();

    Set<Reference> capabilities();
}
