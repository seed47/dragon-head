package org.dragon.config.config;

import org.dragon.config.store.ConfigStore;
import org.dragon.config.store.MemoryConfigStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 配置模块自动配置类
 */
@AutoConfiguration
@EnableConfigurationProperties(ConfigProperties.class)
@ConditionalOnProperty(prefix = "dragon.config", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ConfigAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ConfigStore.class)
    @ConditionalOnProperty(prefix = "dragon.config", name = "type", havingValue = "memory", matchIfMissing = true)
    public ConfigStore memoryConfigStore() {
        return new MemoryConfigStore();
    }
}
