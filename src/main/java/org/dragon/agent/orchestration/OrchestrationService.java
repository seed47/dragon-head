package org.dragon.agent.orchestration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 编排服务接口
 * 负责组合 Workflow 和 ReAct 形成复杂流程
 *
 * @author wyj
 * @version 1.0
 */
public interface OrchestrationService {

    /**
     * 执行编排
     * 根据 Mode 决定使用哪种执行方式（WORKFLOW 或 REACT）
     * 实际执行由 Character 完成
     *
     * @param request 编排请求
     * @return 编排结果
     */
    OrchestrationResult orchestrate(OrchestrationRequest request);

    /**
     * 编排请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class OrchestrationRequest {
        private String characterId;
        private String message;
        private Mode mode;
        private String workflowId;
        private String contextId;
    }

    /**
     * ReAct 请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ReActRequest {
        private String characterId;
        private String userInput;
        private String systemPrompt;
        private String defaultModelId;
        private int maxIterations;
    }

    /**
     * 编排结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class OrchestrationResult {
        private boolean success;
        private String response;
        private String executionId;
        private long durationMs;
    }

    /**
     * 编排模式
     */
    enum Mode {
        WORKFLOW,
        REACT
    }
}
