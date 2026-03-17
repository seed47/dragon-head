package org.dragon.character.task;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 * 任务管理器默认实现
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
public class DefaultTaskManager implements TaskManager {

    private final Map<String, Task> tasks = new ConcurrentHashMap<>();

    @Override
    public Task addTask(Task task) {
        if (task.getId() == null || task.getId().isEmpty()) {
            task.setId(UUID.randomUUID().toString());
        }
        if (task.getStatus() == null) {
            task.setStatus(TaskStatus.PENDING);
        }
        if (task.getCreatedAt() == null) {
            task.setCreatedAt(LocalDateTime.now());
        }
        task.setUpdatedAt(LocalDateTime.now());
        tasks.put(task.getId(), task);
        log.info("[TaskManager] Task added: {}", task.getId());
        return task;
    }

    @Override
    public Task getTask(String taskId) {
        return tasks.get(taskId);
    }

    @Override
    public List<Task> listTasks() {
        return new ArrayList<>(tasks.values());
    }

    @Override
    public List<Task> getTasksByStatus(TaskStatus status) {
        return tasks.values().stream()
                .filter(task -> task.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public Task operateTask(String taskId, TaskOperation operation) {
        Task task = tasks.get(taskId);
        if (task == null) {
            log.warn("[TaskManager] Task not found: {}", taskId);
            return null;
        }

        switch (operation) {
            case PAUSE -> {
                if (task.getStatus() == TaskStatus.RUNNING) {
                    task.setStatus(TaskStatus.PAUSED);
                    task.setUpdatedAt(LocalDateTime.now());
                    log.info("[TaskManager] Task paused: {}", taskId);
                }
            }

            case RESUME -> {
                if (task.getStatus() == TaskStatus.PAUSED) {
                    task.setStatus(TaskStatus.PENDING);
                    task.setUpdatedAt(LocalDateTime.now());
                    log.info("[TaskManager] Task resumed: {}", taskId);
                }
            }

            case CANCEL -> {
                if (task.getStatus() != TaskStatus.COMPLETED && task.getStatus() != TaskStatus.CANCELLED) {
                    task.setStatus(TaskStatus.CANCELLED);
                    task.setUpdatedAt(LocalDateTime.now());
                    log.info("[TaskManager] Task cancelled: {}", taskId);
                }
            }

            case RETRY -> {
                if (task.getStatus() == TaskStatus.FAILED) {
                    task.setStatus(TaskStatus.PENDING);
                    task.setErrorMessage(null);
                    task.setUpdatedAt(LocalDateTime.now());
                    log.info("[TaskManager] Task retry scheduled: {}", taskId);
                }
            }

            case DELETE -> {
                deleteTask(taskId);
                return null;
            }

            default -> log.warn("[TaskManager] Unknown operation: {}", operation);
        }

        return task;
    }

    @Override
    public boolean deleteTask(String taskId) {
        Task removed = tasks.remove(taskId);
        if (removed != null) {
            log.info("[TaskManager] Task deleted: {}", taskId);
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        tasks.clear();
        log.info("[TaskManager] All tasks cleared");
    }

    @Override
    public int size() {
        return tasks.size();
    }
}
