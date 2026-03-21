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
import org.dragon.workspace.scheduler.WorkspaceScheduler;
import org.dragon.workspace.service.WorkspaceActionLogService;
import org.dragon.workspace.service.WorkspaceHiringService;
import org.dragon.workspace.service.WorkspaceLifecycleService;
import org.dragon.workspace.service.WorkspaceMaterialService;
import org.dragon.workspace.service.WorkspaceMemberManagementService;
import org.dragon.workspace.service.WorkspaceTaskService;
import org.dragon.workspace.task.WorkspaceTask;
import org.dragon.workspace.task.WorkspaceTaskStore;
import org.dragon.workspace.task.WorkspaceTaskStatus;

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
    private final WorkspaceScheduler workspaceScheduler;
    private final WorkspaceTaskStore workspaceTaskStore;

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
        this.workspaceScheduler = builder.workspaceScheduler;
        this.workspaceTaskStore = builder.workspaceTaskStore;
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

    public Optional<WorkspaceTask> getTask(String workspaceId, String taskId) {
        return workspaceTaskService.getTask(workspaceId, taskId);
    }

    public String getTaskResult(String workspaceId, String taskId) {
        return workspaceTaskService.getTaskResult(workspaceId, taskId);
    }

    public WorkspaceTask cancelTask(String workspaceId, String taskId) {
        return workspaceTaskService.cancelTask(workspaceId, taskId);
    }

    public List<WorkspaceTask> listTasks(String workspaceId) {
        return workspaceTaskService.listTasks(workspaceId);
    }

    // ==================== 任务分发执行 ====================

    /**
     * 执行即时任务（用于即时聊天等场景）
     * 直接分发给 Character 执行
     *
     * @param characterId Character ID
     * @param userInput 用户输入
     * @return 执行结果
     */
    public String executeInstantTask(String characterId, String userInput) {
        // 获取 Character
        Character character = characterRegistry.get(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

        // 直接调用 Character 执行
        String result = character.run(userInput);
        log.info("[WorkspaceApplication] Executed instant task for character: {}", characterId);

        return result;
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

    // ==================== 任务分发 ====================

    /**
     * 分发给 Character 执行任务
     */
    private void dispatchToCharacter(WorkspaceTask task) {
        String characterId = task.getExecutorId();

        // 更新任务状态为 RUNNING
        task.setStatus(WorkspaceTaskStatus.RUNNING);
        task.setStartedAt(java.time.LocalDateTime.now());
        workspaceTaskStore.update(task);

        // 获取 Character 并执行任务
        Character character = characterRegistry.get(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

        // 执行任务（异步）
        try {
            String taskInput = task.getInput() != null ? task.getInput().toString() : "";
            org.dragon.character.task.Task characterTask = character.addTaskAndRun(taskInput);
            // 保存 Character 任务 ID
            task.setInternalTaskId(characterTask.getId());
            task.setStatus(WorkspaceTaskStatus.RUNNING);
            workspaceTaskStore.update(task);
            log.info("[WorkspaceApplication] Task dispatched to character: {} -> taskId: {}", characterId, characterTask.getId());
        } catch (Exception e) {
            log.error("[WorkspaceApplication] Task failed: {}", task.getId(), e);
            task.setErrorMessage(e.getMessage());
            task.setStatus(WorkspaceTaskStatus.FAILED);
            task.setCompletedAt(java.time.LocalDateTime.now());
            workspaceTaskStore.update(task);
        }
    }
}
