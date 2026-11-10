package io.github.regalpine.agentforge.sdk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * YAML-like configuration loader.
 * AF-ENHANCE-04 §5: loads agentforge.yaml
 * with a priority chain:
 * Programmatic > Environment > Profile > Default.
 *
 * <p>Uses a lightweight key:value parser
 * (no external YAML library). Supports
 * flat and one-level nested keys.
 *
 * <p>Example agentforge.yaml:
 * <pre>
 * application:
 *   id: my-app
 *   tenant: default
 * model:
 *   provider: openai
 *   name: gpt-4
 * memory:
 *   store: postgresql
 *   scope: USER
 * </pre>
 */
public final class YamlConfigLoader {

    private YamlConfigLoader() {}

    /**
     * Load configuration with the full
     * priority chain.
     */
    public static AgentForgeConfig load() {
        // Priority: Programmatic > Env > Profile > Default
        Map<String, Object> defaults = loadDefaults();
        Map<String, Object> profile = loadProfile();
        Map<String, Object> env = loadEnvironment();

        Map<String, Object> merged = new LinkedHashMap<>();
        merged.putAll(defaults);
        merged.putAll(profile);
        merged.putAll(env);

        return buildFromMap(merged);
    }

    /**
     * Load from a specific YAML file.
     */
    public static AgentForgeConfig loadFrom(
        Path yamlFile
    ) {
        if (!Files.exists(yamlFile)) {
            return AgentForgeConfig.defaults();
        }

        try {
            String content = Files.readString(yamlFile);
            Map<String, Object> parsed =
                parseSimpleYaml(content);
            return buildFromMap(parsed);
        } catch (IOException e) {
            return AgentForgeConfig.defaults();
        }
    }

    /**
     * Load from the default location:
     * ./agentforge.yaml or
     * ~/.agentforge/agentforge.yaml
     */
    public static AgentForgeConfig loadFromFile() {
        Path local = Path.of("agentforge.yaml");

        if (Files.exists(local)) {
            return loadFrom(local);
        }

        Path home = Path.of(
            System.getProperty("user.home"),
            ".agentforge",
            "agentforge.yaml"
        );

        if (Files.exists(home)) {
            return loadFrom(home);
        }

        return AgentForgeConfig.defaults();
    }

    /**
     * Parse a simple YAML-like format.
     * Supports key: value and one-level
     * nesting via indentation.
     */
    static Map<String, Object> parseSimpleYaml(
        String content
    ) {
        Map<String, Object> result =
            new LinkedHashMap<>();

        String[] lines = content.split("\n");
        String currentSection = null;

        for (String line : lines) {
            // Skip comments and empty lines
            String trimmed = line.trim();

            if (trimmed.isEmpty()
                || trimmed.startsWith("#")
            ) {
                continue;
            }

            int indent = line.length()
                - line.stripLeading().length();

            int colonIdx = trimmed.indexOf(':');

            if (colonIdx < 0) continue;

            String key = trimmed.substring(
                0, colonIdx
            ).trim();

            String value = trimmed.substring(
                colonIdx + 1
            ).trim();

            if (indent == 0 && value.isEmpty()) {
                // Section header
                currentSection = key;
            } else if (indent > 0
                && currentSection != null
            ) {
                // Nested key
                String fullKey = currentSection
                    + "." + key;
                result.put(fullKey, parseValue(value));
            } else {
                result.put(key, parseValue(value));
            }
        }

        return result;
    }

    private static Object parseValue(String v) {
        if (v.isEmpty()) return "";

        // Strip quotes
        if (v.startsWith("\"")
            && v.endsWith("\"")
            && v.length() >= 2
        ) {
            return v.substring(1, v.length() - 1);
        }

        // Boolean
        if ("true".equalsIgnoreCase(v)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(v)) {
            return Boolean.FALSE;
        }

        // Number
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException ignored) {}

        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException ignored) {}

        return v;
    }

    private static Map<String, Object> loadDefaults() {
        return Map.of(
            "application.id", "default",
            "application.tenant", "default"
        );
    }

    private static Map<String, Object> loadProfile() {
        // Load from ~/.agentforge/agentforge.yaml
        Path home = Path.of(
            System.getProperty("user.home"),
            ".agentforge",
            "agentforge.yaml"
        );

        if (Files.exists(home)) {
            try {
                return parseSimpleYaml(
                    Files.readString(home)
                );
            } catch (IOException ignored) {}
        }

        return Map.of();
    }

    private static Map<String, Object> loadEnvironment() {
        Map<String, Object> env = new LinkedHashMap<>();

        String appId = System.getenv(
            "AGENTFORGE_APPLICATION_ID"
        );
        if (appId != null && !appId.isBlank()) {
            env.put("application.id", appId);
        }

        String tenant = System.getenv(
            "AGENTFORGE_TENANT_ID"
        );
        if (tenant != null && !tenant.isBlank()) {
            env.put("application.tenant", tenant);
        }

        String modelProvider = System.getenv(
            "AGENTFORGE_MODEL_PROVIDER"
        );
        if (modelProvider != null
            && !modelProvider.isBlank()
        ) {
            env.put("model.provider", modelProvider);
        }

        return env;
    }

    @SuppressWarnings("unchecked")
    private static AgentForgeConfig buildFromMap(
        Map<String, Object> map
    ) {
        AgentForgeConfig.Builder builder =
            AgentForgeConfig.builder();

        Object appId = map.get("application.id");
        if (appId != null) {
            builder.applicationId(appId.toString());
        }

        Object tenant = map.get("application.tenant");
        if (tenant != null) {
            builder.tenantId(tenant.toString());
        }

        // Collect model.* keys
        Map<String, Object> modelCfg = new LinkedHashMap<>();
        Map<String, Object> govCfg = new LinkedHashMap<>();
        Map<String, Object> obsCfg = new LinkedHashMap<>();
        Map<String, Object> props = new LinkedHashMap<>();

        for (var e : map.entrySet()) {
            String k = e.getKey();
            if (k.startsWith("model.")) {
                modelCfg.put(
                    k.substring("model.".length()),
                    e.getValue()
                );
            } else if (k.startsWith("governance.")) {
                govCfg.put(
                    k.substring("governance.".length()),
                    e.getValue()
                );
            } else if (k.startsWith("observability.")) {
                obsCfg.put(
                    k.substring("observability.".length()),
                    e.getValue()
                );
            } else if (!k.startsWith("application.")) {
                props.put(k, e.getValue());
            }
        }

        if (!modelCfg.isEmpty()) {
            builder.modelConfig(modelCfg);
        }
        if (!govCfg.isEmpty()) {
            builder.governanceConfig(govCfg);
        }
        if (!obsCfg.isEmpty()) {
            builder.observabilityConfig(obsCfg);
        }
        if (!props.isEmpty()) {
            builder.properties(props);
        }

        return builder.build();
    }
}
