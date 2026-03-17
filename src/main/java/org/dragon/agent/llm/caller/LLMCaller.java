package org.dragon.agent.llm.caller;

import org.dragon.agent.llm.LLMRequest;
import org.dragon.agent.llm.LLMResponse;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * LLM 调用器接口
 * 负责与各厂商 LLM 的交互
 *
 * @author wyj
 * @version 1.0
 */
public interface LLMCaller {

    /**
     * 同步调用
     *
     * @param request 请求
     * @return 响应
     */
    LLMResponse call(LLMRequest request);

    /**
     * 异步调用
     *
     * @param request 请求
     * @return 异步响应
     */
    CompletableFuture<LLMResponse> callAsync(LLMRequest request);

    /**
     * 指定模型调用
     *
     * @param modelId 模型 ID
     * @param request 请求
     * @return 响应
     */
    LLMResponse call(String modelId, LLMRequest request);

    /**
     * 指定模型异步调用
     *
     * @param modelId 模型 ID
     * @param request 请求
     * @return 异步响应
     */
    CompletableFuture<LLMResponse> callAsync(String modelId, LLMRequest request);

    /**
     * 流式调用
     *
     * @param request 请求
     * @return 流式响应
     */
    Stream<LLMResponse> streamCall(LLMRequest request);

    /**
     * 指定模型流式调用
     *
     * @param modelId 模型 ID
     * @param request 请求
     * @return 流式响应
     */
    Stream<LLMResponse> streamCall(String modelId, LLMRequest request);
}
