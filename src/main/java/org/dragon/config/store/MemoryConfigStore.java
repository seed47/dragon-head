package org.dragon.config.store;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 内存配置存储实现
 * 使用 ConcurrentHashMap 存储，支持命名空间和 workspace 维度配置
 *
 * <p>存储结构：使用组合键 "workspace|entityType|entityId|key" 作为单层 Map 的 key
 */
@Slf4j
public class MemoryConfigStore implements ConfigStore {

    /**
     * 存储结构: compositeKey -> value
     * compositeKey 格式: "workspace|entityType|entityId|key"
     * 空值用空字符串表示
     */
    private final ConcurrentMap<String, Object> store = new ConcurrentHashMap<>();

    @Override
    public void set(ConfigKey configKey, Object value) {
        if (configKey == null || configKey.getKey() == null) {
            throw new IllegalArgumentException("configKey and key cannot be null");
        }
        String compositeKey = buildKey(configKey);
        store.put(compositeKey, value);
        log.debug("Config set: {} = {}", configKey, value);
    }

    @Override
    public Optional<Object> get(ConfigKey configKey) {
        if (configKey == null || configKey.getKey() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(buildKey(configKey)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(ConfigKey configKey, T defaultValue) {
        return (T) get(configKey).orElse(defaultValue);
    }

    @Override
    public void delete(ConfigKey configKey) {
        if (configKey == null || configKey.getKey() == null) {
            return;
        }
        store.remove(buildKey(configKey));
        log.debug("Config deleted: {}", configKey);
    }

    @Override
    public boolean exists(ConfigKey configKey) {
        if (configKey == null || configKey.getKey() == null) {
            return false;
        }
        return store.containsKey(buildKey(configKey));
    }

    @Override
    public Map<String, Object> getAll(ConfigKey configKey) {
        if (configKey == null) {
            return Collections.emptyMap();
        }

        String prefix = buildPrefix(configKey);
        Map<String, Object> result = new HashMap<>();

        store.forEach((key, value) -> {
            if (key.startsWith(prefix)) {
                String suffix = key.substring(prefix.length());
                // 提取最后一个部分作为配置键
                String[] parts = suffix.split("\\|", 2);
                if (parts.length > 1) {
                    result.put(parts[1], value);
                }
            }
        });

        return result;
    }

    @Override
    public void deleteAll(ConfigKey configKey) {
        if (configKey == null) {
            return;
        }

        String prefix = buildPrefix(configKey);
        List<String> keysToRemove = new ArrayList<>();

        store.forEach((key, value) -> {
            if (key.startsWith(prefix)) {
                keysToRemove.add(key);
            }
        });

        keysToRemove.forEach(store::remove);
        log.debug("Config deleted all with prefix: {}", prefix);
    }

    @Override
    public void clear() {
        store.clear();
        log.info("All config cleared");
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建组合键
     * 格式: workspace|entityType|entityId|key
     * 空值用空字符串表示
     */
    private String buildKey(ConfigKey configKey) {
        String workspace = configKey.getWorkspace() != null ? configKey.getWorkspace() : "";
        String entityType = configKey.getEntityType() != null ? configKey.getEntityType() : "";
        String entityId = configKey.getEntityId() != null ? configKey.getEntityId() : "";
        String key = configKey.getKey() != null ? configKey.getKey() : "";
        return workspace + "|" + entityType + "|" + entityId + "|" + key;
    }

    /**
     * 构建前缀（用于批量查询和删除）
     */
    private String buildPrefix(ConfigKey configKey) {
        String workspace = configKey.getWorkspace() != null ? configKey.getWorkspace() : "";
        String entityType = configKey.getEntityType() != null ? configKey.getEntityType() : "";
        String entityId = configKey.getEntityId() != null ? configKey.getEntityId() : "";
        return workspace + "|" + entityType + "|" + entityId + "|";
    }
}
