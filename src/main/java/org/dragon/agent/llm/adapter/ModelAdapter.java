package org.dragon.agent.llm.adapter;

import org.dragon.agent.llm.LLMRequest;
import org.dragon.agent.llm.LLMResponse;

import java.util.stream.Stream;

/**
 * 模型适配器接口
 * 负责将统一请求转换为厂商特有格式，将厂商响应转换为统一格式
 *
 * @author wyj
 * @version 1.0
 */
public interface ModelAdapter {

    /**
     * 获取支持的提供商
     *
     * @return 提供商类型
     */
    String getProvider();

    /**
     * 将统一请求转换为厂商特有格式
     *
     * @param request 统一请求
     * @return 厂商特有请求对象
     */
    Object toProviderRequest(LLMRequest request);

    /**
     * 将厂商响应转换为统一格式
     *
     * @param providerResponse 厂商响应
     * @return 统一响应
     */
    LLMResponse fromProviderResponse(Object providerResponse);

    /**
     * 将统一请求转换为厂商流式请求格式
     *
     * @param request 统一请求
     * @return 厂商流式请求对象
     */
    Object toProviderStreamRequest(LLMRequest request);

    /**
     * 将厂商流式响应转换为统一格式
     *
     * @param providerStream 厂商流式响应
     * @return 统一流式响应
     */
    Stream<LLMResponse> fromProviderStream(Object providerStream);
}
