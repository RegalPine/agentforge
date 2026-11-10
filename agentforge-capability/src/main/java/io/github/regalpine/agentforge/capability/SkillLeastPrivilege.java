package io.github.regalpine.agentforge.capability;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Skill 最小权限执行器。
 *
 * <p>确保 Skill 只使用声明的权限。
 * 对应 AF-ENHANCE-05 §17。</p>
 */
public final class SkillLeastPrivilege {

    private final Set<String>
        allowedTools;
    private final Set<String>
        allowedModels;
    private final Set<String>
        allowedActions;
    private final long maxInputBytes;
    private final long maxOutputBytes;
    private final int maxExecutionTimeMs;

    public SkillLeastPrivilege(
        Set<String> allowedTools,
        Set<String> allowedModels,
        Set<String> allowedActions,
        long maxInputBytes,
        long maxOutputBytes,
        int maxExecutionTimeMs
    ) {
        this.allowedTools =
            allowedTools == null
                ? Set.of()
                : Set.copyOf(allowedTools);
        this.allowedModels =
            allowedModels == null
                ? Set.of()
                : Set.copyOf(allowedModels);
        this.allowedActions =
            allowedActions == null
                ? Set.of()
                : Set.copyOf(allowedActions);
        this.maxInputBytes = maxInputBytes;
        this.maxOutputBytes =
            maxOutputBytes;
        this.maxExecutionTimeMs =
            maxExecutionTimeMs;
    }

    /**
     * 检查工具是否被允许。
     */
    public boolean isToolAllowed(
        String toolName
    ) {
        return allowedTools.isEmpty()
            || allowedTools.contains(
                toolName
            );
    }

    /**
     * 检查模型是否被允许。
     */
    public boolean isModelAllowed(
        String modelId
    ) {
        return allowedModels.isEmpty()
            || allowedModels.contains(
                modelId
            );
    }

    /**
     * 检查操作是否被允许。
     */
    public boolean isActionAllowed(
        String action
    ) {
        return allowedActions.isEmpty()
            || allowedActions.contains(
                action
            );
    }

    /**
     * 检查输入大小是否超限。
     */
    public boolean isInputWithinLimit(
        long bytes
    ) {
        return maxInputBytes <= 0
            || bytes <= maxInputBytes;
    }

    /**
     * 检查输出大小是否超限。
     */
    public boolean isOutputWithinLimit(
        long bytes
    ) {
        return maxOutputBytes <= 0
            || bytes <= maxOutputBytes;
    }

    /**
     * 验证并返回违规列表。
     */
    public Set<String> validate(
        String toolName,
        String action
    ) {
        var violations =
            new java.util.HashSet<String>();
        if (!isToolAllowed(toolName)) {
            violations.add(
                "Tool not allowed: "
                    + toolName
            );
        }
        if (!isActionAllowed(action)) {
            violations.add(
                "Action not allowed: "
                    + action
            );
        }
        return violations;
    }

    public Set<String> allowedTools() {
        return allowedTools;
    }

    public Set<String> allowedModels() {
        return allowedModels;
    }

    public Set<String> allowedActions() {
        return allowedActions;
    }

    /**
     * 创建无限制实例。
     */
    public static SkillLeastPrivilege
    unrestricted() {
        return new SkillLeastPrivilege(
            Set.of(), Set.of(), Set.of(),
            0, 0, 0
        );
    }
}
