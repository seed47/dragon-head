package org.dragon.character.task;

/**
 * 任务操作枚举
 * 定义对任务可以执行的操作
 *
 * @author wyj
 * @version 1.0
 */
public enum TaskOperation {

    /** 添加任务 */
    ADD,

    /** 暂停任务 */
    PAUSE,

    /** 恢复任务 */
    RESUME,

    /** 取消任务 */
    CANCEL,

    /** 重试任务 */
    RETRY,

    /** 删除任务 */
    DELETE
}
