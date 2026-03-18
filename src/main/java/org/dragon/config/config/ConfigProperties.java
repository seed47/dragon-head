package org.dragon.config.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * 配置模块配置属性
 */
@Data
@ConfigurationProperties(prefix = "dragon.config")
public class ConfigProperties {

    /**
     * 是否启用配置模块
     */
    private boolean enabled = true;

    /**
     * 存储类型: memory, jdbc, redis
     */
    private String type = "memory";

    /**
     * skills配置
     */
    private SkillsConfig skills;

    /**
     * plugins配置
     */
    private PluginsConfig plugins;

    /**
     * skill配置信息
     */
    @Data
    public static class SkillsConfig {
        private Map<String, Object> entries;
        private Object allowBundled;
        private SkillLoadConfig load;
    }

    /**
     * skill加载配置
     */
    @Data
    public static class SkillLoadConfig {
        private Boolean watch;
        private Long watchDebounceMs;
        private List<String> extraDirs;
    }

    /**
     * plugins配置
     */
    @Data
    public static class PluginsConfig {
        private Map<String, PluginEntryConfig> entries;
    }

    /**
     * plugin具体配置信息
     */
    @Data
    public static class PluginEntryConfig {
        private Boolean enabled;
        private Map<String, Object> config;
    }
}