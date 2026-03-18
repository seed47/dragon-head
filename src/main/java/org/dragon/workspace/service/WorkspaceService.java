package org.dragon.workspace.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.organization.OrganizationRegistry;
import org.dragon.organization.scheduler.OrganizationScheduler;
import org.dragon.workspace.Workspace;
import org.dragon.workspace.WorkspaceRegistry;
import org.dragon.workspace.context.ExecutionContext;
import org.dragon.workspace.hiring.Candidate;
import org.dragon.workspace.hiring.HiringRequest;
import org.dragon.workspace.hiring.HiringService;
import org.dragon.workspace.hiring.HiringRecord;
import org.dragon.workspace.hiring.LLMHiringEngine;
import org.dragon.workspace.material.Material;
import org.dragon.workspace.material.MaterialService;
import org.dragon.workspace.task.WorkspaceTask;
import org.dragon.workspace.task.WorkspaceTaskService;
import org.dragon.workspace.task.WorkspaceTaskStatus;
import org.dragon.workspace.task.WorkspaceTaskStore;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WorkspaceService 工作空间主服务
 * 提供工作空间的完整管理能力
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRegistry workspaceRegistry;
    private final HiringService hiringService;
    private final WorkspaceTaskService workspaceTaskService;
    private final WorkspaceTaskStore workspaceTaskStore;
    private final MaterialService materialService;
    private final CharacterRegistry characterRegistry;
    private final OrganizationRegistry organizationRegistry;
    private final OrganizationScheduler organizationScheduler;
    private final LLMHiringEngine llmHiringEngine;

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

    // ==================== 雇佣请求管理 ====================

    /**
     * 发布雇佣请求
     *
     * @param workspaceId 工作空间 ID
     * @param request 雇佣请求
     * @return 雇佣请求
     */
    public HiringRequest publishHiringRequest(String workspaceId, HiringRequest request) {
        return hiringService.submitHiringRequest(workspaceId, request);
    }

    /**
     * 获取雇佣请求
     *
     * @param workspaceId 工作空间 ID
     * @param hireId 雇佣请求 ID
     * @return 雇佣请求
     */
    public Optional<HiringRequest> getHiringRequest(String workspaceId, String hireId) {
        return hiringService.getHiringRequest(workspaceId, hireId);
    }

    /**
     * 获取雇佣请求列表
     *
     * @param workspaceId 工作空间 ID
     * @return 雇佣请求列表
     */
    public List<HiringRequest> listHiringRequests(String workspaceId) {
        return hiringService.listHiringRequests(workspaceId);
    }

    /**
     * 获取雇佣记录
     *
     * @param workspaceId 工作空间 ID
     * @param hireId 雇佣请求 ID
     * @return 雇佣记录列表
     */
    public List<HiringRecord> getHiringRecords(String workspaceId, String hireId) {
        // 验证工作空间
        workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        return hiringService.getHiringRecords(hireId);
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
     * 根据雇佣请求分发所有任务
     * 将任务分发给 Character 或 Organization 执行
     *
     * @param workspaceId 工作空间 ID
     * @param hireId 雇佣请求 ID
     */
    public void dispatchTasksByHiringRequest(String workspaceId, String hireId) {
        // 验证工作空间
        workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        // 获取雇佣记录
        List<HiringRecord> records = hiringService.getHiringRecords(hireId);
        HiringRequest request = hiringService.getHiringRequest(workspaceId, hireId)
                .orElseThrow(() -> new IllegalArgumentException("Hiring request not found: " + hireId));

        // 为每个被录用的候选人创建任务并分发
        for (HiringRecord record : records) {
            if (record.getDecision() == HiringRecord.Decision.ACCEPTED) {
                dispatchTask(workspaceId, request, record);
            }
        }

        log.info("[WorkspaceService] Dispatched tasks for hiring request: {}", hireId);
    }

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
     * 分发单个任务
     *
     * @param workspaceId 工作空间 ID
     * @param request 雇佣请求
     * @param record 雇佣记录
     */
    private void dispatchTask(String workspaceId, HiringRequest request, HiringRecord record) {
        // 构建候选人信息
        Candidate candidate = Candidate.builder()
                .id(record.getCandidateId())
                .type(record.getCandidateType() == HiringRecord.CandidateType.CHARACTER ?
                        Candidate.Type.CHARACTER : Candidate.Type.ORGANIZATION)
                .matchScore(record.getMatchScore())
                .build();

        // 获取执行者名称
        if (candidate.getType() == Candidate.Type.CHARACTER) {
            characterRegistry.get(candidate.getId()).ifPresent(c -> candidate.setName(c.getName()));
        } else {
            organizationRegistry.get(candidate.getId()).ifPresent(o -> candidate.setName(o.getName()));
        }

        // 创建 WorkspaceTask
        WorkspaceTask task = WorkspaceTask.builder()
                .id(UUID.randomUUID().toString())
                .workspaceId(workspaceId)
                .hiringRequestId(request.getId())
                .hiringRecordId(record.getId())
                .executorType(record.getCandidateType() == HiringRecord.CandidateType.CHARACTER ?
                        WorkspaceTask.ExecutorType.CHARACTER : WorkspaceTask.ExecutorType.ORGANIZATION)
                .executorId(record.getCandidateId())
                .name(request.getWorkDescription())
                .description(request.getWorkDescription())
                .status(WorkspaceTaskStatus.PENDING)
                .input(request.getWorkDescription())
                .createdAt(LocalDateTime.now())
                .build();

        workspaceTaskStore.save(task);
        log.info("[WorkspaceService] Created task: {} for executor: {}", task.getId(), candidate.getId());

        // 构建执行上下文
        ExecutionContext context = llmHiringEngine.buildExecutionContext(request, candidate);
        context.setTaskId(task.getId());
        context.setWorkspaceId(workspaceId);

        // 根据执行者类型分发任务
        if (candidate.getType() == Candidate.Type.CHARACTER) {
            dispatchToCharacter(task, context);
        } else {
            dispatchToOrganization(task, context);
        }
    }

    /**
     * 分发给 Character 执行
     */
    private void dispatchToCharacter(WorkspaceTask task, ExecutionContext context) {
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
            org.dragon.character.task.Task characterTask = character.addTaskAndRun(context.getExecutionPrompt());
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

    /**
     * 分发给 Organization 执行
     */
    private void dispatchToOrganization(WorkspaceTask task, ExecutionContext context) {
        String organizationId = task.getExecutorId();

        // 更新任务状态为 RUNNING
        task.setStatus(WorkspaceTaskStatus.RUNNING);
        task.setStartedAt(LocalDateTime.now());
        workspaceTaskStore.update(task);

        // 提交给 Organization 执行
        try {
            // 使用 OrganizationScheduler 提交任务
            var orgTask = organizationScheduler.submitTask(
                    organizationId,
                    task.getName(),
                    task.getDescription(),
                    context.getExecutionPrompt(),
                    "workspace" // creatorId
            );

            // 保存内部任务 ID
            task.setInternalTaskId(orgTask.getId());
            workspaceTaskStore.update(task);

            log.info("[WorkspaceService] Task submitted to organization: {} -> {}", task.getId(), organizationId);
        } catch (Exception e) {
            log.error("[WorkspaceService] Failed to submit task to organization: {}", task.getId(), e);
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
