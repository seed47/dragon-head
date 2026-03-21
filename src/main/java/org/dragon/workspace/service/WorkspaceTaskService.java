package org.dragon.workspace.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.dragon.task.Task;
import org.dragon.task.TaskStore;
import org.dragon.task.TaskStatus;
import org.dragon.workspace.WorkspaceRegistry;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WorkspaceTaskService 工作空间任务服务
 * 管理工作空间任务的查询、状态更新、结果获取等
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceTaskService {

    private final TaskStore taskStore;
    private final WorkspaceRegistry workspaceRegistry;

    /**
     * 获取任务
     *
     * @param workspaceId 工作空间 ID
     * @param taskId 任务 ID
     * @return 任务
     */
    public Optional<Task> getTask(String workspaceId, String taskId) {
        // 验证工作空间存在
        workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        return taskStore.findById(taskId)
                .filter(task -> workspaceId.equals(task.getWorkspaceId()));
    }

    /**
     * 获取任务结果
     *
     * @param workspaceId 工作空间 ID
     * @param taskId 任务 ID
     * @return 任务结果
     */
    public String getTaskResult(String workspaceId, String taskId) {
        Task task = getTask(workspaceId, taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (task.getResult() == null) {
            throw new IllegalStateException("Task result not available yet: " + taskId);
        }

        return task.getResult();
    }

    /**
     * 取消任务
     *
     * @param workspaceId 工作空间 ID
     * @param taskId 任务 ID
     * @return 更新后的任务
     */
    public Task cancelTask(String workspaceId, String taskId) {
        Task task = getTask(workspaceId, taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        // 只有 PENDING 或 RUNNING 状态可以取消
        if (task.getStatus() != TaskStatus.PENDING &&
                task.getStatus() != TaskStatus.RUNNING) {
            throw new IllegalStateException("Cannot cancel task in status: " + task.getStatus());
        }

        task.setStatus(TaskStatus.CANCELLED);
        task.setUpdatedAt(LocalDateTime.now());
        task.setCompletedAt(LocalDateTime.now());

        taskStore.update(task);
        log.info("[WorkspaceTaskService] Cancelled task: {}", taskId);

        return task;
    }

    /**
     * 更新任务状态
     *
     * @param workspaceId 工作空间 ID
     * @param taskId 任务 ID
     * @param status 新状态
     * @return 更新后的任务
     */
    public Task updateTaskStatus(String workspaceId, String taskId, TaskStatus status) {
        Task task = getTask(workspaceId, taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        task.setStatus(status);
        task.setUpdatedAt(LocalDateTime.now());

        if (status == TaskStatus.RUNNING && task.getStartedAt() == null) {
            task.setStartedAt(LocalDateTime.now());
        }

        if (status == TaskStatus.COMPLETED || status == TaskStatus.FAILED || status == TaskStatus.CANCELLED) {
            task.setCompletedAt(LocalDateTime.now());
        }

        taskStore.update(task);
        log.info("[WorkspaceTaskService] Updated task status: {} -> {}", taskId, status);

        return task;
    }

    /**
     * 更新任务结果
     *
     * @param workspaceId 工作空间 ID
     * @param taskId 任务 ID
     * @param result 任务结果
     * @return 更新后的任务
     */
    public Task updateTaskResult(String workspaceId, String taskId, String result) {
        Task task = getTask(workspaceId, taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        task.setResult(result);
        task.setStatus(TaskStatus.COMPLETED);
        task.setUpdatedAt(LocalDateTime.now());
        task.setCompletedAt(LocalDateTime.now());

        taskStore.update(task);
        log.info("[WorkspaceTaskService] Updated task result: {}", taskId);

        return task;
    }

    /**
     * 更新任务错误信息
     *
     * @param workspaceId 工作空间 ID
     * @param taskId 任务 ID
     * @param errorMessage 错误信息
     * @return 更新后的任务
     */
    public Task updateTaskError(String workspaceId, String taskId, String errorMessage) {
        Task task = getTask(workspaceId, taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        task.setErrorMessage(errorMessage);
        task.setStatus(TaskStatus.FAILED);
        task.setUpdatedAt(LocalDateTime.now());
        task.setCompletedAt(LocalDateTime.now());

        taskStore.update(task);
        log.error("[WorkspaceTaskService] Task failed: {} - {}", taskId, errorMessage);

        return task;
    }

    /**
     * 获取工作空间的所有任务
     *
     * @param workspaceId 工作空间 ID
     * @return 任务列表
     */
    public List<Task> listTasks(String workspaceId) {
        workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        return taskStore.findByWorkspaceId(workspaceId);
    }

    /**
     * 获取工作空间中指定状态的任务
     *
     * @param workspaceId 工作空间 ID
     * @param status 任务状态
     * @return 任务列表
     */
    public List<Task> listTasksByStatus(String workspaceId, TaskStatus status) {
        List<Task> tasks = listTasks(workspaceId);
        return tasks.stream()
                .filter(task -> task.getStatus() == status)
                .collect(java.util.stream.Collectors.toList());
    }
}
