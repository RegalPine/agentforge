package io.github.regalpine.agentforge.cli.command;

import io.github.regalpine.agentforge.cli.CliCommand;
import io.github.regalpine.agentforge.cli.CliOutput;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * CLI create command.
 * AF-ENHANCE-04 §6: project scaffolding.
 *
 * <p>Usage:
 * <pre>
 * agentforge create &lt;template&gt; &lt;project-name&gt;
 * </pre>
 *
 * <p>Templates:
 * <ul>
 *   <li>basic-agent — Simple agent</li>
 *   <li>graph-agent — Graph-based agent</li>
 *   <li>rag-agent — RAG-enhanced agent</li>
 *   <li>tool-agent — Tool-using agent</li>
 *   <li>multi-agent — Multi-agent system</li>
 * </ul>
 */
public final class CreateCommand
    implements CliCommand {

    private static final Map<String, Template>
        TEMPLATES = new LinkedHashMap<>();

    static {
        TEMPLATES.put("basic-agent",
            new Template(
                "basic-agent",
                "Simple single-agent project",
                List.of("Main.java",
                    "agentforge.yaml")
            ));

        TEMPLATES.put("graph-agent",
            new Template(
                "graph-agent",
                "Graph-based multi-step agent",
                List.of("Main.java",
                    "MyGraph.java",
                    "agentforge.yaml")
            ));

        TEMPLATES.put("rag-agent",
            new Template(
                "rag-agent",
                "RAG-enhanced agent with knowledge",
                List.of("Main.java",
                    "KnowledgeSetup.java",
                    "agentforge.yaml")
            ));

        TEMPLATES.put("tool-agent",
            new Template(
                "tool-agent",
                "Agent with tool capabilities",
                List.of("Main.java",
                    "MyTools.java",
                    "agentforge.yaml")
            ));

        TEMPLATES.put("multi-agent",
            new Template(
                "multi-agent",
                "Multi-agent collaboration",
                List.of("Main.java",
                    "AgentA.java",
                    "AgentB.java",
                    "agentforge.yaml")
            ));
    }

    @Override
    public String name() {
        return "create";
    }

    @Override
    public CliOutput execute(String[] args) {
        if (args.length < 1) {
            return CliOutput.failure(
                "Usage: agentforge create "
                + "<template> [project-name]\n"
                + "Templates: "
                + String.join(", ",
                    TEMPLATES.keySet())
            );
        }

        String templateName = args[0];
        String projectName =
            args.length > 1 ? args[1] : "my-agent";

        Template template =
            TEMPLATES.get(templateName);

        if (template == null) {
            return CliOutput.failure(
                "Unknown template: "
                + templateName + "\n"
                + "Available: "
                + String.join(", ",
                    TEMPLATES.keySet())
            );
        }

        Path projectDir = Path.of(projectName);

        try {
            createProject(
                projectDir, template, projectName
            );
        } catch (IOException e) {
            return CliOutput.failure(
                "Failed to create project: "
                + e.getMessage()
            );
        }

        return CliOutput.success(
            "Created project '" + projectName
            + "' from template '"
            + templateName + "'\n"
            + "Files:\n"
            + template.describe(projectName)
        );
    }

    private void createProject(
        Path dir,
        Template template,
        String projectName
    ) throws IOException {
        Files.createDirectories(dir);

        // Create src directory
        Path srcDir = dir.resolve("src")
            .resolve("main")
            .resolve("java")
            .resolve("com")
            .resolve("example");

        Files.createDirectories(srcDir);

        // Generate files
        for (String file : template.files()) {
            Path target = file.equals("agentforge.yaml")
                ? dir.resolve(file)
                : srcDir.resolve(file);

            String content = generateContent(
                file, projectName, template
            );

            Files.writeString(target, content);
        }
    }

    private String generateContent(
        String file,
        String projectName,
        Template template
    ) {
        String pkg = "com.example."
            + projectName.replace("-", "");

        return switch (file) {
            case "agentforge.yaml" ->
                yamlContent(projectName);
            case "Main.java" ->
                mainContent(pkg, template.name());
            case "MyGraph.java" ->
                graphContent(pkg);
            case "KnowledgeSetup.java" ->
                knowledgeContent(pkg);
            case "MyTools.java" ->
                toolsContent(pkg);
            case "AgentA.java" ->
                agentAContent(pkg);
            case "AgentB.java" ->
                agentBContent(pkg);
            default -> "// " + file;
        };
    }

    private String yamlContent(String name) {
        return """
            application:
              id: %s
              tenant: default
            model:
              provider: mock
              name: test-model
            """.formatted(name);
    }

    private String mainContent(
        String pkg, String template
    ) {
        return """
            package %s;

            import io.github.regalpine.agentforge.sdk.AgentForge;
            import io.github.regalpine.agentforge.sdk.AgentForgeConfig;

            /**
             * %s project: %s
             */
            public class Main {
                public static void main(String[] args) {
                    AgentForge forge = AgentForge.bootstrap(
                        AgentForgeConfig.defaults()
                    );
                    System.out.println("AgentForge %s started");
                }
            }
            """.formatted(
                pkg, template,
                "Main entry point", template
            );
    }

    private String graphContent(String pkg) {
        return """
            package %s;

            import io.github.regalpine.agentforge.sdk.GraphBuilder;
            import io.github.regalpine.agentforge.sdk.GraphDescriptor;

            /**
             * Graph definition.
             */
            public class MyGraph {
                public static GraphDescriptor build() {
                    return new GraphBuilder("my-graph")
                        .node("start", "classifier")
                        .node("end", "responder")
                        .edge("start", "end")
                        .build();
                }
            }
            """.formatted(pkg);
    }

    private String knowledgeContent(String pkg) {
        return """
            package %s;

            import io.github.regalpine.agentforge.sdk.RagApi;

            /**
             * Knowledge setup.
             */
            public class KnowledgeSetup {
                public static void setup(RagApi rag) {
                    rag.upsert("doc-1", "Sample knowledge");
                }
            }
            """.formatted(pkg);
    }

    private String toolsContent(String pkg) {
        return """
            package %s;

            /**
             * Tool definitions.
             */
            public class MyTools {
                public static String search(String query) {
                    return "Result for: " + query;
                }
            }
            """.formatted(pkg);
    }

    private String agentAContent(String pkg) {
        return """
            package %s;

            /**
             * Agent A in multi-agent system.
             */
            public class AgentA {
                public static String name() {
                    return "agent-a";
                }
            }
            """.formatted(pkg);
    }

    private String agentBContent(String pkg) {
        return """
            package %s;

            /**
             * Agent B in multi-agent system.
             */
            public class AgentB {
                public static String name() {
                    return "agent-b";
                }
            }
            """.formatted(pkg);
    }

    /**
     * Get available template names.
     */
    public static Set<String> templateNames() {
        return TEMPLATES.keySet();
    }

    /**
     * Template descriptor.
     */
    record Template(
        String name,
        String description,
        List<String> files
    ) {
        String describe(String projectName) {
            StringBuilder sb = new StringBuilder();
            for (String f : files) {
                if (f.equals("agentforge.yaml")) {
                    sb.append("  ")
                        .append(projectName)
                        .append("/")
                        .append(f)
                        .append("\n");
                } else {
                    sb.append("  ")
                        .append(projectName)
                        .append("/src/main/java/com/example/")
                        .append(projectName.replace("-", ""))
                        .append("/")
                        .append(f)
                        .append("\n");
                }
            }
            return sb.toString();
        }
    }
}
