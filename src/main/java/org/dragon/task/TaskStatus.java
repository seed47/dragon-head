package org.dragon.task;

/**
 * Task 状态枚举
 */
public enum TaskStatus {
    /**
     * 待执行
     */
    PENDING,

    /**
     * 执行中
     */
    RUNNING,

    /**
     * 已完成
     */
    COMPLETED,

    /**
     * 失败
     */
    FAILED,

    /**
     * 已取消
     */
    CANCELLED,

    /**
     * 已挂起
     */
    SUSPENDED,

    /**
     * 等待用户输入
     */
    WAITING_USER_INPUT,

    /**
     * 等待依赖任务完成
     */
    WAITING_DEPENDENCY
}
