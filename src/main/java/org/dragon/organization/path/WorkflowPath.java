package org.dragon.organization.path;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WorkflowPath 工作路径
 * 记录组织内成功完成的任务流程
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowPath {

    /**
     * 路径 ID
     */
    private String id;

    /**
     * 组织 ID
     */
    private String organizationId;

    /**
     * 路径名称
     */
    private String name;

    /**
     * 路径描述
     */
    private String description;

    /**
     * 任务类型
     */
    private String taskType;

    /**
     * 步骤列表
     */
    private List<PathStep> steps;

    /**
     * 路径指标
     */
    private PathMetrics metrics;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 是否为最佳实践
     */
    @Builder.Default
    private boolean isBestPractice = false;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 版本号
     */
    @Builder.Default
    private int version = 1;

    /**
     * PathStep 路径步骤
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PathStep {
        /**
         * 步骤序号
         */
        private int sequence;

        /**
         * 负责角色
         */
        private String characterRole;

        /**
         * 执行动作
         */
        private String action;

        /**
         * 输入模式
         */
        private Map<String, Object> inputSchema;

        /**
         * 输出模式
         */
        private Map<String, Object> outputSchema;
    }

    /**
     * PathMetrics 路径指标
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PathMetrics {
        /**
         * 平均执行时长（毫秒）
         */
        private long avgDurationMs;

        /**
         * 成功率
         */
        @Builder.Default
        private double successRate = 0.0;

        /**
         * Token 消耗
         */
        @Builder.Default
        private int tokenConsumption = 0;

        /**
         * 使用次数
         */
        @Builder.Default
        private int usageCount = 0;
    }
}
