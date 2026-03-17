package org.dragon.character.task;

/**
 * 任务状态枚举
 * 定义任务的生命周期状态
 *
 * @author wyj
 * @version 1.0
 */
public enum TaskStatus {

    /** 待执行 */
    PENDING,

    /** 执行中 */
    RUNNING,

    /** 已暂停 */
    PAUSED,

    /** 已完成 */
    COMPLETED,

    /** 执行失败 */
    FAILED,

    /** 已取消 */
    CANCELLED
}
