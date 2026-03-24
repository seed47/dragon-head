package org.dragon.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 工具定义适配器
 *
 * <p>
 * 将 AgentTool 定义适配为 LLM provider 特定的格式。
 * 对应 TypeScript 中的 pi-tool-definition-adapter.ts。
 * </p>
 *
 * <p>
 * 支持的格式：
 * </p>
 * <ul>
 * <li>Anthropic tool_use 格式</li>
 * <li>OpenAI function calling 格式</li>
 * <li>Gemini function declarations 格式</li>
 * </ul>
 */
@Slf4j
public final class ToolDefinitionAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ToolDefinitionAdapter() {
    }

    /**
     * 将工具转换为 Anthropic tool_use 格式。
     *
     * <pre>
     * { "name": "...", "description": "...", "input_schema": { ... } }
     * </pre>
     *
     * @param tools 工具集合
     * @return Anthropic 格式的工具定义列表
     */
    public static List<Map<String, Object>> toAnthropicFormat(Collection<AgentTool> tools) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (AgentTool tool : tools) {
            Map<String, Object> def = new LinkedHashMap<>();
            def.put("name", normalizeToolName(tool.getName()));
            def.put("description", tool.getDescription());
            JsonNode schema = ToolSchemaAdapter.normalizeToolParameters(tool.getParameterSchema());
            def.put("input_schema", schema);
            result.add(def);
        }
        return result;
    }

    /**
     * 将工具转换为 OpenAI function calling 格式。
     *
     * <pre>
     * { "type": "function", "function": { "name": "...", "description": "...", "parameters": { ... } } }
     * </pre>
     *
     * @param tools 工具集合
     * @return OpenAI 格式的工具定义列表
     */
    public static List<Map<String, Object>> toOpenAIFormat(Collection<AgentTool> tools) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (AgentTool tool : tools) {
            Map<String, Object> func = new LinkedHashMap<>();
            func.put("name", normalizeToolName(tool.getName()));
            func.put("description", tool.getDescription());
            JsonNode schema = ToolSchemaAdapter.normalizeToolParameters(tool.getParameterSchema());
            func.put("parameters", schema);

            Map<String, Object> wrapper = new LinkedHashMap<>();
            wrapper.put("type", "function");
            wrapper.put("function", func);
            result.add(wrapper);
        }
        return result;
    }

    /**
     * 将工具转换为 Gemini function declarations 格式。
     *
     * <pre>
     * { "name": "...", "description": "...", "parameters": { ... } }
     * </pre>
     *
     * @param tools 工具集合
     * @return Gemini 格式的工具定义列表
     */
    public static List<Map<String, Object>> toGeminiFormat(Collection<AgentTool> tools) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (AgentTool tool : tools) {
            Map<String, Object> def = new LinkedHashMap<>();
            def.put("name", normalizeToolName(tool.getName()));
            def.put("description", tool.getDescription());
            JsonNode schema = ToolSchemaAdapter.cleanSchemaForGemini(
                    ToolSchemaAdapter.normalizeToolParameters(tool.getParameterSchema()));
            def.put("parameters", schema);
            result.add(def);
        }
        return result;
    }

    /**
     * 将工具转换为适合特定 provider 的格式。
     *
     * @param tools    Agent 工具集合
     * @param provider provider 名称 (anthropic, openai, google 等)
     * @return 格式化后的工具定义
     */
    public static List<Map<String, Object>> toProviderFormat(
            Collection<AgentTool> tools, String provider) {
        if (provider == null)
            return toAnthropicFormat(tools);
        return switch (provider.toLowerCase().trim()) {
            case "openai", "openai-codex" -> toOpenAIFormat(tools);
            case "google", "gemini" -> toGeminiFormat(tools);
            default -> toAnthropicFormat(tools);
        };
    }

    /**
     * 规范化工具名称（小写，将空格/连字符替换为下划线）。
     *
     * @param name 原始工具名称
     * @return 规范化后的名称
     */
    public static String normalizeToolName(String name) {
        if (name == null || name.isBlank())
            return "tool";
        return name.trim().toLowerCase().replaceAll("[\\s-]+", "_");
    }

    /**
     * 统一错误处理执行工具，与 TS 适配器行为匹配。
     *
     * @param tool    要执行的工具
     * @param context 执行上下文
     * @return 工具结果，不会抛出异常（错误包装在 ToolResult 中）
     */
    public static AgentTool.ToolResult safeExecute(AgentTool tool, AgentTool.ToolContext context) {
        try {
            return tool.execute(context).join();
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : String.valueOf(e);
            log.error("[tools] {} failed: {}", normalizeToolName(tool.getName()), message);
            if (log.isDebugEnabled() && e.getCause() != null) {
                log.debug("tools: {} failed stack:", normalizeToolName(tool.getName()), e.getCause());
            }
            ObjectNode errorPayload = MAPPER.createObjectNode();
            errorPayload.put("status", "error");
            errorPayload.put("tool", normalizeToolName(tool.getName()));
            errorPayload.put("error", message);
            return AgentTool.ToolResult.fail(errorPayload.toPrettyString());
        }
    }
}
