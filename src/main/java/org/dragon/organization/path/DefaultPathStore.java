package org.dragon.organization.path;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * DefaultPathStore 默认路径存储实现
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class DefaultPathStore implements PathStore {

    private final Map<String, WorkflowPath> paths = new ConcurrentHashMap<>();

    @Override
    public void save(WorkflowPath path) {
        if (path == null || path.getId() == null) {
            throw new IllegalArgumentException("Path or Path id cannot be null");
        }
        paths.put(path.getId(), path);
    }

    @Override
    public WorkflowPath findById(String pathId) {
        return paths.get(pathId);
    }

    @Override
    public List<WorkflowPath> findByOrganizationId(String organizationId) {
        return paths.values().stream()
                .filter(p -> p.getOrganizationId().equals(organizationId))
                .collect(Collectors.toList());
    }

    @Override
    public List<WorkflowPath> findByOrganizationIdAndTaskType(String organizationId, String taskType) {
        return paths.values().stream()
                .filter(p -> p.getOrganizationId().equals(organizationId)
                        && p.getTaskType().equals(taskType))
                .collect(Collectors.toList());
    }

    @Override
    public List<WorkflowPath> findBestPractices(String organizationId) {
        return paths.values().stream()
                .filter(p -> p.getOrganizationId().equals(organizationId)
                        && p.isBestPractice())
                .collect(Collectors.toList());
    }

    @Override
    public void update(WorkflowPath path) {
        if (path == null || path.getId() == null) {
            throw new IllegalArgumentException("Path or Path id cannot be null");
        }
        paths.put(path.getId(), path);
    }

    @Override
    public void delete(String pathId) {
        paths.remove(pathId);
    }

    @Override
    public void deleteByOrganizationId(String organizationId) {
        List<String> keysToRemove = paths.keySet().stream()
                .filter(key -> paths.get(key).getOrganizationId().equals(organizationId))
                .collect(Collectors.toList());
        keysToRemove.forEach(paths::remove);
    }
}
