package org.dragon.sandbox;

import org.dragon.sandbox.SandboxTypes.*;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 沙箱工具策略解析器
 * 提供工具策略的pattern匹配和按Agent级别的策略解析
 */
public final class SandboxToolPolicyResolver {

    private SandboxToolPolicyResolver() {
    }

    // ── 模式匹配 ────────────────────────────────────────────

    /** 已编译的模式接口 */
    private sealed interface CompiledPattern {
        /** 匹配所有 */
        record All() implements CompiledPattern {
        }

        /** 精确匹配 */
        record Exact(String value) implements CompiledPattern {
        }

        /** 正则表达式匹配 */
        record Regex(Pattern pattern) implements CompiledPattern {
        }
    }

    /**
     * 将字符串模式编译为可匹配的对象
     */
    private static CompiledPattern compilePattern(String pattern) {
        String normalized = pattern == null ? "" : pattern.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return new CompiledPattern.Exact("");
        }
        if ("*".equals(normalized)) {
            return new CompiledPattern.All();
        }
        if (!normalized.contains("*")) {
            return new CompiledPattern.Exact(normalized);
        }
        // 将通配符转换为正则表达式
        String escaped = Pattern.quote(normalized).replace("\\*", ".*");
        return new CompiledPattern.Regex(Pattern.compile("^" + escaped + "$"));
    }

    /**
     * 编译一组模式
     */
    private static List<CompiledPattern> compilePatterns(List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return List.of();
        }
        return patterns.stream()
                .map(SandboxToolPolicyResolver::compilePattern)
                .filter(p -> !(p instanceof CompiledPattern.Exact e) || !e.value().isEmpty())
                .toList();
    }

    /**
     * 检查名称是否匹配任一模式
     */
    private static boolean matchesAny(String name, List<CompiledPattern> patterns) {
        for (CompiledPattern p : patterns) {
            if (p instanceof CompiledPattern.All)
                return true;
            if (p instanceof CompiledPattern.Exact e && name.equals(e.value()))
                return true;
            if (p instanceof CompiledPattern.Regex r && r.pattern().matcher(name).matches())
                return true;
        }
        return false;
    }

    /**
     * 检查工具是否被沙箱策略允许
     *
     * @param policy 工具策略
     * @param name 工具名称
     * @return 是否允许
     */
    public static boolean isToolAllowed(SandboxToolPolicy policy, String name) {
        String normalized = name == null ? "" : name.trim().toLowerCase();
        List<CompiledPattern> deny = compilePatterns(policy.getDeny());
        if (matchesAny(normalized, deny)) {
            return false;
        }
        List<CompiledPattern> allow = compilePatterns(policy.getAllow());
        if (allow.isEmpty()) {
            return true;
        }
        return matchesAny(normalized, allow);
    }

    // ── 策略解析 ───────────────────────────────────────────

    /**
     * 解析特定 Agent 的沙箱工具策略
     * 合并优先级：Agent 配置 > 全局配置 > 默认配置
     */
    public static SandboxToolPolicyResolved resolveSandboxToolPolicyForAgent(
            Map<String, Object> agentToolConfig,
            Map<String, Object> globalToolConfig) {

        @SuppressWarnings("unchecked")
        List<String> agentAllow = agentToolConfig != null
                ? (List<String>) agentToolConfig.get("allow")
                : null;
        @SuppressWarnings("unchecked")
        List<String> agentDeny = agentToolConfig != null
                ? (List<String>) agentToolConfig.get("deny")
                : null;
        @SuppressWarnings("unchecked")
        List<String> globalAllow = globalToolConfig != null
                ? (List<String>) globalToolConfig.get("allow")
                : null;
        @SuppressWarnings("unchecked")
        List<String> globalDeny = globalToolConfig != null
                ? (List<String>) globalToolConfig.get("deny")
                : null;

        SandboxToolPolicySource allowSource;
        if (agentAllow != null) {
            allowSource = SandboxToolPolicySource.builder()
                    .source(ToolPolicySourceType.AGENT)
                    .key("agents.list[].tools.sandbox.tools.allow").build();
        } else if (globalAllow != null) {
            allowSource = SandboxToolPolicySource.builder()
                    .source(ToolPolicySourceType.GLOBAL)
                    .key("tools.sandbox.tools.allow").build();
        } else {
            allowSource = SandboxToolPolicySource.builder()
                    .source(ToolPolicySourceType.DEFAULT)
                    .key("tools.sandbox.tools.allow").build();
        }

        SandboxToolPolicySource denySource;
        if (agentDeny != null) {
            denySource = SandboxToolPolicySource.builder()
                    .source(ToolPolicySourceType.AGENT)
                    .key("agents.list[].tools.sandbox.tools.deny").build();
        } else if (globalDeny != null) {
            denySource = SandboxToolPolicySource.builder()
                    .source(ToolPolicySourceType.GLOBAL)
                    .key("tools.sandbox.tools.deny").build();
        } else {
            denySource = SandboxToolPolicySource.builder()
                    .source(ToolPolicySourceType.DEFAULT)
                    .key("tools.sandbox.tools.deny").build();
        }

        List<String> deny = agentDeny != null ? agentDeny
                : globalDeny != null ? globalDeny
                        : new ArrayList<>(SandboxConstants.DEFAULT_TOOL_DENY);

        List<String> allow = agentAllow != null ? agentAllow
                : globalAllow != null ? globalAllow
                        : new ArrayList<>(SandboxConstants.DEFAULT_TOOL_ALLOW);

        // 确保 "image" 工具始终被允许（除非被显式拒绝）
        Set<String> denyLower = new HashSet<>();
        deny.stream().map(String::toLowerCase).forEach(denyLower::add);
        Set<String> allowLower = new HashSet<>();
        allow.stream().map(String::toLowerCase).forEach(allowLower::add);

        if (!denyLower.contains("image") && !allowLower.contains("image")) {
            allow = new ArrayList<>(allow);
            allow.add("image");
        }

        return SandboxToolPolicyResolved.builder()
                .allow(allow)
                .deny(deny)
                .sources(SandboxToolPolicyResolved.PolicySources.builder()
                        .allow(allowSource)
                        .deny(denySource)
                        .build())
                .build();
    }
}
