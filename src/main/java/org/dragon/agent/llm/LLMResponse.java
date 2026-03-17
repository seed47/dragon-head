package org.dragon.agent.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * LLM 响应
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LLMResponse {

    /**
     * 模型 ID
     */
    private String modelId;

    /**
     * 响应内容
     */
    private String content;

    /**
     * 完成原因
     */
    private String finishReason;

    /**
     * 用量统计
     */
    private Usage usage;

    /**
     * 函数调用
     */
    private FunctionCall functionCall;

    /**
     * 是否是流式响应的最后一块
     */
    private boolean lastChunk;

    /**
     * 原始响应
     */
    private Object rawResponse;

    /**
     * 响应时间
     */
    private LocalDateTime responseTime;

    /**
     * 用量统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        /**
         * 输入 token 数
         */
        private int promptTokens;

        /**
         * 输出 token 数
         */
        private int completionTokens;

        /**
         * 总 token 数
         */
        private int totalTokens;
    }

    /**
     * 函数调用
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunctionCall {
        /**
         * 函数名称
         */
        private String name;

        /**
         * 函数参数 (JSON 字符串)
         */
        private String arguments;
    }
}
