package org.dragon.tools.policy;

import org.dragon.tools.policy.ToolPolicyTypes.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 工具策略工具类
 *
 * <p>
 * 提供工具名称规范化、工具组扩展、工具配置解析等功能。
 * </p>
 */
@Slf4j
public final class ToolPolicyUtils {

    private ToolPolicyUtils() {
    }

    // --- 工具名称别名 ---

    private static final Map<String, String> TOOL_NAME_ALIASES = Map.of(
            "bash", "exec",
            "apply-patch", "apply_patch");

    /**
     * 规范化工具名称（小写、去除空格）。
     *
     * @param name 原始名称
     * @return 规范化后的名称
     */
    public static String normalizeToolName(String name) {
        if (name == null)
            return "";
        String normalized = name.trim().toLowerCase();
        return TOOL_NAME_ALIASES.getOrDefault(normalized, normalized);
    }

    /**
     * 规范化工具名称列表。
     *
     * @param list 原始列表
     * @return 规范化后的列表
     */
    public static List<String> normalizeToolList(List<String> list) {
        if (list == null)
            return List.of();
        return list.stream()
                .map(ToolPolicyUtils::normalizeToolName)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    // --- 工具组 ---

    /** 预定义的工具组映射 */
    public static final Map<String, List<String>> TOOL_GROUPS;
    static {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put("group:memory", List.of("memory_search", "memory_get"));
        m.put("group:web", List.of("web_search", "web_fetch"));
        m.put("group:fs", List.of("read", "write", "edit", "apply_patch"));
        m.put("group:runtime", List.of("exec", "process"));
        m.put("group:sessions",
                List.of("sessions_list", "sessions_history", "sessions_send", "sessions_spawn", "session_status"));
        m.put("group:ui", List.of("browser", "canvas"));
        m.put("group:automation", List.of("cron", "gateway"));
        m.put("group:messaging", List.of("message"));
        m.put("group:nodes", List.of("nodes"));
        m.put("group:dragonhead", List.of(
                "browser", "canvas", "nodes", "cron", "message", "gateway",
                "agents_list", "sessions_list", "sessions_history", "sessions_send",
                "sessions_spawn", "session_status", "memory_search", "memory_get",
                "web_search", "web_fetch", "image"));
        TOOL_GROUPS = Collections.unmodifiableMap(m);
    }

    /**
     * 展开工具组引用。
     *
     * @param list 包含组引用的列表
     * @return 展开后的工具列表
     */
    public static List<String> expandToolGroups(List<String> list) {
        if (list == null)
            return List.of();
        List<String> normalized = normalizeToolList(list);
        List<String> expanded = new ArrayList<>();
        for (String value : normalized) {
            List<String> group = TOOL_GROUPS.get(value);
            if (group != null) {
                expanded.addAll(group);
            } else {
                expanded.add(value);
            }
        }
        return expanded.stream().distinct().toList();
    }

    // --- 工具配置文件 ---

    /** 工具配置文件的预定义配置 */
    private static final Map<ToolProfileId, ToolPolicy> TOOL_PROFILES;
    static {
        Map<ToolProfileId, ToolPolicy> m = new EnumMap<>(ToolProfileId.class);
        m.put(ToolProfileId.minimal, new ToolPolicy(List.of("session_status"), null));
        m.put(ToolProfileId.coding, new ToolPolicy(
                List.of("group:fs", "group:runtime", "group:sessions", "group:memory", "image"), null));
        m.put(ToolProfileId.messaging, new ToolPolicy(
                List.of("group:messaging", "sessions_list", "sessions_history", "sessions_send", "session_status"),
                null));
        m.put(ToolProfileId.full, new ToolPolicy());
        TOOL_PROFILES = Collections.unmodifiableMap(m);
    }

    /**
     * 根据配置文件解析工具策略。
     *
     * @param profile 配置文件 ID
     * @return 工具策略
     */
    public static ToolPolicy resolveToolProfilePolicy(String profile) {
        if (profile == null || profile.isBlank())
            return null;
        try {
            ToolProfileId id = ToolProfileId.valueOf(profile.trim().toLowerCase());
            ToolPolicy resolved = TOOL_PROFILES.get(id);
            if (resolved == null || (resolved.allow() == null && resolved.deny() == null))
                return null;
            return new ToolPolicy(
                    resolved.allow() != null ? new ArrayList<>(resolved.allow()) : null,
                    resolved.deny() != null ? new ArrayList<>(resolved.deny()) : null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // --- 仅所有者工具 ---

    /** 仅所有者可用的工具名称集合 */
    private static final Set<String> OWNER_ONLY_TOOL_NAMES = Set.of("whatsapp_login");

    /**
     * 检查工具名称是否为仅所有者工具。
     *
     * @param name 工具名称
     * @return true 表示仅所有者可用
     */
    public static boolean isOwnerOnlyToolName(String name) {
        return OWNER_ONLY_TOOL_NAMES.contains(normalizeToolName(name));
    }

    // --- 显式允许列表收集 ---

    /**
     * 收集多个策略中的显式允许列表。
     *
     * @param policies 策略数组
     * @return 允许列表
     */
    @SafeVarargs
    public static List<String> collectExplicitAllowlist(ToolPolicy... policies) {
        List<String> entries = new ArrayList<>();
        for (ToolPolicy p : policies) {
            if (p == null || p.allow() == null)
                continue;
            for (String v : p.allow()) {
                if (v != null && !v.isBlank())
                    entries.add(v.trim());
            }
        }
        return entries;
    }
}
