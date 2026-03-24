package org.dragon.skill;

import org.dragon.config.config.ConfigProperties;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 插件技能解析器。
 * 解析由插件提供的技能目录。
 *
 * @since 1.0
 */
@Slf4j
public final class SkillPluginResolver {

    private SkillPluginResolver() {
    }

    /**
     * 插件清单记录。
     * 用于技能解析的最小插件信息，包含插件ID、来源、类型、根目录和技能列表。
     */
    public static class PluginRecord {
        private final String id;
        private final String origin; // "workspace" | "global"
        private final String kind; // "memory" | "tool" | 等等
        private final String rootDir;
        private final List<String> skills;
        private final boolean enabled;

        public PluginRecord(String id, String origin, String kind, String rootDir, List<String> skills, boolean enabled) {
            this.id = id;
            this.origin = origin;
            this.kind = kind;
            this.rootDir = rootDir;
            this.skills = skills;
            this.enabled = enabled;
        }

        public String getId() {
            return id;
        }

        public String getOrigin() {
            return origin;
        }

        public String getKind() {
            return kind;
        }

        public String getRootDir() {
            return rootDir;
        }

        public List<String> getSkills() {
            return skills;
        }

        public boolean isEnabled() {
            return enabled;
        }
    }

    /**
     * 解析工作区的插件技能目录。
     * 读取插件清单，检查启用状态，并返回插件技能目录的绝对路径。
     *
     * @param workspaceDir 工作区根目录
     * @param config       可选配置，用于插件启用/禁用覆盖
     * @return 解析后的插件技能目录路径列表
     */
    public static List<String> resolvePluginSkillDirs(String workspaceDir, ConfigProperties config) {
        if (workspaceDir == null || workspaceDir.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<PluginRecord> plugins = loadPluginManifests(workspaceDir, config);
        if (plugins.isEmpty())
            return Collections.emptyList();

        Set<String> seen = new LinkedHashSet<>();
        List<String> resolved = new ArrayList<>();

        for (PluginRecord record : plugins) {
            if (record.getSkills() == null || record.getSkills().isEmpty())
                continue;
            if (!record.isEnabled())
                continue;

            for (String raw : record.getSkills()) {
                String trimmed = raw.trim();
                if (trimmed.isEmpty())
                    continue;

                Path candidate = Paths.get(record.getRootDir()).resolve(trimmed).toAbsolutePath();
                if (!Files.exists(candidate)) {
                    log.warn("未找到插件技能路径 ({}): {}", record.getId(), candidate);
                    continue;
                }

                String canonical = candidate.toString();
                if (seen.contains(canonical))
                    continue;
                seen.add(canonical);
                resolved.add(canonical);
            }
        }

        return resolved;
    }

    // ── 插件清单加载（简化版） ────────────────────────

    /**
     * 从工作区和全局配置目录加载插件清单。
     * 这是一个简化版本 — 完整实现将使用插件子系统中的 PluginManifestRegistry。
     */
    private static List<PluginRecord> loadPluginManifests(
            String workspaceDir, ConfigProperties config) {
        List<PluginRecord> records = new ArrayList<>();

        // 工作区插件
        Path workspacePluginsDir = Paths.get(workspaceDir, "plugins");
        scanPluginDir(workspacePluginsDir, "workspace", records, config);

        // 全局插件
        String configDir = System.getProperty("user.home") + "/.dragonhead";
        Path globalPluginsDir = Paths.get(configDir, "plugins");
        scanPluginDir(globalPluginsDir, "global", records, config);

        return records;
    }

    private static void scanPluginDir(Path pluginsDir, String origin,
            List<PluginRecord> records,
            ConfigProperties config) {
        if (!Files.isDirectory(pluginsDir))
            return;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginsDir)) {
            for (Path child : stream) {
                if (!Files.isDirectory(child))
                    continue;
                Path manifest = child.resolve("manifest.json");
                if (!Files.isRegularFile(manifest))
                    continue;

                try {
                    String content = new String(Files.readAllBytes(manifest), StandardCharsets.UTF_8);
                    // 简单的 JSON 解析，用于 id、kind、skills 字段
                    String id = extractJsonString(content, "id");
                    String kind = extractJsonString(content, "kind");
                    List<String> skills = extractJsonStringArray(content, "skills");

                    if (id == null || id.trim().isEmpty())
                        continue;

                    boolean enabled = isPluginEnabled(id, config);
                    records.add(new PluginRecord(
                            id, origin, kind,
                            child.toAbsolutePath().toString(),
                            skills, enabled));
                } catch (Exception e) {
                    log.debug("读取插件清单失败 {}: {}", manifest, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("扫描插件目录失败 {}: {}", pluginsDir, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean isPluginEnabled(String pluginId, ConfigProperties config) {
        if (config == null || config.getPlugins() == null)
            return true;
        Object pluginsConfig = config.getPlugins();
        if (pluginsConfig instanceof Map) {
            Object entry = ((Map<String, Object>) pluginsConfig).get(pluginId);
            if (entry instanceof Map) {
                Object enabled = ((Map<?, ?>) entry).get("enabled");
                if (enabled instanceof Boolean)
                    return (Boolean) enabled;
            }
        }
        return true;
    }

    // ── 简单的 JSON 辅助方法（避免在此处引入完整的 Jackson 依赖） ─────

    private static String extractJsonString(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        Matcher matcher = Pattern.compile(pattern).matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static List<String> extractJsonStringArray(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\\[([^\\]]*)]";
        Matcher matcher = Pattern.compile(pattern).matcher(json);
        if (!matcher.find())
            return Collections.emptyList();
        String array = matcher.group(1);
        List<String> result = new ArrayList<>();
        Matcher itemMatcher = Pattern.compile("\"([^\"]+)\"").matcher(array);
        while (itemMatcher.find()) {
            result.add(itemMatcher.group(1));
        }
        return result;
    }
}
