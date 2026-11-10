package io.github.regalpine.agentforge.examples.agent;

import io.github.regalpine.agentforge.agent.AgentDefinition;
import io.github.regalpine.agentforge.agent.AgentProvider;
import io.github.regalpine.agentforge.agent.RegistrationOptions;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;

import java.util.List;
import java.util.Set;

/**
 * Example {@link AgentProvider} that
 * contributes two sample Agents to the
 * platform.
 *
 * <h2>How to use</h2>
 * <ol>
 *   <li>Implement {@link AgentProvider}</li>
 *   <li>Create
 *       {@code META-INF/services/io.github.regalpine.agentforge.agent.AgentProvider}
 *       containing the FQCN of this class</li>
 *   <li>Package into a jar and place on the
 *       server classpath</li>
 *   <li>Start the server — the Agents are
 *       auto-discovered and registered</li>
 * </ol>
 */
public class SampleAgentProvider
    implements AgentProvider {

    @Override
    public List<AgentDefinition> provide() {
        return List.of(
            new AgentDefinition(
                new EntityId("sample-translator"),
                new Version("1.0.0"),
                "Sample Translator Agent",
                Set.of()
            ),
            new AgentDefinition(
                new EntityId("sample-summarizer"),
                new Version("1.0.0"),
                "Sample Summarizer Agent",
                Set.of()
            )
        );
    }

    @Override
    public RegistrationOptions options(
        AgentDefinition agent
    ) {
        return RegistrationOptions.builder()
            .autoStart(true)
            .tag("provider", "sample")
            .build();
    }

    @Override
    public int order() {
        return 50;
    }
}
