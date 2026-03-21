package org.dragon.task;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Task 任务实体
 * 作为基础包，统一表示所有任务
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    /**
     * 任务唯一标识
     */
    private String id;

    /**
     * 工作空间 ID
     */
    private String workspaceId;

    /**
     * 父任务 ID（如果是子任务则有值）
     */
    private String parentTaskId;

    /**
     * 创建者 ID
     */
    private String creatorId;

    /**
     * 执行者 Character ID
     */
    private String characterId;

    /**
     * 任务名称
     */
    private String name;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 任务状态
     */
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    /**
     * 任务输入
     */
    private Object input;

    /**
     * 任务输出
     */
    private Object output;

    /**
     * 任务结果（String 形式）
     */
    private String result;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 子任务 ID 列表
     */
    @Builder.Default
    private List<String> childTaskIds = new ArrayList<>();

    /**
     * 协作会话 ID
     */
    private String collaborationSessionId;

    /**
     * 分配的成员 ID 列表
     */
    @Builder.Default
    private List<String> assignedMemberIds = new ArrayList<>();

    /**
     * 执行步骤列表
     */
    @Builder.Default
    private List<ExecutionStep> executionSteps = new ArrayList<>();

    /**
     * 执行消息列表（支持流式）
     */
    @Builder.Default
    private List<ExecutionMessage> executionMessages = new ArrayList<>();

    /**
     * 当前流式消息（正在输出中）
     */
    private String currentStreamingContent;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 开始执行时间
     */
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * ExecutionStep 执行步骤
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionStep {

        /**
         * 步骤序号
         */
        private int stepNumber;

        /**
         * 步骤类型 (THOUGHT, ACTION, OBSERVATION)
         */
        private String stepType;

        /**
         * 步骤内容
         */
        private String content;

        /**
         * 使用的模型
         */
        private String modelId;

        /**
         * 消耗的 token
         */
        private int tokenConsumption;

        /**
         * 执行时长（毫秒）
         */
        private long durationMs;

        /**
         * 时间戳
         */
        private LocalDateTime timestamp;
    }

    /**
     * ExecutionMessage 执行消息（支持流式）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionMessage {

        /**
         * 消息 ID
         */
        private String messageId;

        /**
         * 角色 (USER, ASSISTANT, SYSTEM, TOOL)
         */
        private String role;

        /**
         * 消息内容
         */
        private String content;

        /**
         * 是否为流式
         */
        private boolean streaming;

        /**
         * 时间戳
         */
        private LocalDateTime timestamp;
    }
}
