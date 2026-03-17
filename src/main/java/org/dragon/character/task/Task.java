package org.dragon.character.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 任务实体
 * 表示 Character 需要执行的任务
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
     * 任务名称
     */
    private String name;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 任务类型
     */
    private TaskType type;

    /**
     * 任务状态
     */
    private TaskStatus status;

    /**
     * 关联的 Character ID
     */
    private String characterId;

    /**
     * 用户输入 / 任务内容
     */
    private String input;

    /**
     * 任务结果
     */
    private String result;

    /**
     * 执行模式：REACT, WORKFLOW
     */
    private String executionMode;

    /**
     * 关联的 workflow ID（如果是 WORKFLOW 模式）
     */
    private String workflowId;

    /**
     * 错误信息（如果执行失败）
     */
    private String errorMessage;

    /**
     * 扩展属性
     */
    private Map<String, Object> extensions;

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
     * 任务类型枚举
     */
    public enum TaskType {
        /** 用户请求 */
        USER_REQUEST,
        /** 内部任务 */
        INTERNAL_TASK,
        /** 定时任务 */
        SCHEDULED_TASK
    }
}
