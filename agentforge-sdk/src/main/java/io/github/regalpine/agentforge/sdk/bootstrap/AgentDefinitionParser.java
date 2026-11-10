package io.github.regalpine.agentforge.sdk.bootstrap;

import io.github.regalpine.agentforge.sdk.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Parser for Agent Definition YAML files.
 * AF-ENHANCE-01 §12: Agent Definition.
 *
 * <p>Parses YAML files from the agents/
 * directory into AgentBuilder instances
 * that can produce Agent objects.
 *
 * <p>Example YAML:
 * <pre>
 * id: researcher
 * name: Researcher
 * model:
 *   ref: default
 * skills:
 *   - research
 * tools:
 *   - web-search
 * </pre>
 */
public final class AgentDefinitionParser {

    private AgentDefinitionParser() {}

    /**
     * Parse an agent definition from
     * a YAML file.
     */
    public static AgentDefinition parse(
        Path yamlFile
    ) {
        if (!Files.exists(yamlFile)) {
            throw new BootstrapException(
                "Agent definition not found: "
                    + yamlFile
            );
        }

        try {
            String content =
                Files.readString(yamlFile);
            return parseContent(content);
        } catch (IOException e) {
            throw new BootstrapException(
                "Failed to read agent definition: "
                    + yamlFile, e
            );
        }
    }

    /**
     * Parse an agent definition from
     * YAML content string.
     */
    public static AgentDefinition parseContent(
        String content
    ) {
        Map<String, Object> parsed =
            parseYaml(content);

        String id = getString(parsed, "id");
        if (id == null || id.isBlank()) {
            throw new BootstrapException(
                "Agent definition must have an 'id'"
            );
        }

        String name = getString(parsed, "name");
        List<String> skills = getStringList(
            parsed, "skills"
        );
        List<String> tools = getStringList(
            parsed, "tools"
        );
        String modelRef = null;
        Object modelObj = parsed.get("model");
        if (modelObj instanceof Map<?, ?> m) {
            @SuppressWarnings("unchecked")
            Map<String, Object> mm =
                (Map<String, Object>) m;
            modelRef = getString(mm, "ref");
        }

        return new AgentDefinition(
            id,
            name != null ? name : id,
            modelRef,
            skills,
            tools
        );
    }

    /**
     * Load all agent definitions from
     * a directory.
     */
    public static List<AgentDefinition> loadAll(
        Path agentsDir
    ) {
        if (!Files.isDirectory(agentsDir)) {
            return List.of();
        }

        List<AgentDefinition> defs =
            new ArrayList<>();

        try (var stream = Files.list(agentsDir)) {
            stream
                .filter(p ->
                    p.toString().endsWith(".yaml")
                    || p.toString().endsWith(".yml")
                )
                .forEach(p ->
                    defs.add(parse(p))
                );
        } catch (IOException e) {
            throw new BootstrapException(
                "Failed to scan agents directory: "
                    + agentsDir, e
        );
        }

        return List.copyOf(defs);
    }

    /**
     * Convert a definition to an
     * AgentBuilder.
     */
    public static AgentBuilder toBuilder(
        AgentDefinition def
    ) {
        AgentBuilder builder =
            new AgentBuilder(def.id());

        if (def.name() != null) {
            builder.description(def.name());
        }

        if (def.modelRef() != null) {
            builder.model("default", def.modelRef());
        }

        for (String skill : def.skills()) {
            builder.skill(skill);
        }

        for (String tool : def.tools()) {
            builder.tool(tool);
        }

        return builder;
    }

    // --- Simple YAML parser ---

    private static Map<String, Object> parseYaml(
        String content
    ) {
        Map<String, Object> result =
            new LinkedHashMap<>();
        String[] lines = content.split("\n");
        String currentSection = null;
        List<String> currentList = null;
        String listKey = null;
        String pendingSection = null;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.isEmpty()
                || trimmed.startsWith("#")
            ) {
                continue;
            }

            int indent = line.length()
                - line.stripLeading().length();

            // List item
            if (trimmed.startsWith("- ")) {
                // If we have a pending section,
                // convert it to a list
                if (pendingSection != null) {
                    currentList = new ArrayList<>();
                    listKey = pendingSection;
                    pendingSection = null;
                }
                if (currentList != null) {
                    currentList.add(
                        trimmed.substring(2).trim()
                    );
                }
                continue;
            }

            // Save pending list
            if (currentList != null
                && listKey != null
            ) {
                result.put(listKey,
                    List.copyOf(currentList));
                currentList = null;
                listKey = null;
            }

            // Save pending section as empty map
            if (pendingSection != null) {
                result.put(pendingSection,
                    new LinkedHashMap<>());
                currentSection = pendingSection;
                pendingSection = null;
            }

            int colonIdx = trimmed.indexOf(':');
            if (colonIdx < 0) continue;

            String key = trimmed.substring(
                0, colonIdx
            ).trim();
            String value = trimmed.substring(
                colonIdx + 1
            ).trim();

            if (indent == 0 && value.isEmpty()) {
                // Section header — defer
                // until we know if it's
                // a map or list
                pendingSection = key;
            } else if (indent > 0
                && currentSection != null
            ) {
                // Nested key
                Object sectionObj =
                    result.get(currentSection);
                if (sectionObj instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> section =
                        (Map<String, Object>) sectionObj;
                    if (value.isEmpty()) {
                        listKey = key;
                        currentList = new ArrayList<>();
                    } else {
                        section.put(key, value);
                    }
                }
            } else {
                result.put(key, value);
            }
        }

        // Save trailing list
        if (currentList != null
            && listKey != null
        ) {
            result.put(listKey,
                List.copyOf(currentList));
        }

        // Save trailing pending section
        if (pendingSection != null) {
            result.put(pendingSection,
                new LinkedHashMap<>());
        }

        return result;
    }

    private static String getString(
        Map<String, Object> map, String key
    ) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private static List<String> getStringList(
        Map<String, Object> map, String key
    ) {
        Object v = map.get(key);
        if (v instanceof List<?> list) {
            return list.stream()
                .map(Object::toString)
                .toList();
        }
        return List.of();
    }
}
