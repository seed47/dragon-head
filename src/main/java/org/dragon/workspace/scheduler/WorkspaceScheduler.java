package org.dragon.workspace.scheduler;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.dragon.workspace.Workspace;
import org.dragon.workspace.WorkspaceRegistry;
import org.dragon.workspace.chat.ChatRoom;
import org.dragon.workspace.chat.ChatSession;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.workspace.service.WorkspaceMemberManagementService;
import org.dragon.workspace.task.SubTask;
import org.dragon.workspace.task.WorkspaceTask;
import org.dragon.workspace.task.WorkspaceTaskStatus;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WorkspaceScheduler 工作空间调度器
 * 核心调度逻辑：任务分解、成员选择、任务分配
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkspaceScheduler {

    private final WorkspaceRegistry workspaceRegistry;
    private final WorkspaceMemberManagementService memberService;
    private final TaskDecomposer taskDecomposer;
    private final MemberSelector memberSelector;
    private final ChatRoom chatRoom;

    // 任务存储
    private final Map<String, WorkspaceTask> tasks = new ConcurrentHashMap<>();
    private final Map<String, SubTask> subTasks = new ConcurrentHashMap<>();

    /**
     * 提交任务到工作空间
     *
     * @param workspaceId 工作空间 ID
     * @param taskName 任务名称
     * @param taskDescription 任务描述
     * @param input 任务输入
     * @param creatorId 创建者 ID
     * @return 工作空间任务
     */
    public WorkspaceTask submitTask(String workspaceId, String taskName,
            String taskDescription, Object input, String creatorId) {
        // 验证工作空间存在
        Workspace workspace = workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        // 创建任务
        WorkspaceTask task = WorkspaceTask.builder()
                .id(UUID.randomUUID().toString())
                .workspaceId(workspaceId)
                .name(taskName)
                .description(taskDescription)
                .input(input)
                .creatorId(creatorId)
                .status(WorkspaceTaskStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        tasks.put(task.getId(), task);
        log.info("[WorkspaceScheduler] Submitted task {} to workspace {}", task.getId(), workspaceId);

        // 自动开始处理任务
        processTask(task, workspace);

        return task;
    }

    /**
     * 处理任务
     */
    private void processTask(WorkspaceTask task, Workspace workspace) {
        // 获取工作空间成员
        List<WorkspaceMember> members = memberService.listMembers(task.getWorkspaceId());
        if (members.isEmpty()) {
            log.warn("[WorkspaceScheduler] No members available in workspace {}", task.getWorkspaceId());
            task.setStatus(WorkspaceTaskStatus.FAILED);
            return;
        }

        // 1. 任务分解
        List<SubTask> decomposedSubTasks = taskDecomposer.decomposeWithReAct(task, workspace, members);
        // 保存子任务到本地存储
        for (SubTask st : decomposedSubTasks) {
            st.setWorkspaceTaskId(task.getId());
            this.subTasks.put(st.getId(), st);
        }
        task.setSubTaskIds(decomposedSubTasks.stream().map(SubTask::getId).toList());
        task.setStatus(WorkspaceTaskStatus.RUNNING);

        // 2. 成员选择
        List<MemberSelector.SelectedMember> selectedMembers = memberSelector.selectWithLLM(
                task.getWorkspaceId(), task, members);

        // 3. 创建协作会话
        List<String> participantIds = selectedMembers.stream()
                .map(MemberSelector.SelectedMember::getCharacterId)
                .toList();
        ChatSession session = chatRoom.createSession(
                task.getWorkspaceId(), participantIds, task.getId());
        task.setCollaborationSessionId(session.getId());

        // 4. 分配任务
        assignSubTasks(decomposedSubTasks, selectedMembers, task);

        // 5. 执行任务
        executeSubTasks(decomposedSubTasks, task);
    }

    /**
     * 分配子任务
     */
    private void assignSubTasks(List<SubTask> subTasks, List<MemberSelector.SelectedMember> selectedMembers,
            WorkspaceTask task) {
        task.setAssignedMemberIds(selectedMembers.stream()
                .map(MemberSelector.SelectedMember::getCharacterId)
                .toList());
        task.setStatus(WorkspaceTaskStatus.RUNNING);

        for (int i = 0; i < subTasks.size(); i++) {
            SubTask subTask = subTasks.get(i);
            if (i < selectedMembers.size()) {
                MemberSelector.SelectedMember selected = selectedMembers.get(i);
                subTask.setCharacterId(selected.getCharacterId());
                subTask.setRole(selected.getRecommendedActions() != null && !selected.getRecommendedActions().isEmpty()
                        ? selected.getRecommendedActions().get(0) : "executor");
            }
            subTask.setStatus(WorkspaceTaskStatus.RUNNING);
            subTask.setOrder(i);
            this.subTasks.put(subTask.getId(), subTask);
        }
    }

    /**
     * 执行子任务
     */
    private void executeSubTasks(List<SubTask> subTasks, WorkspaceTask task) {
        for (SubTask subTask : subTasks) {
            try {
                subTask.setStatus(WorkspaceTaskStatus.RUNNING);
                subTask.setStartedAt(LocalDateTime.now());

                // TODO: 通过 TaskBridge 提交给 Character 执行
                // 这里模拟执行
                log.info("[WorkspaceScheduler] Executing subTask {} on character {}",
                        subTask.getId(), subTask.getCharacterId());

                // 模拟执行完成
                subTask.setStatus(WorkspaceTaskStatus.COMPLETED);
                subTask.setCompletedAt(LocalDateTime.now());
                subTask.setExecutionResult(SubTask.ExecutionResult.builder()
                        .success(true)
                        .result("Task completed")
                        .build());

            } catch (Exception e) {
                subTask.setStatus(WorkspaceTaskStatus.FAILED);
                subTask.setExecutionResult(SubTask.ExecutionResult.builder()
                        .success(false)
                        .error(e.getMessage())
                        .build());

                log.error("[WorkspaceScheduler] SubTask {} failed: {}", subTask.getId(), e.getMessage());
            }
        }

        // 检查所有子任务是否完成
        checkAndCompleteTask(task);
    }

    /**
     * 检查并完成任务
     */
    private void checkAndCompleteTask(WorkspaceTask task) {
        List<SubTask> taskSubTasks = task.getSubTaskIds().stream()
                .map(subTasks::get)
                .toList();

        boolean allCompleted = taskSubTasks.stream()
                .allMatch(st -> st.getStatus() == WorkspaceTaskStatus.COMPLETED);
        boolean anyFailed = taskSubTasks.stream()
                .anyMatch(st -> st.getStatus() == WorkspaceTaskStatus.FAILED);

        if (allCompleted) {
            task.setStatus(WorkspaceTaskStatus.COMPLETED);
            task.setOutput("All sub-tasks completed successfully");
            log.info("[WorkspaceScheduler] Task {} completed", task.getId());
        } else if (anyFailed) {
            task.setStatus(WorkspaceTaskStatus.FAILED);
            log.warn("[WorkspaceScheduler] Task {} failed", task.getId());
        }
    }

    /**
     * 重新平衡任务
     *
     * @param taskId 任务 ID
     * @param feedback 执行反馈
     */
    public void rebalance(String taskId, ExecutionFeedback feedback) {
        // TODO: 实现动态调整逻辑
        log.info("[WorkspaceScheduler] Rebalancing task {} with feedback: {}", taskId, feedback);
    }

    /**
     * ExecutionFeedback 执行反馈
     */
    public static class ExecutionFeedback {
        private String subTaskId;
        private boolean success;
        private String errorMessage;
        private long durationMs;

        public ExecutionFeedback(String subTaskId, boolean success, String errorMessage, long durationMs) {
            this.subTaskId = subTaskId;
            this.success = success;
            this.errorMessage = errorMessage;
            this.durationMs = durationMs;
        }

        public String getSubTaskId() { return subTaskId; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public long getDurationMs() { return durationMs; }
    }
}
