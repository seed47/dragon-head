package org.dragon.workspace;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.workspace.actionlog.WorkspaceActionLog;
import org.dragon.workspace.actionlog.WorkspaceActionLogStore;
import org.dragon.workspace.built_ins.character.hr.HrCharacterFactory;
import org.dragon.workspace.built_ins.character.hr.HrHiringExecutor;
import org.dragon.workspace.hiring.HireMode;
import org.dragon.workspace.material.Material;
import org.dragon.workspace.material.MaterialService;
import org.dragon.workspace.member.CharacterDuty;
import org.dragon.workspace.member.CharacterDutyStore;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.workspace.member.WorkspaceMemberManagementService;
import org.dragon.workspace.scheduler.WorkspaceScheduler;
import org.dragon.workspace.task.WorkspaceTask;
import org.dragon.workspace.task.WorkspaceTaskService;
import org.dragon.workspace.task.WorkspaceTaskStatus;
import org.dragon.workspace.task.WorkspaceTaskStore;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WorkspaceService 工作空间主服务
 * 提供工作空间的完整管理能力，包括雇佣管理
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRegistry workspaceRegistry;
    private final WorkspaceTaskService workspaceTaskService;
    private final WorkspaceTaskStore workspaceTaskStore;
    private final MaterialService materialService;
    private final CharacterRegistry characterRegistry;
    private final WorkspaceScheduler workspaceScheduler;
    private final WorkspaceMemberManagementService memberManagementService;

    // 雇佣管理相关依赖
    private final CharacterDutyStore characterDutyStore;
    private final WorkspaceActionLogStore actionLogStore;
    private final HrCharacterFactory hrCharacterFactory;
    private final HrHiringExecutor hrHiringExecutor;

    // ==================== Workspace 生命周期管理 ====================

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
        log.info("[WorkspaceService] Created workspace: {}", workspace.getId());

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
        log.info("[WorkspaceService] Updated workspace: {}", workspace.getId());

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
        log.info("[WorkspaceService] Deleted workspace: {}", workspaceId);
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
        log.info("[WorkspaceService] Activated workspace: {}", workspaceId);
    }

    /**
     * 停用工作空间
     *
     * @param workspaceId 工作空间 ID
     */
    public void deactivateWorkspace(String workspaceId) {
        workspaceRegistry.deactivate(workspaceId);
        log.info("[WorkspaceService] Deactivated workspace: {}", workspaceId);
    }

    /**
     * 归档工作空间
     *
     * @param workspaceId 工作空间 ID
     */
    public void archiveWorkspace(String workspaceId) {
        workspaceRegistry.archive(workspaceId);
        log.info("[WorkspaceService] Archived workspace: {}", workspaceId);
    }

    // ==================== 成员管理 ====================

    /**
     * 添加成员到工作空间
     *
     * @param workspaceId 工作空间 ID
     * @param characterId Character ID
     * @param role 角色
     * @return 添加的成员
     */
    public WorkspaceMember addMember(String workspaceId, String characterId, String role) {
        return memberManagementService.addMember(workspaceId, characterId, role, WorkspaceMember.Layer.NORMAL);
    }

    /**
     * 移除工作空间成员
     *
     * @param workspaceId 工作空间 ID
     * @param characterId Character ID
     */
    public void removeMember(String workspaceId, String characterId) {
        memberManagementService.removeMember(workspaceId, characterId);
    }

    /**
     * 获取工作空间成员列表
     *
     * @param workspaceId 工作空间 ID
     * @return 成员列表
     */
    public List<WorkspaceMember> listMembers(String workspaceId) {
        return memberManagementService.listMembers(workspaceId);
    }

    // ==================== 雇佣管理 ====================

    /**
     * 雇佣 Character 到 Workspace
     *
     * @param workspaceId 工作空间 ID
     * @param characterId 要雇佣的 Character ID
     * @param mode 雇佣模式
     */
    public void hire(String workspaceId, String characterId, HireMode mode) {
        hire(workspaceId, characterId, mode, null);
    }

    /**
     * 雇佣 Character 到 Workspace (指定默认 Character 池)
     *
     * @param workspaceId 工作空间 ID
     * @param characterId 要雇佣的 Character ID（AUTO 模式下可为 null，表示自动选择）
     * @param mode 雇佣模式
     * @param defaultCharacterIds 默认 Character 池
     */
    public void hire(String workspaceId, String characterId, HireMode mode, List<String> defaultCharacterIds) {
        // 验证 workspace 存在
        workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        switch (mode) {
            case DEFAULT:
            case MANUAL:
                // DEFAULT 和 MANUAL 模式需要指定 characterId
                if (characterId == null) {
                    throw new IllegalArgumentException("characterId is required for DEFAULT and MANUAL mode");
                }
                Character character = characterRegistry.get(characterId)
                        .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));
                if (mode == HireMode.DEFAULT) {
                    hireDefault(workspaceId, character, defaultCharacterIds);
                } else {
                    hireManual(workspaceId, character);
                }
                break;
            case AUTO:
                // AUTO 模式支持自动选择 Character
                hireAuto(workspaceId, characterId);
                break;
            default:
                throw new IllegalArgumentException("Unknown hire mode: " + mode);
        }
    }

    /**
     * 从 Workspace 解雇 Character
     *
     * @param workspaceId 工作空间 ID
     * @param characterId 要解雇的 Character ID
     * @param mode 雇佣模式
     */
    public void fire(String workspaceId, String characterId, HireMode mode) {
        // 验证 workspace 存在
        workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        switch (mode) {
            case DEFAULT:
            case MANUAL:
                fireManual(workspaceId, characterId);
                break;
            case AUTO:
                fireAuto(workspaceId, characterId);
                break;
            default:
                throw new IllegalArgumentException("Unknown hire mode: " + mode);
        }
    }

    // ==================== HR Character ====================

    /**
     * 获取 Workspace 的 HR Character
     *
     * @param workspaceId Workspace ID
     * @return HR Character (如果存在)
     */
    public Optional<Character> getHrCharacter(String workspaceId) {
        if (hrCharacterFactory.hasHrCharacter(workspaceId)) {
            return Optional.of(hrCharacterFactory.getOrCreateHrCharacter(workspaceId));
        }
        return Optional.empty();
    }

    // ==================== Character Duty 管理 ====================

    /**
     * 设置 Character 的职责描述
     *
     * @param workspaceId 工作空间 ID
     * @param characterId Character ID
     * @param dutyDescription 职责描述
     */
    public void setCharacterDuty(String workspaceId, String characterId, String dutyDescription) {
        // 验证 workspace 和 character 存在
        workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));
        characterRegistry.get(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

        String dutyId = CharacterDuty.createId(workspaceId, characterId);
        Optional<CharacterDuty> existing = characterDutyStore.findById(dutyId);

        LocalDateTime now = LocalDateTime.now();
        CharacterDuty duty;
        if (existing.isPresent()) {
            duty = existing.get();
            duty.setDutyDescription(dutyDescription);
            duty.setAutoGenerated(false);
            duty.setUpdatedAt(now);
            characterDutyStore.update(duty);
        } else {
            duty = CharacterDuty.builder()
                    .id(dutyId)
                    .workspaceId(workspaceId)
                    .characterId(characterId)
                    .dutyDescription(dutyDescription)
                    .autoGenerated(false)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            characterDutyStore.save(duty);
        }

        // 记录动作日志
        logAction(workspaceId, WorkspaceActionLog.ActionType.UPDATE_DUTY, characterId, "user",
                "Updated duty description: " + dutyDescription);

        log.info("[WorkspaceService] Set duty for character {} in workspace {}", characterId, workspaceId);
    }

    /**
     * 获取 Character 的职责描述
     *
     * @param workspaceId 工作空间 ID
     * @param characterId Character ID
     * @return Character Duty
     */
    public Optional<CharacterDuty> getCharacterDuty(String workspaceId, String characterId) {
        return characterDutyStore.findByWorkspaceIdAndCharacterId(workspaceId, characterId);
    }

    // ==================== 动作日志 ====================

    /**
     * 获取 Workspace 的动作日志
     *
     * @param workspaceId 工作空间 ID
     * @return 动作日志列表
     */
    public List<WorkspaceActionLog> getActionLogs(String workspaceId) {
        return actionLogStore.findByWorkspaceId(workspaceId);
    }

    // ==================== 私有方法：雇佣实现 ====================

    /**
     * 默认模式雇佣
     */
    private void hireDefault(String workspaceId, Character character, List<String> defaultCharacterIds) {
        // 如果指定了默认 Character 池，验证目标是否在池中
        if (defaultCharacterIds != null && !defaultCharacterIds.isEmpty()) {
            if (!defaultCharacterIds.contains(character.getId())) {
                throw new IllegalArgumentException("Character not in default pool: " + character.getId());
            }
        }

        // 添加成员
        memberManagementService.addMember(workspaceId, character.getId(), "MEMBER",
                WorkspaceMember.Layer.NORMAL);

        // 记录动作日志
        logAction(workspaceId, WorkspaceActionLog.ActionType.HIRE, character.getId(), "user",
                "Hired via DEFAULT mode");

        log.info("[WorkspaceService] Hired character {} to workspace {} via DEFAULT mode",
                character.getId(), workspaceId);
    }

    /**
     * 手动模式雇佣
     */
    private void hireManual(String workspaceId, Character character) {
        // 添加成员
        memberManagementService.addMember(workspaceId, character.getId(), "MEMBER",
                WorkspaceMember.Layer.NORMAL);

        // 记录动作日志
        logAction(workspaceId, WorkspaceActionLog.ActionType.HIRE, character.getId(), "user",
                "Hired via MANUAL mode");

        log.info("[WorkspaceService] Hired character {} to workspace {} via MANUAL mode",
                character.getId(), workspaceId);
    }

    /**
     * 自动模式雇佣 (HR Character)
     * @param workspaceId Workspace ID
     * @param characterId 要雇佣的 Character ID，如果为 null 则自动选择
     */
    private void hireAuto(String workspaceId, String characterId) {
        // 获取当前 workspace 的成员，排除已在其中的 Character
        List<String> currentMemberIds = memberManagementService.listMembers(workspaceId)
                .stream()
                .map(m -> m.getCharacterId())
                .toList();

        // 获取所有可用 Character
        List<Character> availableCharacters = characterRegistry.listAll().stream()
                .filter(c -> !currentMemberIds.contains(c.getId()))
                .filter(c -> c.getStatus() == Character.Status.RUNNING)
                .toList();

        if (availableCharacters.isEmpty()) {
            log.warn("[WorkspaceService] No available characters to hire in workspace {}", workspaceId);
            return;
        }

        // 调用 HrHiringExecutor 执行自动雇佣
        hrHiringExecutor.hireAuto(workspaceId, characterId, availableCharacters, (hiredCharacter) -> {
            // 执行实际的雇佣操作
            memberManagementService.addMember(workspaceId, hiredCharacter.getId(), "MEMBER",
                    WorkspaceMember.Layer.NORMAL);
            logAction(workspaceId, WorkspaceActionLog.ActionType.HIRE, hiredCharacter.getId(), "hr",
                    "Hired via AUTO mode with auto-selection");
            log.info("[WorkspaceService] Hired character {} to workspace {} via AUTO mode",
                    hiredCharacter.getId(), workspaceId);
        });
    }

    /**
     * 手动模式解雇
     */
    private void fireManual(String workspaceId, String characterId) {
        // 移除成员
        memberManagementService.removeMember(workspaceId, characterId);

        // 记录动作日志
        logAction(workspaceId, WorkspaceActionLog.ActionType.FIRE, characterId, "user",
                "Fired via MANUAL mode");

        log.info("[WorkspaceService] Fired character {} from workspace {} via MANUAL mode",
                characterId, workspaceId);
    }

    /**
     * 自动模式解雇 (HR Character)
     */
    private void fireAuto(String workspaceId, String characterId) {
        hrHiringExecutor.fireAuto(workspaceId, characterId, () -> {
            // 执行实际的解雇操作
            memberManagementService.removeMember(workspaceId, characterId);
            logAction(workspaceId, WorkspaceActionLog.ActionType.FIRE, characterId, "hr",
                    "Fired via AUTO mode");
        });

        log.info("[WorkspaceService] Fired character {} from workspace {} via AUTO mode",
                characterId, workspaceId);
    }

    /**
     * 记录动作日志
     */
    private void logAction(String workspaceId, WorkspaceActionLog.ActionType actionType,
                           String characterId, String operator, String details) {
        WorkspaceActionLog log = WorkspaceActionLog.builder()
                .id(UUID.randomUUID().toString())
                .workspaceId(workspaceId)
                .actionType(actionType)
                .targetCharacterId(characterId)
                .operator(operator)
                .details(details)
                .createdAt(LocalDateTime.now())
                .build();

        actionLogStore.save(log);
    }

    // ==================== 任务管理 ====================

    /**
     * 获取任务
     *
     * @param workspaceId 工作空间 ID
     * @param taskId 任务 ID
     * @return 任务
     */
    public Optional<WorkspaceTask> getTask(String workspaceId, String taskId) {
        return workspaceTaskService.getTask(workspaceId, taskId);
    }

    /**
     * 获取任务结果
     *
     * @param workspaceId 工作空间 ID
     * @param taskId 任务 ID
     * @return 任务结果
     */
    public String getTaskResult(String workspaceId, String taskId) {
        return workspaceTaskService.getTaskResult(workspaceId, taskId);
    }

    /**
     * 取消任务
     *
     * @param workspaceId 工作空间 ID
     * @param taskId 任务 ID
     * @return 更新后的任务
     */
    public WorkspaceTask cancelTask(String workspaceId, String taskId) {
        return workspaceTaskService.cancelTask(workspaceId, taskId);
    }

    /**
     * 获取工作空间的任务列表
     *
     * @param workspaceId 工作空间 ID
     * @return 任务列表
     */
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
        log.info("[WorkspaceService] Executed instant task for character: {}", characterId);

        return result;
    }

    /**
     * 分发给 Character 执行任务
     *
     * @param task 任务
     */
    private void dispatchToCharacter(WorkspaceTask task) {
        String characterId = task.getExecutorId();

        // 更新任务状态为 RUNNING
        task.setStatus(WorkspaceTaskStatus.RUNNING);
        task.setStartedAt(LocalDateTime.now());
        workspaceTaskStore.update(task);

        // 获取 Character 并执行任务
        Character character = characterRegistry.get(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

        // 执行任务（异步）
        // 这里使用 addTaskAndRun 异步执行
        try {
            String taskInput = task.getInput() != null ? task.getInput().toString() : "";
            org.dragon.character.task.Task characterTask = character.addTaskAndRun(taskInput);
            // 保存 Character 任务 ID
            task.setInternalTaskId(characterTask.getId());
            task.setStatus(WorkspaceTaskStatus.RUNNING);
            workspaceTaskStore.update(task);
            log.info("[WorkspaceService] Task dispatched to character: {} -> taskId: {}", characterId, characterTask.getId());
        } catch (Exception e) {
            log.error("[WorkspaceService] Task failed: {}", task.getId(), e);
            task.setErrorMessage(e.getMessage());
            task.setStatus(WorkspaceTaskStatus.FAILED);
            task.setCompletedAt(LocalDateTime.now());
            workspaceTaskStore.update(task);
        }
    }

    // ==================== 物料管理 ====================

    /**
     * 上传物料
     *
     * @param workspaceId 工作空间 ID
     * @param inputStream 输入流
     * @param filename 文件名
     * @param size 文件大小
     * @param contentType 内容类型
     * @param uploader 上传者 ID
     * @return 物料
     */
    public Material uploadMaterial(String workspaceId, java.io.InputStream inputStream,
                                   String filename, long size, String contentType, String uploader) {
        return materialService.upload(workspaceId, inputStream, filename, size, contentType, uploader);
    }

    /**
     * 获取物料
     *
     * @param workspaceId 工作空间 ID
     * @param materialId 物料 ID
     * @return 物料
     */
    public Optional<Material> getMaterial(String workspaceId, String materialId) {
        return materialService.get(materialId);
    }

    /**
     * 下载物料
     *
     * @param workspaceId 工作空间 ID
     * @param materialId 物料 ID
     * @return 输入流
     */
    public java.io.InputStream downloadMaterial(String workspaceId, String materialId) {
        return materialService.download(materialId);
    }

    /**
     * 删除物料
     *
     * @param workspaceId 工作空间 ID
     * @param materialId 物料 ID
     */
    public void deleteMaterial(String workspaceId, String materialId) {
        materialService.delete(materialId);
    }

    /**
     * 获取工作空间的物料列表
     *
     * @param workspaceId 工作空间 ID
     * @return 物料列表
     */
    public List<Material> listMaterials(String workspaceId) {
        return materialService.listByWorkspace(workspaceId);
    }

}
