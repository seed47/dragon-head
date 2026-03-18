package org.dragon.agent.orchestration;

/**
 * 编排服务接口
 * 负责决定使用哪种执行策略（WORKFLOW 或 REACT）
 *
 * 调用方：Character
 *
 * @author wyj
 * @version 1.0
 */
public interface OrchestrationService {

    /**
     * 执行编排
     * 根据请求内容决定使用哪种执行策略
     * 返回策略信息，由调用方执行具体流程
     *
     * @param request 编排请求
     * @return 编排结果（包含执行策略信息）
     */
    OrchestrationResult orchestrate(OrchestrationRequest request);

    /**
     * 编排请求
     */
    public class OrchestrationRequest {
        private String characterId;
        private String message;
        private Mode mode;
        private String workflowId;
        private String contextId;

        public OrchestrationRequest() {
        }

        public OrchestrationRequest(String characterId, String message, Mode mode, String workflowId) {
            this.characterId = characterId;
            this.message = message;
            this.mode = mode;
            this.workflowId = workflowId;
        }

        public String getCharacterId() {
            return characterId;
        }

        public void setCharacterId(String characterId) {
            this.characterId = characterId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Mode getMode() {
            return mode;
        }

        public void setMode(Mode mode) {
            this.mode = mode;
        }

        public String getWorkflowId() {
            return workflowId;
        }

        public void setWorkflowId(String workflowId) {
            this.workflowId = workflowId;
        }

        public String getContextId() {
            return contextId;
        }

        public void setContextId(String contextId) {
            this.contextId = contextId;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String characterId;
            private String message;
            private Mode mode;
            private String workflowId;
            private String contextId;

            public Builder characterId(String characterId) {
                this.characterId = characterId;
                return this;
            }

            public Builder message(String message) {
                this.message = message;
                return this;
            }

            public Builder mode(Mode mode) {
                this.mode = mode;
                return this;
            }

            public Builder workflowId(String workflowId) {
                this.workflowId = workflowId;
                return this;
            }

            public Builder contextId(String contextId) {
                this.contextId = contextId;
                return this;
            }

            public OrchestrationRequest build() {
                return new OrchestrationRequest(characterId, message, mode, workflowId);
            }
        }
    }

    /**
     * 编排结果
     * 只包含策略信息，不包含执行结果
     * 执行结果由 Character.run() / ReActExecutor 返回
     */
    public class OrchestrationResult {
        private boolean success;
        private String executionId;
        private long durationMs;
        private Mode mode;
        private String workflowId;

        public OrchestrationResult() {
        }

        public OrchestrationResult(boolean success, String executionId, long durationMs, Mode mode, String workflowId) {
            this.success = success;
            this.executionId = executionId;
            this.durationMs = durationMs;
            this.mode = mode;
            this.workflowId = workflowId;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getExecutionId() {
            return executionId;
        }

        public void setExecutionId(String executionId) {
            this.executionId = executionId;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public void setDurationMs(long durationMs) {
            this.durationMs = durationMs;
        }

        public Mode getMode() {
            return mode;
        }

        public void setMode(Mode mode) {
            this.mode = mode;
        }

        public String getWorkflowId() {
            return workflowId;
        }

        public void setWorkflowId(String workflowId) {
            this.workflowId = workflowId;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private boolean success;
            private String executionId;
            private long durationMs;
            private Mode mode;
            private String workflowId;

            public Builder success(boolean success) {
                this.success = success;
                return this;
            }

            public Builder executionId(String executionId) {
                this.executionId = executionId;
                return this;
            }

            public Builder durationMs(long durationMs) {
                this.durationMs = durationMs;
                return this;
            }

            public Builder mode(Mode mode) {
                this.mode = mode;
                return this;
            }

            public Builder workflowId(String workflowId) {
                this.workflowId = workflowId;
                return this;
            }

            public OrchestrationResult build() {
                return new OrchestrationResult(success, executionId, durationMs, mode, workflowId);
            }
        }
    }

    /**
     * 编排模式
     */
    enum Mode {
        WORKFLOW,
        REACT
    }
}
