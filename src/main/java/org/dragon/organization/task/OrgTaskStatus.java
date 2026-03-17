package org.dragon.organization.task;

/**
 * OrgTaskStatus 组织任务状态枚举
 *
 * @author wyj
 * @version 1.0
 */
public enum OrgTaskStatus {
    SUBMITTED,    // 已提交
    DECOMPOSED,   // 已分解
    ASSIGNED,     // 已分配
    RUNNING,       // 执行中
    COMPLETED,     // 已完成
    FAILED,        // 失败
    CANCELLED      // 已取消
}
