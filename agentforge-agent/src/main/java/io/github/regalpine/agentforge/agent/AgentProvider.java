package io.github.regalpine.agentforge.agent;

import java.util.List;

/**
 * ServiceLoader-discoverable provider
 * of Agent definitions.
 *
 * <p>Users implement this interface to
 * contribute custom Agents to the
 * platform. At startup, the server scans
 * for providers via
 * {@link java.util.ServiceLoader}, sorts
 * them by {@link #order()}, and registers
 * each Agent through
 * {@link AgentRegistrar#registerAndStart}.
 *
 * <h2>Usage</h2>
 * <ol>
 *   <li>Implement this interface</li>
 *   <li>Create a file
 *       {@code META-INF/services/io.github.regalpine.agentforge.agent.AgentProvider}
 *       containing the FQCN of the impl</li>
 *   <li>Package the impl + service file
 *       into a jar and place it in the
 *       server's classpath (or plugins/
 *       directory when hot-load is enabled)</li>
 * </ol>
 *
 * <p>N: implementations MUST provide a
 * public no-arg constructor so that
 * {@link java.util.ServiceLoader} can
 * instantiate them.
 */
public interface AgentProvider {

    /**
     * The Agent definitions contributed
     * by this provider.
     *
     * @return a non-null list (may be empty)
     */
    List<AgentDefinition> provide();

    /**
     * Registration options for a given
     * Agent definition.
     *
     * <p>Default: {@link RegistrationOptions#defaults()}.
     *
     * @param agent one of the Agents
     *   returned by {@link #provide()}
     * @return the options to apply
     */
    default RegistrationOptions options(
        AgentDefinition agent
    ) {
        return RegistrationOptions.defaults();
    }

    /**
     * Provider ordering. Lower numbers
     * register first.
     *
     * <p>Default: 100.
     */
    default int order() {
        return 100;
    }
}
