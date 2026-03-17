package org.dragon.agent.llm.transformer;

import org.dragon.agent.llm.LLMResponse;

import java.util.stream.Stream;

/**
 * 响应转换器接口
 * 负责将厂商返回的原始响应转换为框架内部统一的消息格式
 *
 * @author wyj
 * @version 1.0
 */
public interface ResponseTransformer {

    /**
     * 将原始响应转换为统一格式
     *
     * @param rawResponse 原始响应
     * @return 统一响应
     */
    LLMResponse transform(Object rawResponse);

    /**
     * 将流式响应转换为统一格式
     *
     * @param providerStream 原始流式响应
     * @return 统一流式响应
     */
    Stream<LLMResponse> transformStream(Object providerStream);
}
