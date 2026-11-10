package ./myproject;

import io.github.regalpine.agentforge.sdk.AgentForge;
import io.github.regalpine.agentforge.sdk.ExecutionResult;

/**
 * ./my-project — main entry point.
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
