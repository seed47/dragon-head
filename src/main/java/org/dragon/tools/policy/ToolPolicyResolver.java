package org.dragon.tools.policy;

import org.dragon.tools.policy.ToolPolicyTypes.ToolPolicy;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 工具策略解析器
 *
 * <p>
 * 负责解析和组合全局、代理、提供商的工具策略。
 * 简化版本，只保留核心策略解析功能。
 * </p>
 */
@Slf4j
public final class ToolPolicyResolver {

    private ToolPolicyResolver() {
    }

    /**
     * 解析有效的工具策略。
     *
     * @param config        全局配置
     * @param sessionKey    会话键
     * @param modelProvider 模型提供商
     * @param modelId       模型 ID
     * @return 有效的工具策略
     */
    public static EffectiveToolPolicy resolveEffectiveToolPolicy(
            Map<String, Object> config,
            String sessionKey,
            String modelProvider,
            String modelId) {

        // 解析代理 ID
        String agentId = resolveAgentIdFromSessionKey(sessionKey);

        // 获取工具配置
        Map<String, Object> agentTools = resolveAgentToolsConfig(config, agentId);
        Map<String, Object> globalTools = getMapField(config, "tools");

        // 解析策略
        ToolPolicy globalPolicy = pickToolPolicy(globalTools);
        ToolPolicy agentPolicy = pickToolPolicy(agentTools);

        // 按提供商解析
        Map<String, Object> globalByProvider = getMapField(globalTools, "byProvider");
        Map<String, Object> agentByProvider = getMapField(agentTools, "byProvider");

        Map<String, Object> providerPolicyConfig = resolveProviderToolPolicy(globalByProvider, modelProvider, modelId);
        Map<String, Object> agentProviderPolicyConfig = resolveProviderToolPolicy(agentByProvider, modelProvider, modelId);

        ToolPolicy globalProviderPolicy = pickToolPolicy(providerPolicyConfig);
        ToolPolicy agentProviderPolicy = pickToolPolicy(agentProviderPolicyConfig);

        // alsoAllow 列表
        List<String> profileAlsoAllow = getStringListField(agentTools, "alsoAllow");
        if (profileAlsoAllow == null)
            profileAlsoAllow = getStringListField(globalTools, "alsoAllow");

        List<String> providerProfileAlsoAllow = getStringListField(agentProviderPolicyConfig, "alsoAllow");
        if (providerProfileAlsoAllow == null)
            providerProfileAlsoAllow = getStringListField(providerPolicyConfig, "alsoAllow");

        return new EffectiveToolPolicy(
                agentId, globalPolicy, globalProviderPolicy,
                agentPolicy, agentProviderPolicy,
                profileAlsoAllow, providerProfileAlsoAllow);
    }

    /**
     * 有效的工具策略结果。
     *
     * @param agentId             代理 ID
     * @param globalPolicy        全局策略
     * @param globalProviderPolicy   全局提供商策略
     * @param agentPolicy         代理策略
     * @param agentProviderPolicy    代理提供商策略
     * @param alsoAllow           额外允许的列表
     * @param providerAlsoAllow     提供商额外允许的列表
     */
    public record EffectiveToolPolicy(
            String agentId,
            ToolPolicy globalPolicy,
            ToolPolicy globalProviderPolicy,
            ToolPolicy agentPolicy,
            ToolPolicy agentProviderPolicy,
            List<String> alsoAllow,
            List<String> providerAlsoAllow) {

        /** 获取配置文件名称（如果存在） */
        public String profile() {
            return null;
        }
    }

    /**
     * 将 alsoAllow 合并到策略中。
     *
     * @param policy     原始策略
     * @param alsoAllow 额外允许的列表
     * @return 合并后的策略
     */
    public static ToolPolicy mergeAlsoAllow(ToolPolicy policy, List<String> alsoAllow) {
        if (policy == null || policy.allow() == null
                || alsoAllow == null || alsoAllow.isEmpty()) {
            return policy;
        }
        Set<String> merged = new LinkedHashSet<>(policy.allow());
        merged.addAll(alsoAllow);
        return new ToolPolicy(new ArrayList<>(merged), policy.deny());
    }

    // --- 辅助方法 ---

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getMapField(Map<String, Object> map, String key) {
        if (map == null)
            return null;
        Object val = map.get(key);
        return val instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
    }

    @SuppressWarnings("unchecked")
    private static List<String> getStringListField(Map<String, Object> map, String key) {
        if (map == null)
            return null;
        Object val = map.get(key);
        if (!(val instanceof List<?> l))
            return null;
        List<String> result = new ArrayList<>();
        for (Object item : l) {
            if (item instanceof String s)
                result.add(s);
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * 从配置中选取工具策略。
     *
     * @param config 配置
     * @return 工具策略
     */
    static ToolPolicy pickToolPolicy(Map<String, Object> config) {
        if (config == null)
            return null;
        List<String> allow = getStringListField(config, "allow");
        List<String> alsoAllow = getStringListField(config, "alsoAllow");

        // 合并 alsoAllow
        if (alsoAllow != null && !alsoAllow.isEmpty()) {
            if (allow == null || allow.isEmpty()) {
                List<String> star = new ArrayList<>();
                star.add("*");
                star.addAll(alsoAllow);
                allow = star.stream().distinct().toList();
            } else {
                Set<String> merged = new LinkedHashSet<>(allow);
                merged.addAll(alsoAllow);
                allow = new ArrayList<>(merged);
            }
        }
        List<String> deny = getStringListField(config, "deny");
        if (allow == null && deny == null)
            return null;
        return new ToolPolicy(allow, deny);
    }

    /**
     * 解析提供商特定的工具策略。
     *
     * @param byProvider     按提供商划分的配置
     * @param modelProvider  模型提供商
     * @param modelId        模型 ID
     * @return 提供商工具策略配置
     */
    @SuppressWarnings("unchecked")
    static Map<String, Object> resolveProviderToolPolicy(
            Map<String, Object> byProvider, String modelProvider, String modelId) {
        if (byProvider == null || modelProvider == null || modelProvider.isBlank())
            return null;

        String normalizedProvider = modelProvider.trim().toLowerCase();
        String rawModelId = modelId != null ? modelId.trim().toLowerCase() : null;
        String fullModelId = rawModelId != null && !rawModelId.contains("/")
                ? normalizedProvider + "/" + rawModelId
                : rawModelId;

        List<String> candidates = new ArrayList<>();
        if (fullModelId != null)
            candidates.add(fullModelId);
        candidates.add(normalizedProvider);

        for (String key : candidates) {
            for (Map.Entry<String, Object> entry : byProvider.entrySet()) {
                if (entry.getKey().trim().toLowerCase().equals(key)
                        && entry.getValue() instanceof Map<?, ?> m) {
                    return (Map<String, Object>) m;
                }
            }
        }
        return null;
    }

    /**
     * 从会话键解析代理 ID。
     *
     * @param sessionKey 会话键
     * @return 代理 ID
     */
    static String resolveAgentIdFromSessionKey(String sessionKey) {
        if (sessionKey == null || sessionKey.isBlank())
            return null;
        String trimmed = sessionKey.trim();
        if (!trimmed.startsWith("agent:"))
            return null;
        String[] parts = trimmed.split(":", 3);
        return parts.length >= 2 && !parts[1].isBlank() ? parts[1].trim() : null;
    }

    /**
     * 解析代理的工具配置。
     *
     * @param config   全局配置
     * @param agentId  代理 ID
     * @return 工具配置
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> resolveAgentToolsConfig(Map<String, Object> config, String agentId) {
        if (config == null || agentId == null)
            return null;
        Map<String, Object> agents = getMapField(config, "agents");
        if (agents == null)
            return null;
        Map<String, Object> agentConfig = getMapField(agents, agentId);
        if (agentConfig == null) {
            // 尝试从条目列表获取
            Object entries = agents.get("entries");
            if (entries instanceof List<?> l) {
                for (Object item : l) {
                    if (item instanceof Map<?, ?> m) {
                        Map<String, Object> entry = (Map<String, Object>) m;
                        if (agentId.equals(entry.get("id"))) {
                            agentConfig = entry;
                            break;
                        }
                    }
                }
            }
        }
        return agentConfig != null ? getMapField(agentConfig, "tools") : null;
    }
}
