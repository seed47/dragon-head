package org.dragon.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 模型实例
 * 代表一个可用的 LLM 模型配置
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelInstance {

    /**
     * 模型实例 ID
     */
    private String id;

    /**
     * 模型提供商
     */
    private ModelProvider provider;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * API 端点
     */
    private String endpoint;

    /**
     * 认证信息 (API Key 等)
     */
    private Map<String, String> credentials;

    /**
     * 默认参数 (temperature, max_tokens 等)
     */
    private Map<String, Object> defaultParams;

    /**
     * 是否启用
     */
    private boolean enabled;

    /**
     * 描述
     */
    private String description;

    /**
     * 优先级 (数字越大优先级越高)
     */
    private int priority;

    /**
     * 模型提供商枚举
     */
    public enum ModelProvider {
        OPENAI,
        ANTHROPIC,
        BAIDU_WENXIN,
        ALIBABA_TONGYI,
        ZHIPU,
        MOONSHOT,
        CUSTOM
    }
}
