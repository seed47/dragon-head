package org.dragon.workspace.actionlog;

import java.util.List;

/**
 * Workspace Action Log 存储接口
 *
 * @author wyj
 * @version 1.0
 */
public interface WorkspaceActionLogStore {

    /**
     * 保存动作日志
     */
    void save(WorkspaceActionLog log);

    /**
     * 根据 workspaceId 查询所有日志
     */
    List<WorkspaceActionLog> findByWorkspaceId(String workspaceId);

    /**
     * 根据 workspaceId 和动作类型查询
     */
    List<WorkspaceActionLog> findByWorkspaceIdAndActionType(String workspaceId, WorkspaceActionLog.ActionType actionType);

    /**
     * 根据 workspaceId 和 Character 查询
     */
    List<WorkspaceActionLog> findByWorkspaceIdAndCharacterId(String workspaceId, String characterId);
}
