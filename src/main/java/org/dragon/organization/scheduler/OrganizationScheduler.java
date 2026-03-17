package org.dragon.organization.scheduler;

import org.dragon.organization.Organization;
import org.dragon.organization.OrganizationRegistry;
import org.dragon.organization.communication.CollaborationSession;
import org.dragon.organization.communication.CommunicationService;
import org.dragon.organization.member.OrganizationMember;
import org.dragon.organization.member.MemberManagementService;
import org.dragon.organization.reward.RewardEngine;
import org.dragon.organization.task.*;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;

/**
 * OrganizationScheduler 组织调度器
 * 核心调度逻辑：任务分解、成员选择、任务分配
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrganizationScheduler {

    private final OrganizationRegistry organizationRegistry;
    private final MemberManagementService memberService;
    private final TaskDecomposer taskDecomposer;
    private final MemberSelector memberSelector;
    private final CommunicationService communicationService;
    private final RewardEngine rewardEngine;

    // 任务存储
    private final Map<String, OrganizationTask> tasks = new HashMap<>();
    private final Map<String, SubTask> subTasks = new HashMap<>();

    /**
     * 提交任务到组织
     *
     * @param organizationId 组织 ID
     * @param taskName 任务名称
     * @param taskDescription 任务描述
     * @param input 任务输入
     * @param creatorId 创建者 ID
     * @return 组织任务
     */
    public OrganizationTask submitTask(String organizationId, String taskName,
            String taskDescription, Object input, String creatorId) {
        // 验证组织存在
        Organization org = organizationRegistry.get(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + organizationId));

        // 创建任务
        OrganizationTask task = OrganizationTask.builder()
                .id(UUID.randomUUID().toString())
                .organizationId(organizationId)
                .name(taskName)
                .description(taskDescription)
                .input(input)
                .creatorId(creatorId)
                .status(OrgTaskStatus.SUBMITTED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        tasks.put(task.getId(), task);
        log.info("[OrganizationScheduler] Submitted task {} to organization {}", task.getId(), organizationId);

        // 自动开始处理任务
        processTask(task);

        return task;
    }

    /**
     * 处理任务
     */
    private void processTask(OrganizationTask task) {
        // 获取组织信息
        Organization org = organizationRegistry.get(task.getOrganizationId())
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        // 获取可用成员
        List<OrganizationMember> members = memberService.listMembers(task.getOrganizationId());
        if (members.isEmpty()) {
            log.warn("[OrganizationScheduler] No members available in organization {}", task.getOrganizationId());
            task.setStatus(OrgTaskStatus.FAILED);
            return;
        }

        // 1. 任务分解
        List<SubTask> decomposedSubTasks = taskDecomposer.decomposeWithReAct(task, org, members);
        // 保存子任务到本地存储
        for (SubTask st : decomposedSubTasks) {
            st.setOrganizationTaskId(task.getId());
            this.subTasks.put(st.getId(), st);
        }
        task.setSubTaskIds(decomposedSubTasks.stream().map(SubTask::getId).toList());
        task.setStatus(OrgTaskStatus.DECOMPOSED);

        // 2. 成员选择
        List<MemberSelector.SelectedMember> selectedMembers = memberSelector.selectWithLLM(
                task.getOrganizationId(), task, members);

        // 3. 创建协作会话
        List<String> participantIds = selectedMembers.stream()
                .map(MemberSelector.SelectedMember::getCharacterId)
                .toList();
        communicationService.createSession(
                task.getOrganizationId(), participantIds, task.getId());

        // 4. 分配任务
        assignSubTasks(decomposedSubTasks, selectedMembers, task);

        // 5. 执行任务
        executeSubTasks(decomposedSubTasks);
    }

    /**
     * 分配子任务
     */
    private void assignSubTasks(List<SubTask> subTasks, List<MemberSelector.SelectedMember> selectedMembers,
            OrganizationTask task) {
        task.setAssignedMemberIds(selectedMembers.stream()
                .map(MemberSelector.SelectedMember::getCharacterId)
                .toList());
        task.setStatus(OrgTaskStatus.ASSIGNED);

        for (int i = 0; i < subTasks.size(); i++) {
            SubTask subTask = subTasks.get(i);
            if (i < selectedMembers.size()) {
                MemberSelector.SelectedMember selected = selectedMembers.get(i);
                subTask.setCharacterId(selected.getCharacterId());
                subTask.setRole(selected.getRecommendedActions() != null && !selected.getRecommendedActions().isEmpty()
                        ? selected.getRecommendedActions().get(0) : "executor");
            }
            subTask.setStatus(OrgTaskStatus.ASSIGNED);
            subTask.setOrder(i);
            this.subTasks.put(subTask.getId(), subTask);
        }
    }

    /**
     * 执行子任务
     */
    private void executeSubTasks(List<SubTask> subTasks) {
        for (SubTask subTask : subTasks) {
            try {
                subTask.setStatus(OrgTaskStatus.RUNNING);
                subTask.setStartedAt(LocalDateTime.now());

                // TODO: 通过 TaskBridge 提交给 Character 执行
                // 这里模拟执行
                log.info("[OrganizationScheduler] Executing subTask {} on character {}",
                        subTask.getId(), subTask.getCharacterId());

                // 模拟执行完成
                subTask.setStatus(OrgTaskStatus.COMPLETED);
                subTask.setCompletedAt(LocalDateTime.now());
                subTask.setExecutionResult(SubTask.ExecutionResult.builder()
                        .success(true)
                        .result("Task completed")
                        .build());

                // 触发奖励评估
                rewardEngine.evaluateAndApply(
                        subTasks.get(0).getOrganizationTaskId().split("_")[0],
                        subTask.getCharacterId(),
                        RewardEngine.TriggerEvent.taskComplete(Map.of(
                                "taskId", subTask.getId(),
                                "success", true
                        ))
                );

            } catch (Exception e) {
                subTask.setStatus(OrgTaskStatus.FAILED);
                subTask.setExecutionResult(SubTask.ExecutionResult.builder()
                        .success(false)
                        .error(e.getMessage())
                        .build());

                // 触发惩罚评估
                log.error("[OrganizationScheduler] SubTask {} failed: {}", subTask.getId(), e.getMessage());
            }
        }

        // 检查所有子任务是否完成
        checkAndCompleteTask(subTasks.get(0).getOrganizationTaskId());
    }

    /**
     * 检查并完成任务
     */
    private void checkAndCompleteTask(String taskId) {
        OrganizationTask task = tasks.get(taskId);
        if (task == null) return;

        List<SubTask> taskSubTasks = task.getSubTaskIds().stream()
                .map(subTasks::get)
                .toList();

        boolean allCompleted = taskSubTasks.stream()
                .allMatch(st -> st.getStatus() == OrgTaskStatus.COMPLETED);
        boolean anyFailed = taskSubTasks.stream()
                .anyMatch(st -> st.getStatus() == OrgTaskStatus.FAILED);

        if (allCompleted) {
            task.setStatus(OrgTaskStatus.COMPLETED);
            task.setOutput("All sub-tasks completed successfully");
            log.info("[OrganizationScheduler] Task {} completed", taskId);
        } else if (anyFailed) {
            task.setStatus(OrgTaskStatus.FAILED);
            log.warn("[OrganizationScheduler] Task {} failed", taskId);
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
        log.info("[OrganizationScheduler] Rebalancing task {} with feedback: {}", taskId, feedback);
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
