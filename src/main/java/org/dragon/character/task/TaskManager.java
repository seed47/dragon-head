package org.dragon.character.task;

import java.util.List;

/**
 * 任务管理器接口
 * 负责管理 Character 的任务队列
 *
 * @author wyj
 * @version 1.0
 */
public interface TaskManager {

    /**
     * 添加任务
     *
     * @param task 任务
     * @return 添加后的任务
     */
    Task addTask(Task task);

    /**
     * 获取任务
     *
     * @param taskId 任务ID
     * @return 任务，如果不存在则返回 null
     */
    Task getTask(String taskId);

    /**
     * 列出所有任务
     *
     * @return 任务列表
     */
    List<Task> listTasks();

    /**
     * 根据状态获取任务
     *
     * @param status 任务状态
     * @return 任务列表
     */
    List<Task> getTasksByStatus(TaskStatus status);

    /**
     * 操作任务
     *
     * @param taskId    任务ID
     * @param operation 操作
     * @return 操作后的任务，如果任务不存在则返回 null
     */
    Task operateTask(String taskId, TaskOperation operation);

    /**
     * 删除任务
     *
     * @param taskId 任务ID
     * @return 是否删除成功
     */
    boolean deleteTask(String taskId);

    /**
     * 清空所有任务
     */
    void clear();

    /**
     * 获取任务数量
     *
     * @return 任务数量
     */
    int size();
}
