package org.dragon.config.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
}
