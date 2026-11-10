package io.github.regalpine.agentforge.cli.command;

import io.github.regalpine.agentforge.cli.CliCommand;
import io.github.regalpine.agentforge.cli.CliOutput;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * CLI init command.
 * AF-ENHANCE-01 §8: agentforge init.
 *
 * <p>Usage:
 * <pre>
 * agentforge init &lt;project-name&gt;
 * </pre>
 *
 * <p>Creates the standard project structure:
 * <pre>
 * project-name/
 * ├── pom.xml
 * ├── agentforge.yaml
 * ├── agents/
 * │   └── default.yaml
 * ├── skills/
 * ├── tools/
 * ├── models/
 * ├── memory/
 * ├── policies/
 * ├── prompts/
 * ├── src/
 * │   ├── main/java/
 * │   └── test/java/
 * └── workspace/
 * </pre>
 */
public final class InitCommand
    implements CliCommand {

    @Override
    public String name() {
        return "init";
    }

    @Override
    public CliOutput execute(
        String[] args
    ) {
        if (args.length < 1
            || args[0].isBlank()
        ) {
            return CliOutput.failure(
                "Usage: agentforge init "
                    + "<project-name>"
            );
        }

        String projectName = args[0];
        Path projectDir = Path.of(projectName);

        if (Files.exists(projectDir)) {
            return CliOutput.failure(
                "Directory already exists: "
                    + projectName
            );
        }

        try {
            createProject(
                projectDir, projectName
            );
        } catch (IOException e) {
            return CliOutput.failure(
                "Failed to create project: "
                    + e.getMessage()
            );
        }

        return CliOutput.success(
            "Initialized AgentForge project '"
                + projectName + "'\n"
                + describeStructure(projectName)
        );
    }

    private void createProject(
        Path dir,
        String projectName
    ) throws IOException {
        // Create directories
        Files.createDirectories(dir);
        Files.createDirectories(
            dir.resolve("agents"));
        Files.createDirectories(
            dir.resolve("skills"));
        Files.createDirectories(
            dir.resolve("tools"));
        Files.createDirectories(
            dir.resolve("models"));
        Files.createDirectories(
            dir.resolve("memory"));
        Files.createDirectories(
            dir.resolve("policies"));
        Files.createDirectories(
            dir.resolve("prompts"));
        Files.createDirectories(
            dir.resolve("workspace"));
        Files.createDirectories(
            dir.resolve("src/main/java"));
        Files.createDirectories(
            dir.resolve("src/test/java"));

        // Generate pom.xml
        Files.writeString(
            dir.resolve("pom.xml"),
            pomContent(projectName)
        );

        // Generate agentforge.yaml
        Files.writeString(
            dir.resolve("agentforge.yaml"),
            yamlContent(projectName)
        );

        // Generate default agent
        Files.writeString(
            dir.resolve("agents/default.yaml"),
            defaultAgentContent(projectName)
        );

        // Generate Main.java
        String pkg = projectName
            .replace("-", "");
        Path mainDir = dir.resolve(
            "src/main/java/" + pkg
        );
        Files.createDirectories(mainDir);
        Files.writeString(
            mainDir.resolve("Main.java"),
            mainContent(pkg, projectName)
        );
    }

    private String pomContent(String name) {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                       https://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>%s</artifactId>
                <version>0.1.0</version>
                <dependencies>
                    <dependency>
                        <groupId>io.github.regalpine.agentforge</groupId>
                        <artifactId>agentforge-sdk</artifactId>
                        <version>1.0.0-SNAPSHOT</version>
                    </dependency>
                </dependencies>
            </project>
            """.formatted(name);
    }

    private String yamlContent(String name) {
        return """
            project:
              name: %s
              version: 0.1.0

            runtime:
              mode: development

            agent:
              default: default

            model:
              provider: default
              model: default

            observability:
              enabled: true

            security:
              enabled: true
            """.formatted(name);
    }

    private String defaultAgentContent(
        String projectName
    ) {
        return """
            id: default
            name: %s

            model:
              ref: default

            skills: []

            tools: []
            """.formatted(projectName);
    }

    private String mainContent(
        String pkg, String projectName
    ) {
        return """
            package %s;

            import io.github.regalpine.agentforge.sdk.AgentForge;
            import io.github.regalpine.agentforge.sdk.ExecutionResult;

            /**
             * %s — main entry point.
             */
            public class Main {
                public static void main(String[] args) {
                    ExecutionResult result = AgentForge.run(
                        "default",
                        "Hello, AgentForge!"
                    );
                    System.out.println("Result: " + result.output());
                }
            }
            """.formatted(pkg, projectName);
    }

    private String describeStructure(
        String projectName
    ) {
        return """
              %s/
              ├── pom.xml
              ├── agentforge.yaml
              ├── agents/
              │   └── default.yaml
              ├── skills/
              ├── tools/
              ├── models/
              ├── memory/
              ├── policies/
              ├── prompts/
              ├── src/
              │   ├── main/java/
              │   └── test/java/
              └── workspace/
            """.formatted(projectName);
    }
}
