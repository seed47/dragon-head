package org.dragon.tools.policy;

import org.dragon.tools.policy.ToolPolicyTypes.ToolPolicy;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 工具策略匹配器
 *
 * <p>
 * 负责编译允许/拒绝模式、过滤工具列表、检查单个工具访问。
 * 使用简单的 glob 模式匹配（支持 * 通配符）。
 * </p>
 */
@Slf4j
public final class ToolPolicyMatcher {

    private ToolPolicyMatcher() {
    }

    /**
     * 检查工具名称是否被策略允许。
     *
     * @param name   工具名称
     * @param policy 工具策略
     * @return true 表示允许，false 表示拒绝
     */
    public static boolean isToolAllowedByPolicy(String name, ToolPolicy policy) {
        if (policy == null)
            return true;
        String normalized = ToolPolicyUtils.normalizeToolName(name);
        List<String> deny = ToolPolicyUtils.expandToolGroups(policy.deny());
        List<String> allow = ToolPolicyUtils.expandToolGroups(policy.allow());

        if (matchesAny(normalized, deny))
            return false;
        if (allow == null || allow.isEmpty())
            return true;
        if (matchesAny(normalized, allow))
            return true;
        // apply_patch 允许 if exec is allowed
        if ("apply_patch".equals(normalized) && matchesAny("exec", allow))
            return true;
        return false;
    }

    /**
     * 检查工具是否被所有策略允许。
     *
     * @param name     工具名称
     * @param policies 策略数组
     * @return true 表示所有策略都允许
     */
    public static boolean isToolAllowedByPolicies(String name, ToolPolicy... policies) {
        for (ToolPolicy p : policies) {
            if (!isToolAllowedByPolicy(name, p))
                return false;
        }
        return true;
    }

    /**
     * 根据策略过滤工具名称列表。
     *
     * @param toolNames 工具名称列表
     * @param policy    工具策略
     * @return 过滤后的工具名称列表
     */
    public static List<String> filterToolNamesByPolicy(List<String> toolNames, ToolPolicy policy) {
        if (policy == null)
            return toolNames;
        List<String> deny = ToolPolicyUtils.expandToolGroups(policy.deny());
        List<String> allow = ToolPolicyUtils.expandToolGroups(policy.allow());

        return toolNames.stream().filter(name -> {
            String normalized = ToolPolicyUtils.normalizeToolName(name);
            if (matchesAny(normalized, deny))
                return false;
            if (allow == null || allow.isEmpty())
                return true;
            if (matchesAny(normalized, allow))
                return true;
            if ("apply_patch".equals(normalized) && matchesAny("exec", allow))
                return true;
            return false;
        }).toList();
    }

    // --- 工具组默认拒绝列表 ---

    /** 子代理默认拒绝的工具列表 */
    private static final Set<String> DEFAULT_SUBAGENT_TOOL_DENY = Set.of(
            "sessions_list", "sessions_history", "sessions_send", "sessions_spawn",
            "gateway", "agents_list", "whatsapp_login", "session_status", "cron",
            "memory_search", "memory_get");

    /**
     * 解析子代理会话的工具策略。
     *
     * @param toolsConfig 工具配置
     * @return 工具策略
     */
    public static ToolPolicy resolveSubagentToolPolicy(Map<String, Object> toolsConfig) {
        Set<String> deny = new LinkedHashSet<>(DEFAULT_SUBAGENT_TOOL_DENY);
        List<String> allow = null;

        if (toolsConfig != null) {
            Object configuredDeny = toolsConfig.get("deny");
            if (configuredDeny instanceof List<?> l) {
                for (Object item : l) {
                    if (item instanceof String s)
                        deny.add(s);
                }
            }

            Object configuredAllow = toolsConfig.get("allow");
            if (configuredAllow instanceof List<?> l) {
                List<String> al = new ArrayList<>();
                for (Object item : l) {
                    if (item instanceof String s)
                        al.add(s);
                }
                if (!al.isEmpty())
                    allow = al;
            }
        }
        return new ToolPolicy(allow, new ArrayList<>(deny));
    }

    // --- 私有辅助方法 ---

    /**
     * 检查工具名称是否匹配任意一个模式。
     *
     * @param name     工具名称
     * @param patterns 模式列表
     * @return true 表示匹配任意模式
     */
    private static boolean matchesAny(String name, List<String> patterns) {
        if (patterns == null || patterns.isEmpty())
            return false;
        for (String pattern : patterns) {
            if (matchesGlob(name, pattern))
                return true;
        }
        return false;
    }

    /**
     * 简单的 glob 模式匹配，支持 * 通配符。
     *
     * @param name    工具名称
     * @param pattern 模式
     * @return true 表示匹配
     */
    private static boolean matchesGlob(String name, String pattern) {
        if (pattern == null || name == null)
            return false;
        if ("*".equals(pattern))
            return true;
        if (pattern.equals(name))
            return true;

        // 前缀通配符：bash* 匹配 bash, bash.exec
        if (pattern.endsWith("*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return name.startsWith(prefix);
        }
        // 后缀通配符：*.exec 匹配 bash.exec
        if (pattern.startsWith("*")) {
            String suffix = pattern.substring(1);
            return name.endsWith(suffix);
        }
        return false;
    }
}
