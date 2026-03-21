package org.dragon.skill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 解析 SKILL.md 文件中类似 YAML 的 frontmatter 并提取元数据。
 * 对应 TypeScript 中的 skills/frontmatter.ts。
 */
@Slf4j
public class SkillFrontmatterParser {

    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * 用于提取文件开头 --- 分隔符之间的 frontmatter 块的正则表达式。
     * 匹配：---\nkey: value\nkey: value\n---
     */
    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile("\\A---\s*\n(.*?)\n---\s*\n?",
            Pattern.DOTALL);

    /**
     * 用于 frontmatter 行的简单 key: value 模式。
     */
    private static final Pattern KV_PATTERN = Pattern.compile("^([a-zA-Z][a-zA-Z0-9_-]*)\s*:\s*(.*)$");

    // =========================================================================
    // Frontmatter 解析 (Frontmatter parsing)
    // =========================================================================

    /**
     * 从 SKILL.md 内容中解析类似 YAML 的 frontmatter。
     *
     * @param content 完整的 SKILL.md 文件内容
     * @return frontmatter 中的键值对映射；如果未找到则返回空映射
     */
    public static Map<String, String> parseFrontmatter(String content) {
        if (content == null || content.trim().isEmpty()) {
            return Collections.emptyMap();
        }

        Matcher m = FRONTMATTER_PATTERN.matcher(content);
        if (!m.find()) {
            return Collections.emptyMap();
        }

        String block = m.group(1);
        Map<String, String> result = new LinkedHashMap<>();
        StringBuilder currentKey = null;
        StringBuilder currentValue = null;

        for (String line : block.split("\\n")) {
            Matcher kv = KV_PATTERN.matcher(line);
            if (kv.matches()) {
                // 刷新上一个键
                if (currentKey != null && currentValue != null) {
                    result.put(currentKey.toString(), currentValue.toString().trim());
                }
                currentKey = new StringBuilder(kv.group(1));
                currentValue = new StringBuilder(kv.group(2));
            } else if (currentKey != null && currentValue != null) {
                // 续行（用于多行值）
                currentValue.append("\n").append(line);
            }
        }

        // 刷新最后一个键
        if (currentKey != null && currentValue != null) {
            result.put(currentKey.toString(), currentValue.toString().trim());
        }

        return result;
    }

    /**
     * 从 SKILL.md 中提取正文内容（frontmatter 之后的部分）。
     */
    public static String extractBody(String content) {
        if (content == null || content.trim().isEmpty())
            return "";
        Matcher m = FRONTMATTER_PATTERN.matcher(content);
        if (m.find()) {
            return content.substring(m.end()).trim();
        }
        return content.trim();
    }

    // =========================================================================
    // 元数据解析 (Metadata resolution)
    // =========================================================================

    /**
     * 从解析后的 frontmatter 中解析 OpenClaw 专属元数据。
     * 查找包含 "dragonhead" 子键的 JSON 对象的 "metadata" 键。
     *
     * @param frontmatter 解析后的 frontmatter 映射
     * @return 解析后的元数据，如果不存在则返回 null
     */
    public static SkillTypes.SkillMetadata resolveMetadata(Map<String, String> frontmatter) {
        String raw = frontmatter.get("metadata");
        if (raw == null || raw.trim().isEmpty())
            return null;

        try {
            JsonNode root = JSON.readTree(raw);
            if (root == null || !root.isObject())
                return null;

            // 查找 dragonhead的skill 元数据（尝试多个键以保持兼容性）
            JsonNode meta = findMetadataNode(root);
            if (meta == null || !meta.isObject())
                return null;

            return new SkillTypes.SkillMetadata(
                    meta.has("always") ? meta.get("always").asBoolean() : null,
                    textOrNull(meta, "skillKey"),
                    textOrNull(meta, "primaryEnv"),
                    textOrNull(meta, "emoji"),
                    textOrNull(meta, "homepage"),
                    stringList(meta, "os"),
                    resolveRequires(meta),
                    null);
        } catch (Exception e) {
            log.debug("解析技能元数据失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 frontmatter 解析调用策略。
     */
    public static SkillTypes.SkillInvocationPolicy resolveInvocationPolicy(
            Map<String, String> frontmatter) {
        boolean userInvocable = parseBool(frontmatter.get("user-invocable"), true);
        boolean disableModelInvocation = parseBool(
                frontmatter.get("disable-model-invocation"), false);
        return new SkillTypes.SkillInvocationPolicy(userInvocable, disableModelInvocation);
    }

    /**
     * 从技能名称和条目中解析技能键（标识符）。
     */
    public static String resolveSkillKey(SkillTypes.Skill skill, SkillTypes.SkillEntry entry) {
        if (entry != null && entry.getMetadata() != null
                && entry.getMetadata().getSkillKey() != null) {
            return entry.getMetadata().getSkillKey();
        }
        return skill.getName();
    }

    // =========================================================================
    // 辅助方法 (Helpers)
    // =========================================================================

    private static JsonNode findMetadataNode(JsonNode root) {
        // metadata: { "dragonhead": { "always": true, "emoji": "🧪", "os": ["darwin", "linux"] } }
        for (String key : Arrays.asList("dragonhead")) {
            JsonNode node = root.get(key);
            if (node != null && node.isObject())
                return node;
        }
        return null;
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        return (child != null && child.isTextual()) ? child.asText() : null;
    }

    private static List<String> stringList(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null)
            return Collections.emptyList();
        if (child.isArray()) {
            List<String> result = new ArrayList<>();
            child.forEach(el -> {
                if (el.isTextual())
                    result.add(el.asText().trim());
            });
            return result;
        }
        if (child.isTextual()) {
            return Arrays.stream(child.asText().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private static SkillTypes.SkillRequires resolveRequires(JsonNode meta) {
        JsonNode req = meta.get("requires");
        if (req == null || !req.isObject())
            return null;
        return new SkillTypes.SkillRequires(
                stringList(req, "bins"),
                stringList(req, "anyBins"),
                stringList(req, "env"),
                stringList(req, "config"));
    }

    private static boolean parseBool(String value, boolean fallback) {
        if (value == null || value.trim().isEmpty())
            return fallback;
        String v = value.trim().toLowerCase();
        switch (v) {
            case "true":
            case "yes":
            case "1":
            case "on":
                return true;
            case "false":
            case "no":
            case "0":
            case "off":
                return false;
            default:
                return fallback;
        }
    }
}
