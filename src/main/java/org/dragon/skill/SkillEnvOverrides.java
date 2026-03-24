package org.dragon.skill;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dragon.config.config.ConfigProperties;

/**
 * 技能环境变量覆盖管理器。
 * 应用来自技能配置的环境变量覆盖，支持恢复原始环境变量。
 *
 * @since 1.0
 */
public final class SkillEnvOverrides {

    private SkillEnvOverrides() {
    }

    /**
     * 环境变量更新记录。
     * 记录单个环境变量覆盖的原始值，以便后续恢复。
     */
    private static class EnvUpdate {
        private final String key;
        private final String previousValue;

        public EnvUpdate(String key, String previousValue) {
            this.key = key;
            this.previousValue = previousValue;
        }

        public String getKey() {
            return key;
        }

        public String getPreviousValue() {
            return previousValue;
        }
    }

    /**
     * 为激活的技能应用环境变量覆盖。
     * 返回一个 Runnable，调用时会恢复所有覆盖。
     */
    @SuppressWarnings("unchecked")
    public static Runnable applySkillEnvOverrides(List<SkillTypes.SkillEntry> skills, ConfigProperties config) {
        List<EnvUpdate> updates = new ArrayList<>();

        for (SkillTypes.SkillEntry entry : skills) {
            String skillKey = SkillFrontmatterParser.resolveSkillKey(entry.getSkill(), entry);
            Map<String, Object> skillConfig = SkillConfigResolver.resolveSkillConfig(config, skillKey);
            if (skillConfig == null)
                continue;

            // 特定技能的环境变量覆盖
            Object envObj = skillConfig.get("env");
            if (envObj instanceof Map) {
                Map<String, String> envMap = (Map<String, String>) envObj;
                for (Map.Entry<String, String> e : envMap.entrySet()) {
                    String envKey = e.getKey();
                    String envValue = e.getValue();
                    if (envValue == null || envValue.trim().isEmpty())
                        continue;
                    if (System.getenv(envKey) != null)
                        continue;
                    updates.add(new EnvUpdate(envKey, System.getenv(envKey)));
                    setEnv(envKey, envValue);
                }
            }

            // 主环境变量 / apiKey 映射
            String primaryEnv = entry.getMetadata() != null ? entry.getMetadata().getPrimaryEnv() : null;
            Object apiKey = skillConfig.get("apiKey");
            if (primaryEnv != null && apiKey instanceof String) {
                String key = (String) apiKey;
                if (!key.trim().isEmpty() && System.getenv(primaryEnv) == null) {
                    updates.add(new EnvUpdate(primaryEnv, System.getenv(primaryEnv)));
                    setEnv(primaryEnv, key);
                }
            }
        }

        // 返回恢复函数
        return () -> {
            for (EnvUpdate update : updates) {
                if (update.getPreviousValue() == null) {
                    clearEnv(update.getKey());
                } else {
                    setEnv(update.getKey(), update.getPreviousValue());
                }
            }
        };
    }

    /**
     * 从技能快照（仅摘要）应用环境变量覆盖。
     */
    @SuppressWarnings("unchecked")
    public static Runnable applySkillEnvOverridesFromSnapshot(
            SkillTypes.SkillSnapshot snapshot, ConfigProperties config) {
        if (snapshot == null)
            return () -> {
            };
        List<EnvUpdate> updates = new ArrayList<>();

        for (SkillTypes.SkillSummary skill : snapshot.getSkills()) {
            Map<String, Object> skillConfig = SkillConfigResolver.resolveSkillConfig(config, skill.getName());
            if (skillConfig == null)
                continue;

            Object envObj = skillConfig.get("env");
            if (envObj instanceof Map) {
                Map<String, String> envMap = (Map<String, String>) envObj;
                for (Map.Entry<String, String> e : envMap.entrySet()) {
                    String envKey = e.getKey();
                    String envValue = e.getValue();
                    if (envValue == null || envValue.trim().isEmpty())
                        continue;
                    if (System.getenv(envKey) != null)
                        continue;
                    updates.add(new EnvUpdate(envKey, System.getenv(envKey)));
                    setEnv(envKey, envValue);
                }
            }

            Object apiKey = skillConfig.get("apiKey");
            if (skill.getPrimaryEnv() != null && apiKey instanceof String) {
                String key = (String) apiKey;
                if (!key.trim().isEmpty() && System.getenv(skill.getPrimaryEnv()) == null) {
                    updates.add(new EnvUpdate(skill.getPrimaryEnv(), System.getenv(skill.getPrimaryEnv())));
                    setEnv(skill.getPrimaryEnv(), key);
                }
            }
        }

        return () -> {
            for (EnvUpdate update : updates) {
                if (update.getPreviousValue() == null) {
                    clearEnv(update.getKey());
                } else {
                    setEnv(update.getKey(), update.getPreviousValue());
                }
            }
        };
    }

    // ── JVM 环境变量操作（通过系统属性作为后备） ─────────

    /**
     * 设置环境变量。在 Java 中，{@code System.getenv()} 是只读的，
     * 因此我们将覆盖存储在系统属性中作为后备。
     */
    private static void setEnv(String key, String value) {
        System.setProperty("dragonhead.env." + key, value);
    }

    private static void clearEnv(String key) {
        System.clearProperty("dragonhead.env." + key);
    }

    /**
     * 考虑覆盖的情况下检索环境变量值。
     */
    public static String getEnvWithOverrides(String key) {
        String override = System.getProperty("dragonhead.env." + key);
        return override != null ? override : System.getenv(key);
    }
}
