package io.github.regalpine.agentforge.a2a;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory external agent registry.
 * AF-ENHANCE-10 §27-28.
 */
public final class InMemoryExternalAgentRegistry
    implements ExternalAgentRegistry {

    private final Map<String, AgentCard>
        cards = new ConcurrentHashMap<>();

    @Override
    public void register(AgentCard card) {
        if (card == null) {
            throw new IllegalArgumentException(
                "card must not be null"
            );
        }

        cards.put(card.agentId(), card);
    }

    @Override
    public Optional<AgentCard> get(
        String agentId
    ) {
        if (agentId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
            cards.get(agentId)
        );
    }

    @Override
    public List<AgentCard> list() {
        return List.copyOf(cards.values());
    }

    @Override
    public List<AgentCard> findByCapability(
        String capability
    ) {
        if (capability == null
            || capability.isBlank()
        ) {
            return List.of();
        }

        List<AgentCard> result =
            new ArrayList<>();

        for (AgentCard card : cards.values()) {
            if (card.hasCapability(
                capability
            )) {
                result.add(card);
            }
        }

        return List.copyOf(result);
    }

    @Override
    public void unregister(
        String agentId
    ) {
        cards.remove(agentId);
    }
}
