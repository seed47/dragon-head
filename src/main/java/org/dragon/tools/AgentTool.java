package org.dragon.tools;

import com.fasterxml.jackson.databind.JsonNode;
// import com.openclaw.agent.models.ModelProvider;
import org.dragon.config.config.ConfigProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.CompletableFuture;

/**
 * Agent 工具接口
 *
 * <p>
 * 定义 Agent 工具的基本契约。每个工具都有名称、描述、参数 schema，
 * 以及一个异步执行方法，返回 {@link ToolResult}。
 * 对应 TypeScript 中的 AgentTool / AnyAgentTool。
 * </p>
 */
public interface AgentTool {

    /**
     * 获取工具的唯一名称。
     *
     * @return 工具名称（如 "exec", "browser", "canvas"）
     */
    String getName();

    /**
     * 获取供 LLM 阅读的人类可读描述。
     *
     * @return 工具描述
     */
    String getDescription();

    /**
     * 获取描述工具输入参数的 JSON Schema。
     *
     * @return 参数 schema 的 JsonNode
     */
    JsonNode getParameterSchema();

    /**
     * 使用给定上下文执行工具。
     *
     * @param context 工具执行上下文
     * @return 异步执行结果
     */
    CompletableFuture<ToolResult> execute(ToolContext context);

    // --- 辅助类型 ---

    /**
     * 工具执行结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ToolResult {
        /** 执行是否成功 */
        private boolean success;
        /** 执行输出的文本 */
        private String output;
        /** 额外的数据对象 */
        private Object data;
        /** 错误信息 */
        private String error;
        /**
         * 多模态内容部分（文本 + 图片）。当非空时，优先于普通 {@code output} 字符串。
         */
        // private java.util.List<ModelProvider.ContentPart> contentParts;

        /**
         * 创建成功的执行结果。
         *
         * @param output 输出文本
         * @return ToolResult 实例
         */
        public static ToolResult ok(String output) {
            return ToolResult.builder().success(true).output(output).build();
        }

        /**
         * 创建成功的执行结果（带额外数据）。
         *
         * @param output 输出文本
         * @param data   额外数据
         * @return ToolResult 实例
         */
        public static ToolResult ok(String output, Object data) {
            return ToolResult.builder().success(true).output(output).data(data).build();
        }

        /**
         * 创建失败的执行结果。
         *
         * @param error 错误信息
         * @return ToolResult 实例
         */
        public static ToolResult fail(String error) {
            return ToolResult.builder().success(false).error(error).build();
        }
    }

    /**
     * 工具执行上下文
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ToolContext {
        /** 工具参数（JSON 格式） */
        private JsonNode parameters;
        /** 会话键 */
        private String sessionKey;
        /** 当前工作目录 */
        private String cwd;
        /** OpenClaw 配置 */
        private org.dragon.config.config.ConfigProperties config;
    }
}
