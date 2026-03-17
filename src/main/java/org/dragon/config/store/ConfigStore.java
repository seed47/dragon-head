package org.dragon.config.store;

import java.util.Map;
import java.util.Optional;

/**
 * 配置存储抽象接口
 * 提供通用的 KV 配置存储能力，支持命名空间和 workspace 维度隔离
 *
 * <p>使用 ConfigKey 统一配置键，支持多种粒度：
 * <pre>
 * // 全局配置
 * configStore.set(ConfigKey.of("app.name"), "DragonHead");
 *
 * // 命名空间配置
 * configStore.set(ConfigKey.of("character", "maxMemory"), 1000);
 *
 * // Character 粒度配置
 * configStore.set(ConfigKey.character("char-1", "feishu.appId"), "xxx");
 *
 * // 完整维度配置
 * configStore.set(ConfigKey.of("default", "character", "char-1", "feishu.appId"), "xxx");
 * </pre>
 */
public interface ConfigStore {

    /**
     * 存储配置值
     *
     * @param configKey 配置键
     * @param value     值（支持任意类型）
     */
    void set(ConfigKey configKey, Object value);

    /**
     * 获取配置值
     *
     * @param configKey 配置键
     * @return Optional 配置值
     */
    Optional<Object> get(ConfigKey configKey);

    /**
     * 获取配置值（带默认值）
     *
     * @param configKey    配置键
     * @param defaultValue 默认值
     * @param <T>          值类型
     * @return 配置值或默认值
     */
    <T> T get(ConfigKey configKey, T defaultValue);

    /**
     * 删除配置
     *
     * @param configKey 配置键
     */
    void delete(ConfigKey configKey);

    /**
     * 检查是否存在
     *
     * @param configKey 配置键
     * @return 是否存在
     */
    boolean exists(ConfigKey configKey);

    /**
     * 获取与 configKey 同维度的所有配置
     *
     * <p>根据 configKey 的维度返回：
     * <ul>
     *   <li>全局粒度 (key only): 返回所有全局配置</li>
     *   <li>命名空间粒度: 返回该命名空间下所有配置</li>
     *   <li>完整维度: 返回该 entityId 下所有配置</li>
     * </ul>
     *
     * @param configKey 带有维度的 ConfigKey（key 可以为 null）
     * @return 配置映射
     */
    Map<String, Object> getAll(ConfigKey configKey);

    /**
     * 删除与 configKey 同维度的所有配置
     *
     * @param configKey 带有维度的 ConfigKey
     */
    void deleteAll(ConfigKey configKey);

    /**
     * 清空所有配置
     */
    void clear();
}
