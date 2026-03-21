package org.dragon.workspace;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.workspace.actionlog.WorkspaceActionLog;
import org.dragon.workspace.hiring.HireMode;
import org.dragon.workspace.material.Material;
import org.dragon.workspace.member.CharacterDuty;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.workspace.service.WorkspaceActionLogService;
import org.dragon.workspace.service.WorkspaceHiringService;
import org.dragon.workspace.service.WorkspaceLifecycleService;
import org.dragon.workspace.service.WorkspaceMaterialService;
import org.dragon.workspace.service.WorkspaceMemberManagementService;
import org.dragon.task.Task;
import org.dragon.task.TaskStore;
import org.dragon.workspace.service.WorkspaceTaskArrangementService;
import org.dragon.workspace.service.WorkspaceTaskService;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * WorkspaceApplication Workspace 应用实例
 * 代表一个具体的 Workspace 实例，通过 Builder 构建
 * 提供对内所有 Workspace 服务的统一访问
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Getter
public class WorkspaceApplication {

    private final String workspaceId;
    private final WorkspaceLifecycleService workspaceLifecycleService;
    private final WorkspaceHiringService workspaceHiringService;
    private final WorkspaceActionLogService workspaceActionLogService;
    private final WorkspaceMemberManagementService workspaceMemberService;
    private final WorkspaceMaterialService materialService;
    private final WorkspaceTaskService workspaceTaskService;
    private final CharacterRegistry characterRegistry;
    private final WorkspaceTaskArrangementService workspaceTaskArrangementService;
    private final TaskStore taskStore;

    /**
     * 私有构造函数，通过 Builder 构建
     */
    WorkspaceApplication(WorkspaceApplicationBuilder builder) {
        this.workspaceId = builder.workspaceId;
        this.workspaceLifecycleService = builder.workspaceLifecycleService;
        this.workspaceHiringService = builder.workspaceHiringService;
        this.workspaceActionLogService = builder.workspaceActionLogService;
        this.workspaceMemberService = builder.workspaceMemberService;
        this.materialService = builder.materialService;
        this.workspaceTaskService = builder.workspaceTaskService;
        this.characterRegistry = builder.characterRegistry;
        this.workspaceTaskArrangementService = builder.workspaceTaskArrangementService;
        this.taskStore = builder.taskStore;
    }

    // ==================== Workspace 生命周期管理委托 ====================

    public Workspace createWorkspace(Workspace workspace) {
        return workspaceLifecycleService.createWorkspace(workspace);
    }

    public Workspace updateWorkspace(Workspace workspace) {
        return workspaceLifecycleService.updateWorkspace(workspace);
    }

    public void deleteWorkspace(String workspaceId) {
        workspaceLifecycleService.deleteWorkspace(workspaceId);
    }

    public Optional<Workspace> getWorkspace(String workspaceId) {
        return workspaceLifecycleService.getWorkspace(workspaceId);
    }

    public List<Workspace> listWorkspaces() {
        return workspaceLifecycleService.listWorkspaces();
    }

    public List<Workspace> listWorkspacesByStatus(Workspace.Status status) {
        return workspaceLifecycleService.listWorkspacesByStatus(status);
    }

    public void activateWorkspace(String workspaceId) {
        workspaceLifecycleService.activateWorkspace(workspaceId);
    }

    public void deactivateWorkspace(String workspaceId) {
        workspaceLifecycleService.deactivateWorkspace(workspaceId);
    }

    public void archiveWorkspace(String workspaceId) {
        workspaceLifecycleService.archiveWorkspace(workspaceId);
    }

    // ==================== 成员管理委托 ====================

    public WorkspaceMember addMember(String workspaceId, String characterId, String role) {
        return workspaceMemberService.addMember(workspaceId, characterId, role, WorkspaceMember.Layer.NORMAL);
    }

    public void removeMember(String workspaceId, String characterId) {
        workspaceMemberService.removeMember(workspaceId, characterId);
    }

    public List<WorkspaceMember> listMembers(String workspaceId) {
        return workspaceMemberService.listMembers(workspaceId);
    }

    // ==================== 雇佣管理委托 ====================

    public void hire(String workspaceId, String characterId, HireMode mode) {
        workspaceHiringService.hire(workspaceId, characterId, mode);
    }

    public void hire(String workspaceId, String characterId, HireMode mode, List<String> defaultCharacterIds) {
        workspaceHiringService.hire(workspaceId, characterId, mode, defaultCharacterIds);
    }

    public void fire(String workspaceId, String characterId, HireMode mode) {
        workspaceHiringService.fire(workspaceId, characterId, mode);
    }

    public Optional<Character> getHrCharacter(String workspaceId) {
        return workspaceHiringService.getHrCharacter(workspaceId);
    }

    public void setCharacterDuty(String workspaceId, String characterId, String dutyDescription) {
        workspaceHiringService.setCharacterDuty(workspaceId, characterId, dutyDescription);
    }

    public Optional<CharacterDuty> getCharacterDuty(String workspaceId, String characterId) {
        return workspaceHiringService.getCharacterDuty(workspaceId, characterId);
    }

    // ==================== 动作日志委托 ====================

    public List<WorkspaceActionLog> getActionLogs(String workspaceId) {
        return workspaceActionLogService.getActionLogs(workspaceId);
    }

    // ==================== 任务管理委托 ====================

    public Optional<Task> getTask(String workspaceId, String taskId) {
        return workspaceTaskService.getTask(workspaceId, taskId);
    }

    public String getTaskResult(String workspaceId, String taskId) {
        return workspaceTaskService.getTaskResult(workspaceId, taskId);
    }

    public Task cancelTask(String workspaceId, String taskId) {
        return workspaceTaskService.cancelTask(workspaceId, taskId);
    }

    public List<Task> listTasks(String workspaceId) {
        return workspaceTaskService.listTasks(workspaceId);
    }

    // ==================== 任务分发执行 ====================

    /**
     * 执行任务（通过 WorkspaceTaskArrangementService 进行智能编排）
     * 1. 通过 MemberSelector 选择合适的 Character
     * 2. 通过 ProjectManager 拆分任务为子任务
     * 3. 分派子任务给合适的 Character 执行
     *
     * @param taskName 任务名称
     * @param taskDescription 任务描述
     * @param input 任务输入
     * @param creatorId 创建者 ID
     * @return 工作空间任务
     */
    public Task executeTask(String taskName, String taskDescription, Object input, String creatorId) {
        return workspaceTaskArrangementService.submitTask(
                workspaceId, taskName, taskDescription, input, creatorId);
    }

    // ==================== 物料管理委托 ====================

    public Material uploadMaterial(String workspaceId, InputStream inputStream,
            String filename, long size, String contentType, String uploader) {
        return materialService.upload(workspaceId, inputStream, filename, size, contentType, uploader);
    }

    public Optional<Material> getMaterial(String workspaceId, String materialId) {
        return materialService.get(materialId);
    }

    public InputStream downloadMaterial(String workspaceId, String materialId) {
        return materialService.download(materialId);
    }

    public void deleteMaterial(String workspaceId, String materialId) {
        materialService.delete(materialId);
    }

    public List<Material> listMaterials(String workspaceId) {
        return materialService.listByWorkspace(workspaceId);
    }
}
