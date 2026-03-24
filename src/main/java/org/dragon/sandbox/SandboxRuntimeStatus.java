package org.dragon.sandbox;

import org.dragon.sandbox.SandboxTypes.*;
import lombok.Builder;
import lombok.Data;

import java.util.*;

/**
 * 沙箱运行时状态
 * 决定会话是否应该使用沙箱隔离，并提供工具被阻止时的提示信息
 */
public final class SandboxRuntimeStatus {

    private SandboxRuntimeStatus() {
    }

    /** 运行时状态 */
    @Data
    @Builder
    public static class RuntimeStatus {
        private String agentId;
        private String sessionKey;
        private String mainSessionKey;
        private SandboxMode mode;
        private boolean sandboxed;
        private SandboxToolPolicyResolved toolPolicy;
    }

    /**
     * 解析沙箱运行时状态
     * 判断给定会话是否应该使用沙箱隔离
     *
     * @param sandboxConfig 沙箱配置
     * @param sessionKey 会话键
     * @param agentId Agent ID
     * @param mainSessionKey 主会话键
     * @return 运行时状态
     */
    public static RuntimeStatus resolveSandboxRuntimeStatus(
            SandboxConfig sandboxConfig,
            String sessionKey,
            String agentId,
            String mainSessionKey) {

        String sk = sessionKey == null ? "" : sessionKey.trim();
        boolean sandboxed = false;

        if (!sk.isEmpty()) {
            sandboxed = shouldSandboxSession(sandboxConfig, sk, mainSessionKey);
        }

        SandboxToolPolicyResolved toolPolicy = SandboxToolPolicyResolver
                .resolveSandboxToolPolicyForAgent(null, null);

        return RuntimeStatus.builder()
                .agentId(agentId != null ? agentId : "main")
                .sessionKey(sk)
                .mainSessionKey(mainSessionKey != null ? mainSessionKey : "main")
                .mode(sandboxConfig.getMode())
                .sandboxed(sandboxed)
                .toolPolicy(toolPolicy)
                .build();
    }

    /**
     * 判断会话是否应该使用沙箱
     */
    private static boolean shouldSandboxSession(SandboxConfig cfg, String sessionKey, String mainSessionKey) {
        if (cfg.getMode() == SandboxMode.OFF)
            return false;
        if (cfg.getMode() == SandboxMode.ALL)
            return true;
        // NON_MAIN 模式：除主会话外的所有会话都使用沙箱
        return !sessionKey.trim().equals(mainSessionKey != null ? mainSessionKey.trim() : "");
    }

    /**
     * 格式化工具被沙箱策略阻止的错误消息
     *
     * @param runtime 运行时状态
     * @param toolName 工具名称
     * @return 错误消息，如果工具未被阻止则返回 null
     */
    public static String formatSandboxToolPolicyBlockedMessage(
            RuntimeStatus runtime, String toolName) {

        String tool = toolName == null ? "" : toolName.trim().toLowerCase();
        if (tool.isEmpty())
            return null;
        if (!runtime.isSandboxed())
            return null;

        Set<String> denySet = new HashSet<>(
                runtime.getToolPolicy().getDeny() != null ? runtime.getToolPolicy().getDeny() : List.of());
        List<String> allow = runtime.getToolPolicy().getAllow() != null
                ? runtime.getToolPolicy().getAllow()
                : List.of();
        Set<String> allowSet = allow.isEmpty() ? null : new HashSet<>(allow);

        boolean blockedByDeny = denySet.contains(tool);
        boolean blockedByAllow = allowSet != null && !allowSet.contains(tool);

        if (!blockedByDeny && !blockedByAllow)
            return null;

        List<String> lines = new ArrayList<>();
        lines.add(String.format("Tool \"%s\" blocked by sandbox tool policy (mode=%s).", tool, runtime.getMode()));
        lines.add("Session: " + (runtime.getSessionKey().isEmpty() ? "(unknown)" : runtime.getSessionKey()));

        List<String> reasons = new ArrayList<>();
        List<String> fixes = new ArrayList<>();

        if (blockedByDeny) {
            reasons.add("deny list");
            fixes.add(String.format("Remove \"%s\" from %s.",
                    tool, runtime.getToolPolicy().getSources().getDeny().getKey()));
        }
        if (blockedByAllow) {
            reasons.add("allow list");
            fixes.add(String.format("Add \"%s\" to %s (or set it to [] to allow all).",
                    tool, runtime.getToolPolicy().getSources().getAllow().getKey()));
        }

        lines.add("Reason: " + String.join(" + ", reasons));
        lines.add("Fix:");
        lines.add("- agents.defaults.sandbox.mode=off (disable sandbox)");
        fixes.forEach(f -> lines.add("- " + f));

        if (runtime.getMode() == SandboxMode.NON_MAIN) {
            lines.add("- Use main session key (direct): " + runtime.getMainSessionKey());
        }

        return String.join("\n", lines);
    }
}
