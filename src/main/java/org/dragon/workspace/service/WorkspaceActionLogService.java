package org.dragon.workspace.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.dragon.workspace.actionlog.WorkspaceActionLog;
import org.dragon.workspace.actionlog.WorkspaceActionLogStore;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WorkspaceActionLogService Workspace 行为日志服务
 * 统一记录 Workspace 内所有行为（Character 行为、Character 间交互、任务行为、雇佣行为等）
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceActionLogService {

    private final WorkspaceActionLogStore actionLogStore;

    /**
     * 记录动作日志
     *
     * @param workspaceId Workspace ID
     * @param actionType 动作类型
     * @param targetCharacterId 目标 Character ID
     * @param operator 操作者 (user / hr)
     * @param details 详情
     */
    public void logAction(String workspaceId, WorkspaceActionLog.ActionType actionType,
            String targetCharacterId, String operator, String details) {
        WorkspaceActionLog actionLog = WorkspaceActionLog.builder()
                .id(UUID.randomUUID().toString())
                .workspaceId(workspaceId)
                .actionType(actionType)
                .targetCharacterId(targetCharacterId)
                .operator(operator)
                .details(details)
                .createdAt(LocalDateTime.now())
                .build();

        actionLogStore.save(actionLog);
        log.info("[WorkspaceActionLogService] Logged action: {} for character: {} in workspace: {}",
                actionType.name(), targetCharacterId, workspaceId);
    }

    /**
     * 获取 Workspace 的所有动作日志
     *
     * @param workspaceId Workspace ID
     * @return 动作日志列表
     */
    public List<WorkspaceActionLog> getActionLogs(String workspaceId) {
        return actionLogStore.findByWorkspaceId(workspaceId);
    }

    /**
     * 根据 Character 获取动作日志
     *
     * @param workspaceId Workspace ID
     * @param characterId Character ID
     * @return 动作日志列表
     */
    public List<WorkspaceActionLog> getActionLogsByCharacter(String workspaceId, String characterId) {
        return actionLogStore.findByWorkspaceIdAndCharacterId(workspaceId, characterId);
    }

    /**
     * 根据动作类型获取动作日志
     *
     * @param workspaceId Workspace ID
     * @param actionType 动作类型
     * @return 动作日志列表
     */
    public List<WorkspaceActionLog> getActionLogsByType(String workspaceId, WorkspaceActionLog.ActionType actionType) {
        return actionLogStore.findByWorkspaceIdAndActionType(workspaceId, actionType);
    }
}
