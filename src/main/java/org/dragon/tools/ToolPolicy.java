package org.dragon.tools;

import lombok.extern.slf4j.Slf4j;
import org.dragon.sandbox.SandboxTypes;

import java.util.*;

/**
 * 工具执行策略
 *
 * <p>
 * 控制哪些工具被允许或拒绝，基于配置。
 * 对应 TypeScript 中的 tool-policy.ts / pi-tools.policy.ts。
 * </p>
 *
 * <p>
 * 策略从沙箱配置或 Agent 特定覆盖中解析。工具可以被全局允许/拒绝，
 * 或者按模式（类似 glob 的匹配）限定范围。
 * </p>
 */
@Slf4j
public class ToolPolicy {

    /**
     * 默认的允许所有策略（当没有配置沙箱/工具限制时使用）。
     */
    public static final ToolPolicy ALLOW_ALL = new ToolPolicy(Collections.emptySet(), Collections.emptySet(), true);

    /** 允许的模式集合 */
    private final Set<String> allowPatterns;
    /** 拒绝的模式集合 */
    private final Set<String> denyPatterns;
    /** 默认是否允许 */
    private final boolean defaultAllow;

    public ToolPolicy(Set<String> allowPatterns, Set<String> denyPatterns, boolean defaultAllow) {
        this.allowPatterns = allowPatterns != null ? allowPatterns : Collections.emptySet();
        this.denyPatterns = denyPatterns != null ? denyPatterns : Collections.emptySet();
        this.defaultAllow = defaultAllow;
    }

    /**
     * 从沙箱配置构建 ToolPolicy。
     * 如果设置了沙箱 tools.allow，则只允许这些工具（白名单模式）。
     * 如果设置了沙箱 tools.deny，则允许除拒绝外的所有工具（黑名单模式）。
     *
     * @param toolsConfig 沙箱工具配置
     * @return 工具策略
     */
    public static ToolPolicy fromConfig(SandboxTypes.SandboxToolPolicy toolsConfig) {
        if (toolsConfig == null) {
            return ALLOW_ALL;
        }
        Set<String> allow = toolsConfig.getAllow() != null
                ? new HashSet<>(toolsConfig.getAllow())
                : Collections.emptySet();
        Set<String> deny = toolsConfig.getDeny() != null
                ? new HashSet<>(toolsConfig.getDeny())
                : Collections.emptySet();

        // 如果设置了允许列表，默认拒绝所有（白名单模式）
        boolean defaultPermit = allow.isEmpty();
        return new ToolPolicy(allow, deny, defaultPermit);
    }

    /**
     * 检查工具是否被此策略允许。
     *
     * @param toolName 要检查的工具名称
     * @return true 表示允许，false 表示拒绝
     */
    public boolean isAllowed(String toolName) {
        if (toolName == null || toolName.isBlank())
            return false;

        // 明确的拒绝始终优先
        if (matchesAny(toolName, denyPatterns)) {
            log.debug("Tool '{}' denied by deny-list", toolName);
            return false;
        }

        // 明确的允许
        if (!allowPatterns.isEmpty()) {
            boolean allowed = matchesAny(toolName, allowPatterns);
            if (!allowed) {
                log.debug("Tool '{}' not in allow-list", toolName);
            }
            return allowed;
        }

        return defaultAllow;
    }

    /**
     * 过滤工具名称集合，只返回允许的工具。
     *
     * @param toolNames 工具名称集合
     * @return 允许的工具名称列表
     */
    public List<String> filterAllowed(Collection<String> toolNames) {
        List<String> result = new ArrayList<>();
        for (String name : toolNames) {
            if (isAllowed(name)) {
                result.add(name);
            }
        }
        return result;
    }

    /**
     * 简单的 glob 模式匹配：支持 * 作为通配符。
     *
     * @param name    工具名称
     * @param pattern 模式
     * @return 是否匹配
     */
    private boolean matchesAny(String toolName, Set<String> patterns) {
        for (String pattern : patterns) {
            if (matchesGlob(toolName, pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesGlob(String name, String pattern) {
        if (pattern.equals("*"))
            return true;
        if (pattern.equals(name))
            return true;

        // 简单通配符："bash*" 匹配 "bash", "bash.exec" 等
        if (pattern.endsWith("*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return name.startsWith(prefix);
        }
        if (pattern.startsWith("*")) {
            String suffix = pattern.substring(1);
            return name.endsWith(suffix);
        }
        return false;
    }

    @Override
    public String toString() {
        if (this == ALLOW_ALL)
            return "ToolPolicy[ALLOW_ALL]";
        return String.format("ToolPolicy[allow=%s, deny=%s, default=%s]",
                allowPatterns, denyPatterns, defaultAllow ? "allow" : "deny");
    }
}
