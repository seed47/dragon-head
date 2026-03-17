package org.dragon.agent.llm.usage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * LLM 调用记录
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LLMCallRecord {

    /**
     * 调用 ID
     */
    private String callId;

    /**
     * 模型 ID
     */
    private String modelId;

    /**
     * Character ID
     */
    private String characterId;

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

    /**
     * 延迟 (毫秒)
     */
    private long latencyMs;

    /**
     * 响应状态
     */
    private ResponseStatus status;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 调用时间
     */
    private LocalDateTime timestamp;

    /**
     * 响应状态枚举
     */
    public enum ResponseStatus {
        SUCCESS,
        ERROR,
        TIMEOUT,
        RATE_LIMIT
    }
}
