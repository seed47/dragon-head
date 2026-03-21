package org.dragon.workspace.actionlog;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * Workspace Action Log 内存存储实现
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class MemoryWorkspaceActionLogStore implements WorkspaceActionLogStore {

    private final List<WorkspaceActionLog> logs = new CopyOnWriteArrayList<>();

    @Override
    public void save(WorkspaceActionLog log) {
        logs.add(log);
    }

    @Override
    public List<WorkspaceActionLog> findByWorkspaceId(String workspaceId) {
        return logs.stream()
                .filter(log -> workspaceId.equals(log.getWorkspaceId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<WorkspaceActionLog> findByWorkspaceIdAndActionType(String workspaceId, WorkspaceActionLog.ActionType actionType) {
        return logs.stream()
                .filter(log -> workspaceId.equals(log.getWorkspaceId())
                        && actionType == log.getActionType())
                .collect(Collectors.toList());
    }

    @Override
    public List<WorkspaceActionLog> findByWorkspaceIdAndCharacterId(String workspaceId, String characterId) {
        return logs.stream()
                .filter(log -> workspaceId.equals(log.getWorkspaceId())
                        && characterId.equals(log.getTargetCharacterId()))
                .collect(Collectors.toList());
    }
}
