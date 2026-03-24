package org.dragon.workspace.service;

import org.dragon.task.Task;

/**
 * 任务执行桥接器接口
 * 负责将任务委托给 Character 执行，并处理执行结果
 *
 * @author wyj
 * @version 1.0
 */
public interface TaskBridge {

    /**
     * 执行任务
     *
     * @param task 任务
     * @param workspaceId Workspace ID
     * @return 执行后的任务
     */
    Task execute(Task task, String workspaceId);

    /**
     * 暂停任务
     *
     * @param task 任务
     * @param reason 暂停原因
     * @return 暂停后的任务
     */
    Task suspend(Task task, String reason);

    /**
     * 恢复任务
     *
     * @param task 任务
     * @param newInput 新的输入
     * @return 恢复后的任务
     */
    Task resume(Task task, Object newInput);

    /**
     * 通知依赖任务已解决
     *
     * @param taskId 当前任务 ID
     * @param dependencyTaskId 已解决的依赖任务 ID
     */
    void notifyDependencyResolved(String taskId, String dependencyTaskId);
}
