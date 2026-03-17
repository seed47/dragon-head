package org.dragon.config.store;

import java.util.Objects;

/**
 * 配置键
 * 支持 4 个维度的粒度设置：workspace -> entityType -> entityId -> key
 *
 * <p>使用示例：
 * <pre>
 * // 全局配置
 * ConfigKey.of("app.name")
 *
 * // 命名空间配置
 * ConfigKey.of("character", "maxMemory")
 *
 * // Character 粒度配置
 * ConfigKey.character("char-1", "feishu.appId")
 *
 * // 完整维度配置
 * ConfigKey.of("default", "character", "char-1", "feishu.appId")
 * </pre>
 */
public final class ConfigKey {

    private final String workspace;
    private final String entityType;
    private final String entityId;
    private final String key;

    private ConfigKey(String workspace, String entityType, String entityId, String key) {
        this.workspace = workspace;
        this.entityType = entityType;
        this.entityId = entityId;
        this.key = key;
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 全局粒度：只设置 key
     */
    public static ConfigKey of(String key) {
        return new ConfigKey(null, null, null, key);
    }

    /**
     * 命名空间粒度：namespace + key
     * (使用 entityId 作为 namespace 字段)
     */
    public static ConfigKey of(String namespace, String key) {
        return new ConfigKey(null, null, namespace, key);
    }

    /**
     * Workspace 粒度：workspace + entityType + entityId + key
     */
    public static ConfigKey of(String workspace, String entityType, String entityId, String key) {
        return new ConfigKey(workspace, entityType, entityId, key);
    }

    // ==================== 便捷工厂方法 ====================

    /**
     * Character 粒度配置 (使用默认 workspace)
     */
    public static ConfigKey character(String characterId, String key) {
        return new ConfigKey("default", "character", characterId, key);
    }

    /**
     * Character 粒度配置 (指定 workspace)
     */
    public static ConfigKey character(String workspace, String characterId, String key) {
        return new ConfigKey(workspace, "character", characterId, key);
    }

    /**
     * Model 粒度配置 (使用默认 workspace)
     */
    public static ConfigKey model(String modelId, String key) {
        return new ConfigKey("default", "model", modelId, key);
    }

    /**
     * Model 粒度配置 (指定 workspace)
     */
    public static ConfigKey model(String workspace, String modelId, String key) {
        return new ConfigKey(workspace, "model", modelId, key);
    }

    /**
     * Channel 粒度配置
     */
    public static ConfigKey channel(String channelId, String key) {
        return new ConfigKey("default", "channel", channelId, key);
    }

    // ==================== 粒度判断 ====================

    /**
     * 是否为全局粒度 (只有 key)
     */
    public boolean isGlobal() {
        return workspace == null && entityType == null && entityId == null && key != null;
    }

    /**
     * 是否为命名空间粒度 (entityId 作为 namespace)
     */
    public boolean isNamespace() {
        return workspace == null && entityType == null && entityId != null && key != null;
    }

    /**
     * 是否为完整维度粒度
     */
    public boolean isFull() {
        return workspace != null && entityType != null && entityId != null && key != null;
    }

    // ==================== Getter ====================

    public String getWorkspace() {
        return workspace;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getKey() {
        return key;
    }

    // ==================== Object 方法 ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigKey configKey = (ConfigKey) o;
        return Objects.equals(workspace, configKey.workspace)
                && Objects.equals(entityType, configKey.entityType)
                && Objects.equals(entityId, configKey.entityId)
                && Objects.equals(key, configKey.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workspace, entityType, entityId, key);
    }

    @Override
    public String toString() {
        return "ConfigKey{" +
                "workspace='" + workspace + '\'' +
                ", entityType='" + entityType + '\'' +
                ", entityId='" + entityId + '\'' +
                ", key='" + key + '\'' +
                '}';
    }
}
