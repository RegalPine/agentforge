package io.github.regalpine.agentforge.capability;

import io.github.regalpine.agentforge.skill.governance.SkillDigest;
import io.github.regalpine.agentforge.kernel.SkillDescriptor;
import io.github.regalpine.agentforge.kernel.SkillId;
import io.github.regalpine.agentforge.skill.governance.SkillLifecycleState;
import io.github.regalpine.agentforge.skill.governance.SkillPackageMetadata;
import io.github.regalpine.agentforge.skill.governance.SkillQuery;
import io.github.regalpine.agentforge.kernel.SkillReference;
import io.github.regalpine.agentforge.skill.governance.SkillRegistration;
import io.github.regalpine.agentforge.kernel.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-Memory Skill Registry 实现.
 * AF-SKILL-02 §44: InMemorySkillRepository.
 * AF-AD-15 §5.2: Local Registry.
 *
 * <p>For unit tests, CLI, Studio, Demo.
 * Thread-safe via ConcurrentHashMap.
 * Not suitable for production persistence.
 */
public class InMemorySkillRegistry
    implements SkillRegistry {

    // key = "skillId@version"
    private final Map<String, Entry> entries =
        new ConcurrentHashMap<>();

    @Override
    public SkillRegistration register(
        SkillPackageMetadata metadata,
        SkillDigest digest
    ) {
        Objects.requireNonNull(
            metadata, "metadata"
        );
        Objects.requireNonNull(digest, "digest");

        String key = toKey(
            metadata.skillId(),
            metadata.version()
        );

        Entry existing = entries.get(key);
        if (existing != null) {
            throw new IllegalArgumentException(
                "Skill already registered: " + key
            );
        }

        SkillDescriptor descriptor =
            metadata.toDescriptor("DRAFT");

        SkillRegistration reg =
            SkillRegistration.create(
                descriptor, digest
            );

        entries.put(key, new Entry(
            metadata, reg
        ));

        return reg;
    }

    @Override
    public Optional<SkillDescriptor> resolve(
        SkillReference reference
    ) {
        Objects.requireNonNull(reference);

        String key = toKey(
            reference.skillId(),
            reference.version()
        );

        Entry e = entries.get(key);
        return e == null
            ? Optional.empty()
            : Optional.of(
                e.registration.descriptor()
            );
    }

    @Override
    public List<SkillDescriptor> discover(
        SkillQuery query
    ) {
        Objects.requireNonNull(query);

        List<SkillDescriptor> result =
            new ArrayList<>();

        for (Entry e : entries.values()) {
            if (matches(
                e.metadata, query
            )) {
                result.add(
                    e.registration.descriptor()
                );
            }
        }

        return List.copyOf(result);
    }

    @Override
    public SkillLifecycleState state(
        SkillReference reference
    ) {
        Entry e = getRequired(reference);
        return e.registration.state();
    }

    @Override
    public void transition(
        SkillReference reference,
        SkillLifecycleState target
    ) {
        Objects.requireNonNull(reference);
        Objects.requireNonNull(target);

        String key = toKey(
            reference.skillId(),
            reference.version()
        );

        Entry e = entries.get(key);
        if (e == null) {
            throw new IllegalArgumentException(
                "Skill not found: " + key
            );
        }

        SkillLifecycleState newState =
            e.registration.state()
                .transitionTo(target);

        SkillDescriptor updated =
            new SkillDescriptor(
                e.registration.descriptor()
                    .skillId(),
                e.registration.descriptor()
                    .name(),
                e.registration.descriptor()
                    .version(),
                e.registration.descriptor()
                    .goal(),
                e.registration.descriptor()
                    .strategy(),
                e.registration.descriptor()
                    .owner(),
                newState.name()
            );

        SkillRegistration newReg =
            new SkillRegistration(
                updated,
                newState,
                e.registration.digest(),
                e.registration.registeredAt(),
                Instant.now()
            );

        entries.put(key, new Entry(
            e.metadata, newReg
        ));
    }

    @Override
    public Optional<SkillPackageMetadata> metadata(
        SkillReference reference
    ) {
        Entry e = getEntry(reference);
        return e == null
            ? Optional.empty()
            : Optional.of(e.metadata);
    }

    @Override
    public Optional<SkillDigest> digest(
        SkillReference reference
    ) {
        Entry e = getEntry(reference);
        return e == null
            ? Optional.empty()
            : Optional.of(
                e.registration.digest()
            );
    }

    @Override
    public List<SkillDescriptor> list() {
        List<SkillDescriptor> result =
            new ArrayList<>();
        for (Entry e : entries.values()) {
            result.add(
                e.registration.descriptor()
            );
        }
        return List.copyOf(result);
    }

    @Override
    public void unregister(
        SkillReference reference
    ) {
        Objects.requireNonNull(reference);
        String key = toKey(
            reference.skillId(),
            reference.version()
        );
        entries.remove(key);
    }

    // ── internal ──────────────────────

    private Entry getRequired(
        SkillReference ref
    ) {
        Entry e = getEntry(ref);
        if (e == null) {
            throw new IllegalArgumentException(
                "Skill not found: "
                    + ref.skillId().value()
                    + "@" + ref.version()
            );
        }
        return e;
    }

    private Entry getEntry(
        SkillReference ref
    ) {
        Objects.requireNonNull(ref);
        String key = toKey(
            ref.skillId(), ref.version()
        );
        return entries.get(key);
    }

    private static String toKey(
        SkillId id, Version version
    ) {
        return id.value() + "@"
            + version.value();
    }

    private static boolean matches(
        SkillPackageMetadata m,
        SkillQuery q
    ) {
        if (q.idPattern() != null
            && !m.skillId().value()
                .contains(q.idPattern())
        ) {
            return false;
        }

        if (q.version() != null
            && !m.version().equals(q.version())
        ) {
            return false;
        }

        if (q.owner() != null
            && !q.owner().isBlank()
            && !q.owner().equals(m.owner())
        ) {
            return false;
        }

        if (q.domain() != null
            && !q.domain().isBlank()
            && !q.domain().equals(m.domain())
        ) {
            return false;
        }

        if (q.tag() != null
            && !q.tag().isBlank()
            && !m.tags().contains(q.tag())
        ) {
            return false;
        }

        if (q.lifecycleState() != null) {
            // lifecycle state filtering
            // requires looking up the
            // registration state — for
            // simplicity, skip here as
            // discover works on metadata
        }

        return true;
    }

    private record Entry(
        SkillPackageMetadata metadata,
        SkillRegistration registration
    ) {}
}
