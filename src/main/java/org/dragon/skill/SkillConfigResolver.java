package org.dragon.skill;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.dragon.config.config.ConfigProperties;

/**
 * 技能配置解析器。
 * 处理配置路径遍历、资格检查、内置白名单、二进制文件检测等功能。
 *
 * @since 1.0
 */
public final class SkillConfigResolver {

    private SkillConfigResolver() {
    }

    private static final Map<String, Boolean> DEFAULT_CONFIG_VALUES;
    static {
        Map<String, Boolean> map = new HashMap<>();
        map.put("browser.enabled", true);
        map.put("browser.evaluateEnabled", true);
        DEFAULT_CONFIG_VALUES = Collections.unmodifiableMap(map);
    }

    private static final Set<String> BUNDLED_SOURCES = Collections.singleton("bundled");

    // ── 配置路径 (Config path) ─────────────────────────────────────────────────

    /**
     * 通过点分隔的路径解析嵌套的配置值。
     */
    public static Object resolveConfigPath(ConfigProperties config, String pathStr) {
        if (config == null || pathStr == null || pathStr.trim().isEmpty())
            return null;
        String[] parts = pathStr.split("\\.");
        Object current = config;
        for (String part : parts) {
            if (current == null)
                return null;
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                current = getProperty(current, part);
            }
        }
        return current;
    }

    /**
     * 检查配置路径是否解析为真值 (truthy)。
     */
    public static boolean isConfigPathTruthy(ConfigProperties config, String pathStr) {
        Object value = resolveConfigPath(config, pathStr);
        if (value == null && pathStr != null && DEFAULT_CONFIG_VALUES.containsKey(pathStr)) {
            return DEFAULT_CONFIG_VALUES.get(pathStr);
        }
        return isTruthy(value);
    }

    // ── 技能配置 (Skill config) ────────────────────────────────────────────────

    /**
     * 从 DragonHead 配置中解析特定技能的配置。
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> resolveSkillConfig(ConfigProperties config, String skillKey) {
        if (config == null || config.getSkills() == null)
            return null;
        Object entries = config.getSkills().getEntries();
        if (entries instanceof Map) {
            Object entry = ((Map<?, ?>) entries).get(skillKey);
            if (entry instanceof Map)
                return (Map<String, Object>) entry;
        }
        return null;
    }

    // ── 内置白名单 (Bundled allowlist) ───────────────────────────────────────────

    /**
     * 从配置中解析内置技能白名单。
     */
    public static List<String> resolveBundledAllowlist(ConfigProperties config) {
        if (config == null || config.getSkills() == null)
            return null;
        Object raw = config.getSkills().getAllowBundled();
        return normalizeAllowlist(raw);
    }

    public static boolean isBundledSkillAllowed(SkillTypes.SkillEntry entry, List<String> allowlist) {
        if (allowlist == null || allowlist.isEmpty())
            return true;
        if (!isBundledSkill(entry))
            return true;
        String key = SkillFrontmatterParser.resolveSkillKey(entry.getSkill(), entry);
        return allowlist.contains(key) || allowlist.contains(entry.getSkill().getName());
    }

    // ── 二进制检测 (Binary detection) ────────────────────────────────────────────

    /**
     * 检查 PATH 中是否存在某个二进制可执行文件。
     */
    public static boolean hasBinary(String bin) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null || pathEnv.trim().isEmpty())
            return false;
        for (String dir : pathEnv.split(File.pathSeparator)) {
            Path candidate = Paths.get(dir, bin);
            if (Files.isExecutable(candidate))
                return true;
        }
        return false;
    }

    /**
     * 解析运行时平台字符串 (darwin/win32/linux)。
     */
    public static String resolveRuntimePlatform() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac") || os.contains("darwin"))
            return "darwin";
        if (os.contains("win"))
            return "win32";
        return "linux";
    }

    // ── 资格检查 (Eligibility check) ───────────────────────────────────────────

    /**
     * 根据配置、操作系统、依赖要求确定是否应包含某个技能。
     */
    public static boolean shouldIncludeSkill(SkillTypes.SkillEntry entry, ConfigProperties config) {
        return shouldIncludeSkill(entry, config, null);
    }

    /**
     * 包含远程上下文的完整资格检查。
     */
    @SuppressWarnings("unchecked")
    public static boolean shouldIncludeSkill(
            SkillTypes.SkillEntry entry, ConfigProperties config, SkillTypes.SkillEligibilityContext eligibility) {

        String skillKey = SkillFrontmatterParser.resolveSkillKey(entry.getSkill(), entry);

        // 被配置禁用
        if (config != null) {
            Map<String, Object> skillConfig = resolveSkillConfig(config, skillKey);
            if (skillConfig != null && Boolean.FALSE.equals(skillConfig.get("enabled"))) {
                return false;
            }
            List<String> allowBundled = resolveBundledAllowlist(config);
            if (!isBundledSkillAllowed(entry, allowBundled)) {
                return false;
            }
        }

        // 操作系统检查
        List<String> osList = entry.getMetadata() != null ? entry.getMetadata().getOs() : null;
        List<String> remotePlatforms = eligibility != null && eligibility.getRemote() != null
                ? eligibility.getRemote().getPlatforms()
                : Collections.emptyList();
        if (osList != null && !osList.isEmpty()) {
            boolean localMatch = osList.contains(resolveRuntimePlatform());
            boolean remoteMatch = remotePlatforms.stream().anyMatch(osList::contains);
            if (!localMatch && !remoteMatch)
                return false;
        }

        // Always 标志
        if (entry.getMetadata() != null && Boolean.TRUE.equals(entry.getMetadata().getAlways())) {
            return true;
        }

        // 必需的二进制文件
        if (entry.getMetadata() != null && entry.getMetadata().getRequires() != null) {
            SkillTypes.SkillRequires req = entry.getMetadata().getRequires();

            if (req.getBins() != null && !req.getBins().isEmpty()) {
                for (String bin : req.getBins()) {
                    if (hasBinary(bin))
                        continue;
                    if (eligibility != null && eligibility.getRemote() != null
                            && eligibility.getRemote().getHasBin() != null
                            && eligibility.getRemote().getHasBin().test(bin))
                        continue;
                    return false;
                }
            }

            if (req.getAnyBins() != null && !req.getAnyBins().isEmpty()) {
                boolean localAny = req.getAnyBins().stream().anyMatch(SkillConfigResolver::hasBinary);
                boolean remoteAny = eligibility != null && eligibility.getRemote() != null
                        && eligibility.getRemote().getHasAnyBin() != null
                        && Boolean.TRUE.equals(eligibility.getRemote().getHasAnyBin().apply(req.getAnyBins()));
                if (!localAny && !remoteAny)
                    return false;
            }

            // 必需的环境变量
            if (req.getEnv() != null && !req.getEnv().isEmpty()) {
                for (String envName : req.getEnv()) {
                    String envValue = System.getenv(envName);
                    if (envValue != null && !envValue.trim().isEmpty())
                        continue;
                    if (config != null) {
                        Map<String, Object> sc = resolveSkillConfig(config, skillKey);
                        if (sc != null) {
                            Map<String, String> envMap = (Map<String, String>) sc.get("env");
                            if (envMap != null && envMap.containsKey(envName))
                                continue;
                            if (entry.getMetadata().getPrimaryEnv() != null
                                    && entry.getMetadata().getPrimaryEnv().equals(envName)
                                    && sc.containsKey("apiKey"))
                                continue;
                        }
                    }
                    return false;
                }
            }

            // 必需的配置路径
            if (req.getConfig() != null && !req.getConfig().isEmpty()) {
                for (String configPath : req.getConfig()) {
                    if (!isConfigPathTruthy(config, configPath))
                        return false;
                }
            }
        }

        return true;
    }

    // ── 辅助方法 (Helpers) ─────────────────────────────────────────────────────

    private static boolean isBundledSkill(SkillTypes.SkillEntry entry) {
        return entry.getSkill().getSource() != null
                && BUNDLED_SOURCES.contains(entry.getSkill().getSource().label());
    }

    @SuppressWarnings("unchecked")
    private static List<String> normalizeAllowlist(Object input) {
        if (input == null)
            return null;
        if (input instanceof List) {
            List<String> normalized = ((List<?>) input).stream()
                    .map(e -> String.valueOf(e).trim())
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            return normalized.isEmpty() ? null : normalized;
        }
        return null;
    }

    private static boolean isTruthy(Object value) {
        if (value == null)
            return false;
        if (value instanceof Boolean)
            return (Boolean) value;
        if (value instanceof Number)
            return ((Number) value).doubleValue() != 0;
        if (value instanceof String)
            return !((String) value).trim().isEmpty();
        return true;
    }

    private static Object getProperty(Object obj, String name) {
        try {
            Method method = obj.getClass().getMethod("get" + capitalize(name));
            return method.invoke(obj);
        } catch (Exception e1) {
            try {
                Method method = obj.getClass().getMethod("is" + capitalize(name));
                return method.invoke(obj);
            } catch (Exception e2) {
                try {
                    Field field = obj.getClass().getDeclaredField(name);
                    field.setAccessible(true);
                    return field.get(obj);
                } catch (Exception e3) {
                    return null;
                }
            }
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty())
            return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
