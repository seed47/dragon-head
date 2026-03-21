package org.dragon.observer.optimization;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OptimizationAction 优化动作实体
 * 定义对 Character 或 Workspace 的优化操作
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizationAction {

    /**
     * 目标类型
     */
    public enum TargetType {
        CHARACTER,
        WORKSPACE
    }

    /**
     * 动作类型
     */
    public enum ActionType {
        UPDATE_MIND,           // 更新心智
        UPDATE_PERSONALITY,    // 更新性格描述
        ADD_SKILL,             // 添加技能
        REMOVE_SKILL,          // 移除技能
        ADJUST_WEIGHT,         // 调整权重
        UPDATE_MEMORY,         // 更新记忆
        UPDATE_TAG,           // 更新标签
        UPDATE_CONFIG          // 更新配置
    }

    /**
     * 状态
     */
    public enum Status {
        PENDING,      // 待执行
        EXECUTED,    // 已执行
        ROLLED_BACK,  // 已回滚
        REJECTED,    // 已拒绝
        FAILED       // 执行失败
    }

    /**
     * 优化动作唯一标识
     */
    private String id;

    /**
     * 关联的评价记录 ID
     */
    private String evaluationId;

    /**
     * 目标类型
     */
    private TargetType targetType;

    /**
     * 目标 ID
     */
    private String targetId;

    /**
     * 动作类型
     */
    private ActionType actionType;

    /**
     * 修改参数 (JSON 格式)
     */
    private Map<String, Object> parameters;

    /**
     * 状态
     */
    @Builder.Default
    private Status status = Status.PENDING;

    /**
     * 执行结果
     */
    private String result;

    /**
     * 执行时间
     */
    private LocalDateTime executedAt;

    /**
     * 回滚时间
     */
    private LocalDateTime rolledBackAt;

    /**
     * 拒绝原因
     */
    private String rejectionReason;

    /**
     * 优先级 (数字越小越高)
     */
    @Builder.Default
    private int priority = 0;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 修改前快照 (JSON 格式)
     */
    private String beforeSnapshot;

    /**
     * 修改后快照 (JSON 格式)
     */
    private String afterSnapshot;

    /**
     * 检查是否可执行
     */
    public boolean canExecute() {
        return status == Status.PENDING;
    }

    /**
     * 检查是否可回滚
     */
    public boolean canRollback() {
        return status == Status.EXECUTED;
    }

    /**
     * 标记为已执行
     */
    public void markExecuted(String result) {
        this.status = Status.EXECUTED;
        this.result = result;
        this.executedAt = LocalDateTime.now();
    }

    /**
     * 标记为已回滚
     */
    public void markRolledBack() {
        this.status = Status.ROLLED_BACK;
        this.rolledBackAt = LocalDateTime.now();
    }

    /**
     * 标记为已拒绝
     */
    public void markRejected(String reason) {
        this.status = Status.REJECTED;
        this.rejectionReason = reason;
    }
}
