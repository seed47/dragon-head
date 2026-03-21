package org.dragon.task;

import java.util.List;
import java.util.Optional;

/**
 * Task 存储接口
 *
 * @author wyj
 * @version 1.0
 */
public interface TaskStore {

    /**
     * 保存任务
     *
     * @param task 任务
     */
    void save(Task task);

    /**
     * 更新任务
     *
     * @param task 任务
     */
    void update(Task task);

    /**
     * 删除任务
     *
     * @param id 任务 ID
     */
    void delete(String id);

    /**
     * 根据 ID 查询任务
     *
     * @param id 任务 ID
     * @return 任务
     */
    Optional<Task> findById(String id);

    /**
     * 根据工作空间 ID 查询任务列表
     *
     * @param workspaceId 工作空间 ID
     * @return 任务列表
     */
    List<Task> findByWorkspaceId(String workspaceId);

    /**
     * 根据父任务 ID 查询子任务列表
     *
     * @param parentTaskId 父任务 ID
     * @return 子任务列表
     */
    List<Task> findByParentTaskId(String parentTaskId);

    /**
     * 根据状态查询任务列表
     *
     * @param status 任务状态
     * @return 任务列表
     */
    List<Task> findByStatus(TaskStatus status);

    /**
     * 检查任务是否存在
     *
     * @param id 任务 ID
     * @return 是否存在
     */
    boolean exists(String id);
}
