package org.dragon.agent.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * LLM 请求
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LLMRequest {

    /**
     * 模型 ID
     */
    private String modelId;

    /**
     * 消息列表
     */
    private List<LLMMessage> messages;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 温度
     */
    private Double temperature;

    /**
     * 最大 token 数
     */
    private Integer maxTokens;

    /**
     * 额外参数
     */
    private Map<String, Object> extraParams;

    /**
     * 是否流式输出
     */
    private boolean stream;

    /**
     * LLM 消息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LLMMessage {
        /**
         * 角色
         */
        private Role role;

        /**
         * 内容
         */
        private String content;

        /**
         * 消息名称（用于 function call）
         */
        private String name;

        /**
         * 角色枚举
         */
        public enum Role {
            SYSTEM,
            USER,
            ASSISTANT,
            FUNCTION
        }
    }
}
