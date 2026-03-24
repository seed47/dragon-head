package org.dragon.workspace.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.task.TaskStore;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Workspace 任务执行服务
 * 负责执行子任务，支持按依赖顺序串行执行
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceTaskExecutionService {

    private final TaskStore taskStore;
    private final TaskBridge taskBridge;

    /**
     * 执行单个子任务
     *
     * @param childTask 子任务
     * @param parentTask 父任务
     */
    public void executeChildTask(Task childTask, Task parentTask) {
        String workspaceId = parentTask.getWorkspaceId();

        // 开始前更新状态为 RUNNING
        childTask.setStatus(TaskStatus.RUNNING);
        childTask.setStartedAt(LocalDateTime.now());
        taskStore.update(childTask);

        log.info("[WorkspaceTaskExecutionService] Executing childTask {} on character {}",
                childTask.getId(), childTask.getCharacterId());

        // 委托 TaskBridge 执行
        Task result = taskBridge.execute(childTask, workspaceId);

        // 根据执行结果更新状态
        if (result.getStatus() == TaskStatus.COMPLETED) {
            log.info("[WorkspaceTaskExecutionService] ChildTask {} completed successfully", childTask.getId());
        } else if (result.getStatus() == TaskStatus.FAILED) {
            log.warn("[WorkspaceTaskExecutionService] ChildTask {} failed: {}", childTask.getId(), result.getErrorMessage());
        }

        // 检查并更新父任务状态
        checkAndCompleteParentTask(parentTask);
    }

    /**
     * 执行多个子任务（串行执行）
     * 第一版先串行，保留后续并发扩展点
     *
     * @param childTasks 子任务列表
     * @param parentTask 父任务
     */
    public void executeChildTasks(List<Task> childTasks, Task parentTask) {
        for (Task childTask : childTasks) {
            try {
                // 检查依赖是否满足
                if (!areDependenciesMet(childTask)) {
                    // 依赖未满足，跳过执行（后续可改为加入等待队列）
                    log.warn("[WorkspaceTaskExecutionService] Dependencies not met for task {}, marking as WAITING_DEPENDENCY", childTask.getId());
                    childTask.setStatus(TaskStatus.WAITING_DEPENDENCY);
                    taskStore.update(childTask);
                    continue;
                }

                executeChildTask(childTask, parentTask);

            } catch (Exception e) {
                log.error("[WorkspaceTaskExecutionService] Error executing childTask {}: {}", childTask.getId(), e.getMessage(), e);
                childTask.setStatus(TaskStatus.FAILED);
                childTask.setErrorMessage(e.getMessage());
                taskStore.update(childTask);
            }
        }
    }

    /**
     * 检查依赖是否满足
     *
     * @param task 任务
     * @return 依赖是否满足
     */
    private boolean areDependenciesMet(Task task) {
        List<String> dependencyTaskIds = task.getDependencyTaskIds();
        if (dependencyTaskIds == null || dependencyTaskIds.isEmpty()) {
            return true;
        }

        // 检查所有依赖任务是否已完成
        for (String depTaskId : dependencyTaskIds) {
            Optional<Task> depTask = taskStore.findById(depTaskId);
            if (depTask.isEmpty() || depTask.get().getStatus() != TaskStatus.COMPLETED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查并更新父任务状态
     */
    private void checkAndCompleteParentTask(Task parentTask) {
        List<String> childTaskIds = parentTask.getChildTaskIds();
        if (childTaskIds == null || childTaskIds.isEmpty()) {
            return;
        }

        List<Task> childTasks = childTaskIds.stream()
                .map(taskStore::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        boolean allCompleted = childTasks.stream()
                .allMatch(st -> st.getStatus() == TaskStatus.COMPLETED);
        boolean anyFailed = childTasks.stream()
                .anyMatch(st -> st.getStatus() == TaskStatus.FAILED);
        boolean anyRunning = childTasks.stream()
                .anyMatch(st -> st.getStatus() == TaskStatus.RUNNING);
        boolean anyWaiting = childTasks.stream()
                .anyMatch(st -> st.getStatus() == TaskStatus.WAITING_DEPENDENCY
                        || st.getStatus() == TaskStatus.WAITING_USER_INPUT
                        || st.getStatus() == TaskStatus.SUSPENDED);

        if (allCompleted) {
            parentTask.setStatus(TaskStatus.COMPLETED);
            parentTask.setCompletedAt(LocalDateTime.now());
            parentTask.setResult("All child tasks completed successfully");
            taskStore.update(parentTask);
            log.info("[WorkspaceTaskExecutionService] Parent task {} completed", parentTask.getId());
        } else if (anyFailed) {
            parentTask.setStatus(TaskStatus.FAILED);
            taskStore.update(parentTask);
            log.warn("[WorkspaceTaskExecutionService] Parent task {} has failed child tasks", parentTask.getId());
        } else if (!anyRunning && anyWaiting) {
            // 没有正在运行的，但有等待中的
            parentTask.setStatus(TaskStatus.RUNNING); // 保持运行状态
            taskStore.update(parentTask);
        }
    }

    /**
     * 暂停任务
     */
    public Task suspendTask(String workspaceId, String taskId, String reason) {
        Task task = taskStore.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        return taskBridge.suspend(task, reason);
    }

    /**
     * 恢复任务
     */
    public Task resumeTask(String workspaceId, String taskId, Object newInput) {
        Task task = taskStore.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        return taskBridge.resume(task, newInput);
    }

    /**
     * 标记任务等待用户输入
     */
    public Task markWaitingUserInput(String workspaceId, String taskId, String question) {
        Task task = taskStore.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        task.setStatus(TaskStatus.WAITING_USER_INPUT);
        task.setErrorMessage(question); // 存储最后的问题
        task.setUpdatedAt(LocalDateTime.now());
        taskStore.update(task);

        log.info("[WorkspaceTaskExecutionService] Task {} waiting for user input: {}", taskId, question);
        return task;
    }

    /**
     * 标记任务等待依赖
     */
    public Task markWaitingDependency(String workspaceId, String taskId, String dependencyTaskId) {
        Task task = taskStore.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        // 添加依赖
        List<String> deps = task.getDependencyTaskIds();
        if (deps == null) {
            deps = new java.util.ArrayList<>();
            task.setDependencyTaskIds(deps);
        }
        if (!deps.contains(dependencyTaskId)) {
            deps.add(dependencyTaskId);
        }

        task.setStatus(TaskStatus.WAITING_DEPENDENCY);
        task.setUpdatedAt(LocalDateTime.now());
        taskStore.update(task);

        log.info("[WorkspaceTaskExecutionService] Task {} waiting for dependency: {}", taskId, dependencyTaskId);
        return task;
    }
}
