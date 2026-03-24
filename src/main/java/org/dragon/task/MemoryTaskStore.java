package org.dragon.task;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Task 内存存储实现
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class MemoryTaskStore implements TaskStore {

    private final Map<String, Task> store = new ConcurrentHashMap<>();

    @Override
    public void save(Task task) {
        store.put(task.getId(), task);
        log.debug("[MemoryTaskStore] Saved task: {}", task.getId());
    }

    @Override
    public void update(Task task) {
        if (store.containsKey(task.getId())) {
            store.put(task.getId(), task);
            log.debug("[MemoryTaskStore] Updated task: {}", task.getId());
        }
    }

    @Override
    public void delete(String id) {
        store.remove(id);
        log.debug("[MemoryTaskStore] Deleted task: {}", id);
    }

    @Override
    public Optional<Task> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Task> findByWorkspaceId(String workspaceId) {
        return store.values().stream()
                .filter(task -> workspaceId.equals(task.getWorkspaceId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findByParentTaskId(String parentTaskId) {
        return store.values().stream()
                .filter(task -> parentTaskId.equals(task.getParentTaskId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findByStatus(TaskStatus status) {
        return store.values().stream()
                .filter(task -> task.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findByCharacterId(String characterId) {
        return store.values().stream()
                .filter(task -> characterId.equals(task.getCharacterId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findByCreatorId(String creatorId) {
        return store.values().stream()
                .filter(task -> creatorId.equals(task.getCreatorId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findByCollaborationSessionId(String collaborationSessionId) {
        return store.values().stream()
                .filter(task -> collaborationSessionId.equals(task.getCollaborationSessionId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findWaitingTasks(String workspaceId) {
        return store.values().stream()
                .filter(task -> workspaceId.equals(task.getWorkspaceId()))
                .filter(task -> task.getStatus() == TaskStatus.SUSPENDED
                        || task.getStatus() == TaskStatus.WAITING_USER_INPUT
                        || task.getStatus() == TaskStatus.WAITING_DEPENDENCY)
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(String id) {
        return store.containsKey(id);
    }
}
