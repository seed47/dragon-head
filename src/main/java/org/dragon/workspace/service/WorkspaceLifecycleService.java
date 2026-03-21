package org.dragon.workspace.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.dragon.workspace.Workspace;
import org.dragon.workspace.WorkspaceRegistry;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WorkspaceLifecycleService 工作空间生命周期服务
 * 管理工作空间的创建、更新、删除、激活、停用、归档等
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceLifecycleService {

    private final WorkspaceRegistry workspaceRegistry;

    /**
     * 创建工作空间
     *
     * @param workspace 工作空间
     * @return 创建后的工作空间
     */
    public Workspace createWorkspace(Workspace workspace) {
        if (workspace.getId() == null || workspace.getId().isEmpty()) {
            workspace.setId(UUID.randomUUID().toString());
        }
        workspace.setCreatedAt(LocalDateTime.now());
        workspace.setUpdatedAt(LocalDateTime.now());

        if (workspace.getStatus() == null) {
            workspace.setStatus(Workspace.Status.INACTIVE);
        }

        workspaceRegistry.register(workspace);
        log.info("[WorkspaceLifecycleService] Created workspace: {}", workspace.getId());

        return workspace;
    }

    /**
     * 更新工作空间
     *
     * @param workspace 工作空间
     * @return 更新后的工作空间
     */
    public Workspace updateWorkspace(Workspace workspace) {
        workspaceRegistry.get(workspace.getId())
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspace.getId()));

        workspace.setUpdatedAt(LocalDateTime.now());
        workspaceRegistry.update(workspace);
        log.info("[WorkspaceLifecycleService] Updated workspace: {}", workspace.getId());

        return workspace;
    }

    /**
     * 删除工作空间
     *
     * @param workspaceId 工作空间 ID
     */
    public void deleteWorkspace(String workspaceId) {
        workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        workspaceRegistry.unregister(workspaceId);
        log.info("[WorkspaceLifecycleService] Deleted workspace: {}", workspaceId);
    }

    /**
     * 获取工作空间
     *
     * @param workspaceId 工作空间 ID
     * @return 工作空间
     */
    public Optional<Workspace> getWorkspace(String workspaceId) {
        return workspaceRegistry.get(workspaceId);
    }

    /**
     * 获取所有工作空间
     *
     * @return 工作空间列表
     */
    public List<Workspace> listWorkspaces() {
        return workspaceRegistry.listAll();
    }

    /**
     * 根据状态获取工作空间
     *
     * @param status 工作空间状态
     * @return 工作空间列表
     */
    public List<Workspace> listWorkspacesByStatus(Workspace.Status status) {
        return workspaceRegistry.listByStatus(status);
    }

    /**
     * 激活工作空间
     *
     * @param workspaceId 工作空间 ID
     */
    public void activateWorkspace(String workspaceId) {
        workspaceRegistry.activate(workspaceId);
        log.info("[WorkspaceLifecycleService] Activated workspace: {}", workspaceId);
    }

    /**
     * 停用工作空间
     *
     * @param workspaceId 工作空间 ID
     */
    public void deactivateWorkspace(String workspaceId) {
        workspaceRegistry.deactivate(workspaceId);
        log.info("[WorkspaceLifecycleService] Deactivated workspace: {}", workspaceId);
    }

    /**
     * 归档工作空间
     *
     * @param workspaceId 工作空间 ID
     */
    public void archiveWorkspace(String workspaceId) {
        workspaceRegistry.archive(workspaceId);
        log.info("[WorkspaceLifecycleService] Archived workspace: {}", workspaceId);
    }
}
