package org.dragon.workspace.service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.dragon.agent.llm.util.CharacterCaller;
import org.dragon.character.Character;
import org.dragon.config.PromptKeys;
import org.dragon.config.PromptManager;
import org.dragon.task.Task;
import org.dragon.task.TaskStore;
import org.dragon.task.TaskStatus;
import org.dragon.workspace.Workspace;
import org.dragon.workspace.WorkspaceRegistry;
import org.dragon.workspace.built_ins.character.member_selector.MemberSelectorCharacterFactory;
import org.dragon.workspace.built_ins.character.project_manager.ProjectManagerCharacterFactory;
import org.dragon.workspace.chat.ChatRoom;
import org.dragon.workspace.chat.ChatSession;
import org.dragon.workspace.member.WorkspaceMember;
import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WorkspaceTaskArrangementService 工作空间任务编排服务
 * 核心编排逻辑：任务分解、成员选择、任务分配
 * 通过 MemberSelector Character 和 ProjectManager Character 实现智能编排
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceTaskArrangementService {

    private final WorkspaceRegistry workspaceRegistry;
    private final WorkspaceMemberManagementService memberService;
    private final ChatRoom chatRoom;
    private final CharacterCaller characterCaller;
    private final PromptManager promptManager;
    private final MemberSelectorCharacterFactory memberSelectorCharacterFactory;
    private final ProjectManagerCharacterFactory projectManagerCharacterFactory;
    private final TaskStore taskStore;

    /**
     * 任务执行模式枚举
     */
    @Getter
    public enum TaskExecutionMode {
        /**
         * 自动选择 Character 执行
         */
        AUTO,
        /**
         * 使用指定的 Character 执行
         */
        SPECIFIED,
        /**
         * 使用默认 Character 执行
         */
        DEFAULT
    }

    /**
     * 提交任务到工作空间
     *
     * @param workspaceId 工作空间 ID
     * @param taskName 任务名称
     * @param taskDescription 任务描述
     * @param input 任务输入
     * @param creatorId 创建者 ID
     * @param executionMode 执行模式
     * @param specifiedCharacterIds 指定的 Character ID 列表（当 executionMode 为 SPECIFIED 时使用）
     * @return 工作空间任务
     */
    public Task submitTask(String workspaceId, String taskName,
            String taskDescription, Object input, String creatorId,
            TaskExecutionMode executionMode, List<String> specifiedCharacterIds) {
        // 验证工作空间存在
        Workspace workspace = workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        // 创建任务
        Task task = Task.builder()
                .id(UUID.randomUUID().toString())
                .workspaceId(workspaceId)
                .name(taskName)
                .description(taskDescription)
                .input(input)
                .creatorId(creatorId)
                .status(TaskStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 持久化任务
        taskStore.save(task);
        log.info("[WorkspaceTaskArrangementService] Submitted task {} to workspace {}", task.getId(), workspaceId);

        // 自动开始处理任务
        processTask(task, workspace, executionMode, specifiedCharacterIds);

        return task;
    }

    /**
     * 提交任务到工作空间（简化版本，使用默认 AUTO 模式）
     *
     * @param workspaceId 工作空间 ID
     * @param taskName 任务名称
     * @param taskDescription 任务描述
     * @param input 任务输入
     * @param creatorId 创建者 ID
     * @return 工作空间任务
     */
    public Task submitTask(String workspaceId, String taskName,
            String taskDescription, Object input, String creatorId) {
        return submitTask(workspaceId, taskName, taskDescription, input, creatorId,
                TaskExecutionMode.AUTO, null);
    }

    /**
     * 处理任务
     */
    private void processTask(Task task, Workspace workspace,
            TaskExecutionMode executionMode, List<String> specifiedCharacterIds) {
        // 获取工作空间成员
        List<WorkspaceMember> members = memberService.listMembers(task.getWorkspaceId());
        if (members.isEmpty()) {
            log.warn("[WorkspaceTaskArrangementService] No members available in workspace {}", task.getWorkspaceId());
            task.setStatus(TaskStatus.FAILED);
            taskStore.update(task);
            return;
        }

        List<Character> selectedCharacters;

        // 根据执行模式决定成员选择策略
        switch (executionMode) {
            case SPECIFIED:
                // 使用指定的 Character
                selectedCharacters = getSpecifiedCharacters(members, specifiedCharacterIds);
                break;
            case DEFAULT:
                // 使用默认 Character（取第一个）
                selectedCharacters = getDefaultCharacter(members);
                break;
            case AUTO:
            default:
                // 通过 MemberSelector Character 自动选择
                selectedCharacters = selectMembersWithCharacter(task, workspace, members);
                break;
        }

        if (selectedCharacters == null || selectedCharacters.isEmpty()) {
            log.warn("[WorkspaceTaskArrangementService] No characters selected for task {}", task.getId());
            task.setStatus(TaskStatus.FAILED);
            taskStore.update(task);
            return;
        }

        // 1. 任务分解 - 通过 ProjectManager Character
        List<Task> childTasks = decomposeTaskWithCharacter(task, workspace, members);

        if (childTasks == null || childTasks.isEmpty()) {
            log.warn("[WorkspaceTaskArrangementService] Task decomposition failed for task {}", task.getId());
            task.setStatus(TaskStatus.FAILED);
            taskStore.update(task);
            return;
        }

        // 保存子任务到 Store
        for (Task childTask : childTasks) {
            childTask.setParentTaskId(task.getId());
            taskStore.save(childTask);
        }
        task.setChildTaskIds(childTasks.stream().map(Task::getId).toList());
        task.setStatus(TaskStatus.RUNNING);
        taskStore.update(task);

        // 2. 创建协作会话
        List<String> participantIds = selectedCharacters.stream()
                .map(Character::getId)
                .toList();
        ChatSession session = chatRoom.createSession(
                task.getWorkspaceId(), participantIds, task.getId());
        task.setCollaborationSessionId(session.getId());
        taskStore.update(task);

        // 3. 分配任务
        assignChildTasks(childTasks, selectedCharacters, task);

        // 4. 执行任务
        executeChildTasks(childTasks, task);
    }

    /**
     * 通过 MemberSelector Character 选择成员
     */
    private List<Character> selectMembersWithCharacter(Task task, Workspace workspace,
            List<WorkspaceMember> members) {
        try {
            // 获取或创建 MemberSelector Character
            Character memberSelectorCharacter = memberSelectorCharacterFactory
                    .getOrCreateMemberSelectorCharacter(workspace.getId());

            // 构建 prompt
            String systemPrompt = promptManager.getGlobalPrompt(PromptKeys.MEMBER_SELECTOR_SELECT,
                    "请从以下候选成员中选择最合适的执行者来完成指定任务。");
            String prompt = buildMemberSelectionPrompt(task, members, systemPrompt);

            // 调用 Character 进行选择
            String result = characterCaller.call(memberSelectorCharacter, prompt);

            // 解析结果
            return parseSelectedCharacters(result, members);
        } catch (Exception e) {
            log.error("[WorkspaceTaskArrangementService] Member selection failed: {}", e.getMessage());
            // 降级：返回第一个成员
            return members.stream()
                    .map(m -> Character.builder().id(m.getCharacterId()).build())
                    .toList();
        }
    }

    /**
     * 通过 ProjectManager Character 分解任务
     */
    private List<Task> decomposeTaskWithCharacter(Task task, Workspace workspace,
            List<WorkspaceMember> members) {
        try {
            // 获取或创建 ProjectManager Character
            Character projectManagerCharacter = projectManagerCharacterFactory
                    .getOrCreateProjectManagerCharacter(workspace.getId());

            // 构建 prompt
            String systemPrompt = promptManager.getGlobalPrompt(PromptKeys.PROJECT_MANAGER_DECOMPOSE,
                    "请将以下任务拆解为可执行的子任务。");
            String prompt = buildTaskDecomposePrompt(task, members, systemPrompt);

            // 调用 Character 进行任务分解
            String result = characterCaller.call(projectManagerCharacter, prompt);

            // 解析结果
            return parseDecomposedTasks(result, task.getId());
        } catch (Exception e) {
            log.error("[WorkspaceTaskArrangementService] Task decomposition failed: {}", e.getMessage());
            // 降级：创建单个简单子任务
            return createFallbackChildTasks(task);
        }
    }

    /**
     * 获取指定的 Character
     */
    private List<Character> getSpecifiedCharacters(List<WorkspaceMember> members,
            List<String> specifiedCharacterIds) {
        if (specifiedCharacterIds == null || specifiedCharacterIds.isEmpty()) {
            return Collections.emptyList();
        }

        return members.stream()
                .filter(m -> specifiedCharacterIds.contains(m.getCharacterId()))
                .map(m -> Character.builder()
                        .id(m.getCharacterId())
                        .name(m.getRole())
                        .build())
                .toList();
    }

    /**
     * 获取默认 Character（取第一个）
     */
    private List<Character> getDefaultCharacter(List<WorkspaceMember> members) {
        if (members.isEmpty()) {
            return Collections.emptyList();
        }

        WorkspaceMember first = members.get(0);
        return List.of(Character.builder()
                .id(first.getCharacterId())
                .name(first.getRole())
                .build());
    }

    /**
     * 构建成员选择 prompt
     */
    private String buildMemberSelectionPrompt(Task task, List<WorkspaceMember> members,
            String systemPrompt) {
        StringBuilder sb = new StringBuilder();
        sb.append(systemPrompt).append("\n\n");
        sb.append("## 任务信息\n");
        sb.append("任务ID: ").append(task.getId()).append("\n");
        sb.append("任务名称: ").append(task.getName()).append("\n");
        sb.append("任务描述: ").append(task.getDescription()).append("\n");
        sb.append("任务输入: ").append(task.getInput()).append("\n\n");
        sb.append("## 候选成员列表\n");
        for (WorkspaceMember member : members) {
            sb.append("- ID: ").append(member.getCharacterId())
                    .append(", 角色: ").append(member.getRole())
                    .append(", 层级: ").append(member.getLayer()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 构建任务分解 prompt
     */
    private String buildTaskDecomposePrompt(Task task, List<WorkspaceMember> members,
            String systemPrompt) {
        StringBuilder sb = new StringBuilder();
        sb.append(systemPrompt).append("\n\n");
        sb.append("## 任务信息\n");
        sb.append("任务ID: ").append(task.getId()).append("\n");
        sb.append("任务名称: ").append(task.getName()).append("\n");
        sb.append("任务描述: ").append(task.getDescription()).append("\n");
        sb.append("任务输入: ").append(task.getInput()).append("\n\n");
        sb.append("## 可用成员列表\n");
        for (WorkspaceMember member : members) {
            sb.append("- ID: ").append(member.getCharacterId())
                    .append(", 角色: ").append(member.getRole())
                    .append(", 层级: ").append(member.getLayer()).append("\n");
        }
        sb.append("\n## 工作空间信息\n");
        sb.append("工作空间ID: ").append(task.getWorkspaceId()).append("\n");
        return sb.toString();
    }

    /**
     * 解析选中的 Character（简化版本，实际需要 JSON 解析）
     */
    private List<Character> parseSelectedCharacters(String result, List<WorkspaceMember> members) {
        // 简化实现：解析 JSON 格式结果
        // 实际实现需要更健壮的 JSON 解析
        try {
            // 尝试解析 JSON，提取 selectedMembers
            // 这里简化处理，如果无法解析则返回第一个成员
            if (result != null && result.contains("selectedMembers")) {
                // TODO: 实现完整的 JSON 解析
            }
        } catch (Exception e) {
            log.warn("[WorkspaceTaskArrangementService] Failed to parse selection result: {}", e.getMessage());
        }

        // 降级：返回第一个成员
        if (!members.isEmpty()) {
            WorkspaceMember first = members.get(0);
            return List.of(Character.builder()
                    .id(first.getCharacterId())
                    .name(first.getRole())
                    .build());
        }
        return Collections.emptyList();
    }

    /**
     * 解析分解后的子任务（简化版本，实际需要 JSON 解析）
     */
    private List<Task> parseDecomposedTasks(String result, String taskId) {
        // 简化实现：解析 JSON 格式结果
        // 实际实现需要更健壮的 JSON 解析
        try {
            // TODO: 实现完整的 JSON 解析
        } catch (Exception e) {
            log.warn("[WorkspaceTaskArrangementService] Failed to parse decomposition result: {}", e.getMessage());
        }

        // 降级：创建单个简单子任务
        return createFallbackChildTasks(Task.builder().id(taskId).build());
    }

    /**
     * 创建降级子任务列表
     */
    private List<Task> createFallbackChildTasks(Task task) {
        Task childTask = Task.builder()
                .id(UUID.randomUUID().toString())
                .parentTaskId(task.getId())
                .workspaceId(task.getWorkspaceId())
                .description(task.getDescription() != null ? task.getDescription() : task.getName())
                .status(TaskStatus.PENDING)
                .build();
        return List.of(childTask);
    }

    /**
     * 分配子任务
     */
    private void assignChildTasks(List<Task> childTasks, List<Character> selectedCharacters,
            Task parentTask) {
        parentTask.setAssignedMemberIds(selectedCharacters.stream()
                .map(Character::getId)
                .toList());
        parentTask.setStatus(TaskStatus.RUNNING);
        taskStore.update(parentTask);

        for (int i = 0; i < childTasks.size(); i++) {
            Task childTask = childTasks.get(i);
            if (i < selectedCharacters.size()) {
                Character character = selectedCharacters.get(i);
                childTask.setCharacterId(character.getId());
            }
            childTask.setStatus(TaskStatus.RUNNING);
            taskStore.update(childTask);
        }
    }

    /**
     * 执行子任务
     */
    private void executeChildTasks(List<Task> childTasks, Task parentTask) {
        for (Task childTask : childTasks) {
            try {
                childTask.setStatus(TaskStatus.RUNNING);
                childTask.setStartedAt(LocalDateTime.now());
                taskStore.update(childTask);

                // TODO: 通过 TaskBridge 提交给 Character 执行
                // 这里模拟执行
                log.info("[WorkspaceTaskArrangementService] Executing childTask {} on character {}",
                        childTask.getId(), childTask.getCharacterId());

                // 模拟执行完成
                childTask.setStatus(TaskStatus.COMPLETED);
                childTask.setCompletedAt(LocalDateTime.now());
                childTask.setResult("Task completed");
                taskStore.update(childTask);

            } catch (Exception e) {
                childTask.setStatus(TaskStatus.FAILED);
                childTask.setErrorMessage(e.getMessage());
                taskStore.update(childTask);

                log.error("[WorkspaceTaskArrangementService] ChildTask {} failed: {}", childTask.getId(), e.getMessage());
            }
        }

        // 检查所有子任务是否完成
        checkAndCompleteTask(parentTask);
    }

    /**
     * 检查并完成任务
     */
    private void checkAndCompleteTask(Task parentTask) {
        List<Task> childTasks = parentTask.getChildTaskIds().stream()
                .map(taskStore::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        boolean allCompleted = childTasks.stream()
                .allMatch(st -> st.getStatus() == TaskStatus.COMPLETED);
        boolean anyFailed = childTasks.stream()
                .anyMatch(st -> st.getStatus() == TaskStatus.FAILED);

        if (allCompleted) {
            parentTask.setStatus(TaskStatus.COMPLETED);
            parentTask.setOutput("All child tasks completed successfully");
            taskStore.update(parentTask);
            log.info("[WorkspaceTaskArrangementService] Task {} completed", parentTask.getId());
        } else if (anyFailed) {
            parentTask.setStatus(TaskStatus.FAILED);
            taskStore.update(parentTask);
            log.warn("[WorkspaceTaskArrangementService] Task {} failed", parentTask.getId());
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
        log.info("[WorkspaceTaskArrangementService] Rebalancing task {} with feedback: {}", taskId, feedback);
    }

    /**
     * ExecutionFeedback 执行反馈
     */
    public static class ExecutionFeedback {
        private String childTaskId;
        private boolean success;
        private String errorMessage;
        private long durationMs;

        public ExecutionFeedback(String childTaskId, boolean success, String errorMessage, long durationMs) {
            this.childTaskId = childTaskId;
            this.success = success;
            this.errorMessage = errorMessage;
            this.durationMs = durationMs;
        }

        public String getChildTaskId() { return childTaskId; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public long getDurationMs() { return durationMs; }
    }
}
