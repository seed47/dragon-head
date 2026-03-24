package org.dragon.tools.policy;

import org.dragon.tools.policy.ToolPolicyTypes.ToolPolicy;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 工具创建协调器
 *
 * <p>
 * 负责解析策略、创建工具列表。
 * 简化版本，专注于核心策略解析和工具过滤功能。
 * </p>
 */
@Slf4j
public final class ToolCreator {

    private ToolCreator() {
    }

    /**
     * 创建工具集的选项（简化版）。
     */
    public static class CreateToolsOptions {
        /** 会话键 */
        public String sessionKey;
        /** 模型提供商 */
        public String modelProvider;
        /** 模型 ID */
        public String modelId;
        /** 全局配置 */
        public Map<String, Object> config;
        /** 发送者是否为所有者 */
        public boolean senderIsOwner;
    }

    /**
     * 检查模型提供商是否为 OpenAI 或 OpenAI-Codex。
     *
     * @param provider 提供商名称
     * @return true 表示是 OpenAI 提供商
     */
    public static boolean isOpenAIProvider(String provider) {
        if (provider == null)
            return false;
        String normalized = provider.trim().toLowerCase();
        return "openai".equals(normalized) || "openai-codex".equals(normalized);
    }

    /**
     * 检查 apply_patch 是否对当前模型允许。
     *
     * @param modelProvider 模型提供商
     * @param modelId       模型 ID
     * @param allowModels   允许的模型列表
     * @return true 表示允许
     */
    public static boolean isApplyPatchAllowedForModel(
            String modelProvider, String modelId, List<String> allowModels) {
        if (allowModels == null || allowModels.isEmpty())
            return true;
        if (modelId == null || modelId.isBlank())
            return false;
        String normalizedModelId = modelId.trim().toLowerCase();
        String provider = modelProvider != null ? modelProvider.trim().toLowerCase() : null;
        String normalizedFull = provider != null && !normalizedModelId.contains("/")
                ? provider + "/" + normalizedModelId
                : normalizedModelId;
        return allowModels.stream().anyMatch(entry -> {
            String normalized = entry.trim().toLowerCase();
            if (normalized.isEmpty())
                return false;
            return normalized.equals(normalizedModelId) || normalized.equals(normalizedFull);
        });
    }

    /**
     * 解析并过滤工具名称列表。
     *
     * @param availableToolNames 过滤前的可用工具名称
     * @param options            创建选项
     * @return 过滤后的工具名称列表
     */
    public static List<String> resolveAndFilterToolNames(
            List<String> availableToolNames, CreateToolsOptions options) {

        var effective = ToolPolicyResolver.resolveEffectiveToolPolicy(
                options.config, options.sessionKey, options.modelProvider, options.modelId);

        // 解析配置文件策略
        ToolPolicy profilePolicy = ToolPolicyUtils.resolveToolProfilePolicy(effective.profile());

        // 合并 alsoAllow
        profilePolicy = ToolPolicyResolver.mergeAlsoAllow(profilePolicy, effective.alsoAllow());
        profilePolicy = ToolPolicyResolver.mergeAlsoAllow(profilePolicy, effective.providerAlsoAllow());

        // 应用策略过滤
        List<String> filtered = availableToolNames;
        filtered = applyPolicyFilter(filtered, effective.globalPolicy());
        filtered = applyPolicyFilter(filtered, effective.globalProviderPolicy());
        filtered = applyPolicyFilter(filtered, effective.agentPolicy());
        filtered = applyPolicyFilter(filtered, effective.agentProviderPolicy());

        // 所有者-only 过滤
        if (!options.senderIsOwner) {
            filtered = filtered.stream()
                    .filter(name -> !ToolPolicyUtils.isOwnerOnlyToolName(name))
                    .toList();
        }

        return filtered;
    }

    private static List<String> applyPolicyFilter(List<String> toolNames, ToolPolicy policy) {
        if (policy == null)
            return toolNames;
        return ToolPolicyMatcher.filterToolNamesByPolicy(toolNames, policy);
    }
}
