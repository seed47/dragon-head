package org.dragon.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 工具参数 JSON Schema 规范化
 *
 * <p>
 * 规范化工具参数的 JSON Schema 以实现跨 provider 兼容性。
 * 对应 TypeScript 中的 pi-tools.schema.ts。
 * </p>
 *
 * <p>
 * 主要职责：
 * </p>
 * <ul>
 * <li>将联合 schema（anyOf/oneOf）展平为单一的 {@code type: "object"} schema</li>
 * <li>清理 Gemini 不支持的 schema 关键字</li>
 * <li>确保 OpenAI 兼容性的顶层 {@code type: "object"}</li>
 * </ul>
 */
@Slf4j
public final class ToolSchemaAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Gemini 不支持的 JSON Schema 关键字，应被移除。
     */
    private static final Set<String> GEMINI_UNSUPPORTED = Set.of(
            "$schema", "$id", "$ref", "$defs", "definitions",
            "if", "then", "else", "not", "dependentSchemas",
            "dependentRequired", "unevaluatedProperties",
            "unevaluatedItems", "contentMediaType", "contentEncoding",
            "examples", "deprecated", "readOnly", "writeOnly",
            "externalDocs", "xml", "discriminator");

    private ToolSchemaAdapter() {
    }

    /**
     * 规范化工具的参数 schema 以实现跨 provider 兼容性。
     * 处理联合展平和 Gemini 清理。
     *
     * @param schema 原始 schema
     * @return 规范化后的 schema
     */
    public static JsonNode normalizeToolParameters(JsonNode schema) {
        if (schema == null || !schema.isObject()) {
            return schema;
        }
        ObjectNode obj = (ObjectNode) schema.deepCopy();

        // 如果已经是 type:object 且有 properties（没有顶层 anyOf）—— 直接清理
        if (obj.has("type") && obj.has("properties") && !obj.has("anyOf")) {
            return cleanSchemaForGemini(obj);
        }

        // 如果缺少 type 但有对象字段，强制 type:object
        if (!obj.has("type")
                && (obj.has("properties") || obj.has("required"))
                && !obj.has("anyOf") && !obj.has("oneOf")) {
            obj.put("type", "object");
            return cleanSchemaForGemini(obj);
        }

        // 展平 anyOf/oneOf 联合
        String variantKey = obj.has("anyOf") ? "anyOf" : (obj.has("oneOf") ? "oneOf" : null);
        if (variantKey == null) {
            return cleanSchemaForGemini(obj);
        }

        JsonNode variants = obj.get(variantKey);
        if (!variants.isArray()) {
            return cleanSchemaForGemini(obj);
        }

        ObjectNode mergedProperties = MAPPER.createObjectNode();
        Map<String, Integer> requiredCounts = new LinkedHashMap<>();
        int objectVariants = 0;

        for (JsonNode entry : variants) {
            if (!entry.isObject() || !entry.has("properties")) {
                continue;
            }
            objectVariants++;
            ObjectNode props = (ObjectNode) entry.get("properties");
            Iterator<String> fieldNames = props.fieldNames();
            while (fieldNames.hasNext()) {
                String key = fieldNames.next();
                if (!mergedProperties.has(key)) {
                    mergedProperties.set(key, props.get(key).deepCopy());
                } else {
                    mergedProperties.set(key,
                            mergePropertySchemas(mergedProperties.get(key), props.get(key)));
                }
            }
            if (entry.has("required") && entry.get("required").isArray()) {
                for (JsonNode req : entry.get("required")) {
                    if (req.isTextual()) {
                        requiredCounts.merge(req.asText(), 1, Integer::sum);
                    }
                }
            }
        }

        // 构建合并后的 required
        ArrayNode mergedRequired = MAPPER.createArrayNode();
        if (obj.has("required") && obj.get("required").isArray()) {
            for (JsonNode req : obj.get("required")) {
                if (req.isTextual())
                    mergedRequired.add(req.asText());
            }
        } else if (objectVariants > 0) {
            int ov = objectVariants;
            requiredCounts.entrySet().stream()
                    .filter(e -> e.getValue() == ov)
                    .forEach(e -> mergedRequired.add(e.getKey()));
        }

        // 构建展平后的 schema
        ObjectNode result = MAPPER.createObjectNode();
        result.put("type", "object");
        if (obj.has("title"))
            result.set("title", obj.get("title"));
        if (obj.has("description"))
            result.set("description", obj.get("description"));
        if (mergedProperties.size() > 0) {
            result.set("properties", mergedProperties);
        } else if (obj.has("properties")) {
            result.set("properties", obj.get("properties"));
        }
        if (mergedRequired.size() > 0) {
            result.set("required", mergedRequired);
        }
        if (obj.has("additionalProperties")) {
            result.set("additionalProperties", obj.get("additionalProperties"));
        } else {
            result.put("additionalProperties", true);
        }

        return cleanSchemaForGemini(result);
    }

    /**
     * 移除 JSON Schema 中 Gemini 不支持的关键字。
     *
     * @param schema 原始 schema
     * @return 清理后的 schema
     */
    public static JsonNode cleanSchemaForGemini(JsonNode schema) {
        if (schema == null || !schema.isObject()) {
            return schema;
        }
        ObjectNode obj = (ObjectNode) schema.deepCopy();
        GEMINI_UNSUPPORTED.forEach(obj::remove);

        // 递归清理 properties
        if (obj.has("properties") && obj.get("properties").isObject()) {
            ObjectNode props = (ObjectNode) obj.get("properties");
            Iterator<String> names = props.fieldNames();
            List<String> keys = new ArrayList<>();
            names.forEachRemaining(keys::add);
            for (String key : keys) {
                props.set(key, cleanSchemaForGemini(props.get(key)));
            }
        }

        // 递归清理 items
        if (obj.has("items") && obj.get("items").isObject()) {
            obj.set("items", cleanSchemaForGemini(obj.get("items")));
        }

        // 递归清理 anyOf/oneOf/allOf（如果仍然存在）
        for (String arrayKey : List.of("anyOf", "oneOf", "allOf")) {
            if (obj.has(arrayKey) && obj.get(arrayKey).isArray()) {
                ArrayNode cleaned = MAPPER.createArrayNode();
                for (JsonNode item : obj.get(arrayKey)) {
                    cleaned.add(cleanSchemaForGemini(item));
                }
                obj.set(arrayKey, cleaned);
            }
        }

        return obj;
    }

    /**
     * 合并两个属性 schema，组合枚举值。
     *
     * @param existing 现有 schema
     * @param incoming 传入的 schema
     * @return 合并后的 schema
     */
    static JsonNode mergePropertySchemas(JsonNode existing, JsonNode incoming) {
        if (existing == null)
            return incoming;
        if (incoming == null)
            return existing;

        List<String> existingEnum = extractEnumValues(existing);
        List<String> incomingEnum = extractEnumValues(incoming);

        if (existingEnum != null || incomingEnum != null) {
            ObjectNode merged = MAPPER.createObjectNode();
            // 从 existing/incoming 复制元数据
            for (JsonNode source : List.of(existing, incoming)) {
                if (source.isObject()) {
                    for (String metaKey : List.of("title", "description", "default")) {
                        if (!merged.has(metaKey) && source.has(metaKey)) {
                            merged.set(metaKey, source.get(metaKey));
                        }
                    }
                }
            }
            // 合并枚举值
            Set<String> values = new LinkedHashSet<>();
            if (existingEnum != null)
                values.addAll(existingEnum);
            if (incomingEnum != null)
                values.addAll(incomingEnum);

            // 根据值确定类型
            if (!values.isEmpty()) {
                merged.put("type", "string");
            }
            ArrayNode enumArray = MAPPER.createArrayNode();
            values.forEach(enumArray::add);
            merged.set("enum", enumArray);
            return merged;
        }

        return existing;
    }

    /**
     * 从 schema 节点提取枚举值。
     *
     * @param schema schema 节点
     * @return 枚举值列表，如果不存在则返回 null
     */
    static List<String> extractEnumValues(JsonNode schema) {
        if (schema == null || !schema.isObject())
            return null;

        if (schema.has("enum") && schema.get("enum").isArray()) {
            List<String> values = new ArrayList<>();
            for (JsonNode v : schema.get("enum")) {
                values.add(v.asText());
            }
            return values;
        }
        if (schema.has("const")) {
            return List.of(schema.get("const").asText());
        }

        // 检查 anyOf/oneOf 变体
        JsonNode variants = schema.has("anyOf") ? schema.get("anyOf")
                : schema.has("oneOf") ? schema.get("oneOf") : null;
        if (variants != null && variants.isArray()) {
            List<String> values = new ArrayList<>();
            for (JsonNode variant : variants) {
                List<String> extracted = extractEnumValues(variant);
                if (extracted != null)
                    values.addAll(extracted);
            }
            return values.isEmpty() ? null : values;
        }

        return null;
    }
}
