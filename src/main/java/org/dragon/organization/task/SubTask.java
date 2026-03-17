package org.dragon.organization.task;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SubTask 子任务
 * 对应 Character Task，由特定 Character 执行
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubTask {

    /**
     * 子任务 ID
     */
    private String id;

    /**
     * 父任务 ID (OrganizationTask ID)
     */
    private String organizationTaskId;

    /**
     * 执行者 Character ID
     */
    private String characterId;

    /**
     * 执行者角色
     */
    private String role;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 任务输入
     */
    private Object input;

    /**
     * 任务输出
     */
    private Object output;

    /**
     * 任务状态
     */
    @Builder.Default
    private OrgTaskStatus status = OrgTaskStatus.SUBMITTED;

    /**
     * 执行结果
     */
    private ExecutionResult executionResult;

    /**
     * 依赖的子任务 ID 列表
     */
    private List<String> dependencies;

    /**
     * 执行顺序
     */
    @Builder.Default
    private int order = 0;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * ExecutionResult 执行结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionResult {
        /**
         * 是否成功
         */
        private boolean success;

        /**
         * 结果数据
         */
        private Object result;

        /**
         * 错误信息
         */
        private String error;

        /**
         * 执行的 Character Task ID
         */
        private String characterTaskId;

        /**
         * 消耗的 token 数量
         */
        private int tokenConsumption;

        /**
         * 执行时长（毫秒）
         */
        private long durationMs;
    }
}
