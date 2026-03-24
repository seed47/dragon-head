package org.dragon.tools;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Agent 工具注册中心
 *
 * <p>
 * 负责管理所有可用的 Agent 工具，包括工具的注册、查找、过滤等功能。
 * 对应 TypeScript 中的 tool resolution (pi-tools.ts / openclaw-tools.ts)。
 * </p>
 */
@Slf4j
public class ToolRegistry {

    /** 工具名称到工具实例的映射 */
    private final Map<String, AgentTool> tools = new ConcurrentHashMap<>();

    /**
     * 注册一个工具。如果已存在同名工具，则覆盖。
     *
     * @param tool 要注册的工具
     */
    public void register(AgentTool tool) {
        tools.put(tool.getName(), tool);
        log.debug("Registered tool: {}", tool.getName());
    }

    /**
     * 批量注册工具。如果工具名称已存在，则跳过。
     *
     * @param toolList 要注册的工具集合
     */
    public void registerAll(Collection<AgentTool> toolList) {
        for (AgentTool tool : toolList) {
            tools.putIfAbsent(tool.getName(), tool);
        }
    }

    // /**
    //  * 注册一个由插件解析的工具，将其包装为 AgentTool。
    //  * ResolvedPluginTool 的处理器被适配到 AgentTool 接口。
    //  *
    //  * @param pluginTool 插件工具
    //  */
    // public void registerPluginTool(
    //         com.openclaw.plugin.tools.PluginToolResolver.ResolvedPluginTool pluginTool) {
    //     if (tools.containsKey(pluginTool.getName())) {
    //         log.warn("Plugin tool '{}' conflicts with existing tool, skipping",
    //                 pluginTool.getName());
    //         return;
    //     }
    //     AgentTool wrapped = new PluginToolAdapter(pluginTool);
    //     tools.put(pluginTool.getName(), wrapped);
    //     log.info("Registered plugin tool: {} (plugin: {})",
    //             pluginTool.getName(), pluginTool.getPluginId());
    // }

    /**
     * 根据名称获取工具。
     *
     * @param name 工具名称
     * @return 工具的 Optional 包装
     */
    public Optional<AgentTool> get(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    /**
     * 获取所有已注册工具的名称集合。
     *
     * @return 工具名称的不可变集合
     */
    public Set<String> getToolNames() {
        return Collections.unmodifiableSet(tools.keySet());
    }

    /**
     * 获取所有已注册的工具列表。
     *
     * @return 工具列表
     */
    public List<AgentTool> listAll() {
        return new ArrayList<>(tools.values());
    }

    /**
     * 将所有工具转换为 LLM 兼容的定义格式（名称 + 描述 + schema）。
     *
     * @return 工具定义列表
     */
    public List<Map<String, Object>> toDefinitions() {
        return tools.values().stream()
                .map(tool -> {
                    Map<String, Object> def = new LinkedHashMap<>();
                    def.put("name", tool.getName());
                    def.put("description", tool.getDescription());
                    def.put("input_schema", tool.getParameterSchema());
                    return def;
                })
                .collect(Collectors.toList());
    }

    /**
     * 返回已注册工具的数量。
     *
     * @return 工具数量
     */
    public int size() {
        return tools.size();
    }

    /**
     * 根据 ToolPolicy 过滤工具，返回允许使用的工具列表。
     *
     * @param policy 工具策略
     * @return 过滤后的工具列表
     */
    public List<AgentTool> filterByPolicy(ToolPolicy policy) {
        if (policy == null || policy == ToolPolicy.ALLOW_ALL) {
            return listAll();
        }
        List<AgentTool> filtered = new ArrayList<>();
        for (AgentTool tool : tools.values()) {
            if (policy.isAllowed(tool.getName())) {
                filtered.add(tool);
            }
        }
        return filtered;
    }

    /**
     * 将工具转换为特定 provider 的格式。
     *
     * @param provider 提供商名称 (anthropic, openai, google)
     * @return 格式化后的工具定义
     */
    public List<Map<String, Object>> toProviderDefinitions(String provider) {
        return ToolDefinitionAdapter.toProviderFormat(listAll(), provider);
    }
}
