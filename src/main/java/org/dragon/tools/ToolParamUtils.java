package org.dragon.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 工具参数读取工具类
 *
 * <p>
 * 提供从 JSON 输入中读取工具参数的实用方法。
 * 对应 TypeScript 中的 tools/common.ts (readStringParam, readNumberParam, readStringArrayParam, jsonResult)。
 * </p>
 */
public final class ToolParamUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ToolParamUtils() {
    }

    // --- 字符串参数 ---

    /**
     * 从 JSON 参数节点读取字符串参数。
     *
     * @param params   JSON 参数节点
     * @param key      参数键
     * @param required 如果为 true，则缺失时抛出异常
     * @return 去除首尾空白后的字符串值，如果不存在且 required 为 false 则返回 null
     */
    public static String readStringParam(JsonNode params, String key, boolean required) {
        if (params == null || !params.has(key)) {
            if (required)
                throw new IllegalArgumentException(key + " required");
            return null;
        }
        JsonNode node = params.get(key);
        if (!node.isTextual()) {
            if (required)
                throw new IllegalArgumentException(key + " required");
            return null;
        }
        String value = node.asText().trim();
        if (value.isEmpty()) {
            if (required)
                throw new IllegalArgumentException(key + " required");
            return null;
        }
        return value;
    }

    public static String readStringParam(JsonNode params, String key) {
        return readStringParam(params, key, false);
    }

    // --- 数字参数 ---

    /**
     * 读取数字参数（整数或浮点数）。
     *
     * @param params   JSON 参数节点
     * @param key      参数键
     * @param required 如果为 true，则缺失时抛出异常
     * @return 数字值，如果不存在且 required 为 false 则返回 null
     */
    public static Number readNumberParam(JsonNode params, String key, boolean required) {
        if (params == null || !params.has(key)) {
            if (required)
                throw new IllegalArgumentException(key + " required");
            return null;
        }
        JsonNode node = params.get(key);
        if (node.isNumber()) {
            return node.isInt() || node.isLong() ? node.asLong() : node.asDouble();
        }
        if (node.isTextual()) {
            String trimmed = node.asText().trim();
            if (!trimmed.isEmpty()) {
                try {
                    return Double.parseDouble(trimmed);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (required)
            throw new IllegalArgumentException(key + " required");
        return null;
    }

    public static Number readNumberParam(JsonNode params, String key) {
        return readNumberParam(params, key, false);
    }

    /**
     * 读取整数参数，提供默认值。
     *
     * @param params      JSON 参数节点
     * @param key        参数键
     * @param defaultValue 默认值
     * @return 整数值
     */
    public static int readIntParam(JsonNode params, String key, int defaultValue) {
        Number n = readNumberParam(params, key, false);
        return n != null ? n.intValue() : defaultValue;
    }

    // --- 字符串数组参数 ---

    /**
     * 读取字符串数组参数。接受 JSON 数组和单个字符串。
     *
     * @param params   JSON 参数节点
     * @param key      参数键
     * @param required 如果为 true，则缺失时抛出异常
     * @return 字符串列表，如果不存在且 required 为 false 则返回 null
     */
    public static List<String> readStringArrayParam(JsonNode params, String key, boolean required) {
        if (params == null || !params.has(key)) {
            if (required)
                throw new IllegalArgumentException(key + " required");
            return null;
        }
        JsonNode node = params.get(key);
        if (node.isArray()) {
            List<String> values = new ArrayList<>();
            for (JsonNode item : node) {
                if (item.isTextual()) {
                    String v = item.asText().trim();
                    if (!v.isEmpty())
                        values.add(v);
                }
            }
            if (values.isEmpty()) {
                if (required)
                    throw new IllegalArgumentException(key + " required");
                return null;
            }
            return values;
        }
        if (node.isTextual()) {
            String value = node.asText().trim();
            if (value.isEmpty()) {
                if (required)
                    throw new IllegalArgumentException(key + " required");
                return null;
            }
            return List.of(value);
        }
        if (required)
            throw new IllegalArgumentException(key + " required");
        return null;
    }

    public static List<String> readStringArrayParam(JsonNode params, String key) {
        return readStringArrayParam(params, key, false);
    }

    // --- 布尔参数 ---

    /**
     * 读取布尔参数。
     *
     * @param params      JSON 参数节点
     * @param key        参数键
     * @param defaultValue 默认值
     * @return 布尔值
     */
    public static boolean readBoolParam(JsonNode params, String key, boolean defaultValue) {
        if (params == null || !params.has(key))
            return defaultValue;
        JsonNode node = params.get(key);
        if (node.isBoolean())
            return node.asBoolean();
        if (node.isTextual())
            return "true".equalsIgnoreCase(node.asText().trim());
        return defaultValue;
    }

    // --- JSON 结果构建器 ---

    /**
     * 构建标准 JSON 工具结果（对应 TS 的 jsonResult）。
     *
     * @param payload 数据对象
     * @return 工具结果
     */
    public static AgentTool.ToolResult jsonResult(Object payload) {
        try {
            String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
            return AgentTool.ToolResult.ok(json, payload);
        } catch (JsonProcessingException e) {
            return AgentTool.ToolResult.ok(String.valueOf(payload));
        }
    }

    /**
     * 从键值对构建 JSON 结果。
     *
     * @param kvPairs 键值对（交替的键和值）
     * @return 工具结果
     */
    public static AgentTool.ToolResult jsonResult(String... kvPairs) {
        ObjectNode node = MAPPER.createObjectNode();
        for (int i = 0; i + 1 < kvPairs.length; i += 2) {
            node.put(kvPairs[i], kvPairs[i + 1]);
        }
        return AgentTool.ToolResult.ok(node.toPrettyString(), node);
    }

    /**
     * 构建错误 JSON 结果。
     *
     * @param error 错误信息
     * @return 工具结果
     */
    public static AgentTool.ToolResult errorResult(String error) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("status", "error");
        node.put("error", error);
        return AgentTool.ToolResult.fail(node.toPrettyString());
    }

    /**
     * 将对象序列化为格式化的 JSON 字符串。
     *
     * @param value 值对象
     * @return JSON 字符串
     */
    public static String toJsonString(Object value) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    /**
     * 读取整数参数，缺失时返回 null。
     *
     * @param params JSON 参数节点
     * @param key    参数键
     * @return 整数值或 null
     */
    public static Integer readIntegerParam(JsonNode params, String key) {
        Number n = readNumberParam(params, key, false);
        return n != null ? n.intValue() : null;
    }
}
