package org.dragon.schedule.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

/**
 * Cron 任务定义
 * 对应架构文档中的 CronDefinition 领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CronDefinition {

    // ==================== 标识 ====================
    private String id;

    // ==================== 基本信息 ====================
    private String name;
    private String description;
    private String createdBy;

    // ==================== 调度配置 ====================
    private CronType cronType;
    private String cronExpression;
    private String timezone;
    private Long startTime;
    private Long endTime;
    private Integer maxConcurrent;

    // ==================== 任务配置 ====================
    private JobType jobType;
    private String jobHandler;
    private Map<String, Object> jobData;

    // ==================== 执行策略 ====================
    private MisfirePolicy misfirePolicy;
    private Integer timeoutMs;
    private Integer retryCount;
    private Integer retryIntervalMs;

    // ==================== 状态管理 ====================
    private CronStatus status;
    private Long createdAt;
    private Long updatedAt;
    private Integer version;

    /**
     * 验证 Cron 定义的有效性
     */
    public ValidationResult validate() {
        ValidationResult result = new ValidationResult();

        if (id == null || id.trim().isEmpty()) {
            result.addError("id", "Cron ID 不能为空");
        }

        if (name == null || name.trim().isEmpty()) {
            result.addError("name", "任务名称不能为空");
        }

        if (cronType == null) {
            result.addError("cronType", "cronType不能为空");
        }

        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            result.addError("cronExpression", "Cron 表达式不能为空");
        }

        if (jobHandler == null || jobHandler.trim().isEmpty()) {
            result.addError("jobHandler", "任务处理器不能为空");
        }

        return result;
    }

    /**
     * 更新状态
     */
    public void updateStatus(CronStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = System.currentTimeMillis();
    }
}
