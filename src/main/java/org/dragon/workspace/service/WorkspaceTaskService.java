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
    private final TaskBridge taskBridge;

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

    /**
     * 暂停任务
     *
     * @param workspaceId 工作空间 ID
     * @param taskId 任务 ID
     * @param reason 暂停原因
     * @return 更新后的任务
     */
    public Task suspendTask(String workspaceId, String taskId, String reason) {
        Task task = getTask(workspaceId, taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        task.setStatus(TaskStatus.SUSPENDED);
        task.setWaitingReason(reason);
        task.setUpdatedAt(LocalDateTime.now());
        taskStore.update(task);

        log.info("[WorkspaceTaskService] Suspended task {}: {}", taskId, reason);
        return task;
    }

    /**
     * 标记任务等待用户输入
     *
     * @param workspaceId 工作空间 ID
     * @param taskId 任务 ID
     * @param question 需要询问用户的问题
     * @return 更新后的任务
     */
    public Task markWaitingUserInput(String workspaceId, String taskId, String question) {
        Task task = getTask(workspaceId, taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        task.setStatus(TaskStatus.WAITING_USER_INPUT);
        task.setLastQuestion(question);
        task.setWaitingReason("WAITING_USER_INPUT");
        task.setUpdatedAt(LocalDateTime.now());
        taskStore.update(task);

        log.info("[WorkspaceTaskService] Task {} waiting for user input: {}", taskId, question);
        return task;
    }

    /**
     * 标记任务等待依赖完成
     *
     * @param workspaceId 工作空间 ID
     * @param taskId 任务 ID
     * @param dependencyTaskId 依赖的任务 ID
     * @return 更新后的任务
     */
    public Task markWaitingDependency(String workspaceId, String taskId, String dependencyTaskId) {
        Task task = getTask(workspaceId, taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        task.setStatus(TaskStatus.WAITING_DEPENDENCY);
        task.setWaitingReason("WAITING_DEPENDENCY: " + dependencyTaskId);
        task.setUpdatedAt(LocalDateTime.now());

        // 添加依赖
        if (task.getDependencyTaskIds() == null) {
            task.setDependencyTaskIds(new java.util.ArrayList<>());
        }
        if (!task.getDependencyTaskIds().contains(dependencyTaskId)) {
            task.getDependencyTaskIds().add(dependencyTaskId);
        }

        taskStore.update(task);
        log.info("[WorkspaceTaskService] Task {} waiting for dependency: {}", taskId, dependencyTaskId);
        return task;
    }

    /**
     * 恢复任务
     *
     * @param workspaceId 工作空间 ID
     * @param taskId 任务 ID
     * @param newInput 新的输入（用户回复）
     * @return 更新后的任务
     */
    public Task resumeTask(String workspaceId, String taskId, Object newInput) {
        Task task = getTask(workspaceId, taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        // 更新输入（追加用户回复）
        if (newInput != null) {
            Object currentInput = task.getInput();
            task.setInput(currentInput != null ? currentInput.toString() + "\n" + newInput.toString() : newInput.toString());
        }

        task.setStatus(TaskStatus.RUNNING);
        task.setWaitingReason(null);
        task.setUpdatedAt(LocalDateTime.now());
        taskStore.update(task);

        log.info("[WorkspaceTaskService] Resumed task {}", taskId);
        return task;
    }

    /**
     * 追加执行消息到任务
     *
     * @param workspaceId 工作空间 ID
     * @param taskId 任务 ID
     * @param message 执行消息
     * @return 更新后的任务
     */
    public Task appendExecutionMessage(String workspaceId, String taskId, Task.ExecutionMessage message) {
        Task task = getTask(workspaceId, taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (task.getExecutionMessages() == null) {
            task.setExecutionMessages(new java.util.ArrayList<>());
        }
        task.getExecutionMessages().add(message);
        task.setUpdatedAt(LocalDateTime.now());
        taskStore.update(task);

        return task;
    }
}
