package org.dragon.workspace.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dragon.agent.llm.util.CharacterCaller;
import org.dragon.character.Character;
import org.dragon.config.PromptKeys;
import org.dragon.config.PromptManager;
import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.task.TaskStore;
import org.dragon.workspace.Workspace;
import org.dragon.workspace.WorkspaceRegistry;
import org.dragon.workspace.built_ins.character.project_manager.ProjectManagerCharacterFactory;
import org.dragon.workspace.built_ins.character.prompt_writer.PromptWriterCharacterFactory;
import org.dragon.workspace.chat.ChatRoom;
import org.dragon.workspace.chat.ChatSession;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.workspace.built_ins.character.prompt_writer.dto.PromptWriterInput;
import org.dragon.workspace.service.dto.ChildTaskPlan;
import org.dragon.workspace.service.dto.TaskDecompositionResult;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WorkspaceTaskArrangementService 工作空间任务编排服务
 * 核心编排逻辑：任务分解、成员选择、任务分配
 * 通过 ProjectManager Character 实现智能编排
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
    private final ProjectManagerCharacterFactory projectManagerCharacterFactory;
    private final PromptWriterCharacterFactory promptWriterCharacterFactory;
    private final TaskStore taskStore;
    private final WorkspaceTaskExecutionService taskExecutionService;
    private final Gson gson = new Gson();

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
     * 主流程：获取成员 -> 任务分解 -> 创建协作会话 -> 保存子任务 -> 执行
     */
    private void processTask(Task task, Workspace workspace,
            TaskExecutionMode executionMode, List<String> specifiedCharacterIds) {
        // 获取工作空间成员
        List<WorkspaceMember> members = memberService.listMembers(task.getWorkspaceId());
        if (members.isEmpty()) {
            log.warn("[WorkspaceTaskArrangementService] No members available in workspace {}", task.getWorkspaceId());
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("No members available");
            taskStore.update(task);
            return;
        }

        // 处理指定模式（AUTO 模式下由 ProjectManager 决定角色）
        if (executionMode == TaskExecutionMode.SPECIFIED) {
            // 使用指定的 Character
            handleSpecifiedMode(task, members, specifiedCharacterIds);
        } else if (executionMode == TaskExecutionMode.DEFAULT) {
            // 使用默认 Character
            handleDefaultMode(task, members);
        } else {
            // AUTO 模式：通过 ProjectManager 分解任务，characterId 直接从分解结果获取
            handleAutoMode(task, workspace, members);
        }
    }

    /**
     * AUTO 模式：任务分解后直接获取 characterId
     */
    private void handleAutoMode(Task task, Workspace workspace, List<WorkspaceMember> members) {
        // 1. 任务分解 - 通过 ProjectManager Character
        TaskDecompositionResult decompositionResult = decomposeTaskWithCharacter(task, workspace, members);

        if (decompositionResult == null || decompositionResult.getChildTasks() == null || decompositionResult.getChildTasks().isEmpty()) {
            log.warn("[WorkspaceTaskArrangementService] Task decomposition failed for task {}", task.getId());
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("Task decomposition returned no child tasks");
            taskStore.update(task);
            return;
        }

        // 2. 解析分解结果为子任务列表
        List<Task> childTasks = parseDecomposedTasks(decompositionResult, task);

        if (childTasks.isEmpty()) {
            log.warn("[WorkspaceTaskArrangementService] Failed to parse child tasks for task {}", task.getId());
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("Failed to parse child tasks");
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

        // 3. 创建协作会话（参与方为所有子任务的执行者）
        List<String> participantIds = childTasks.stream()
                .map(Task::getCharacterId)
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        if (!participantIds.isEmpty()) {
            ChatSession session = chatRoom.createSession(
                    task.getWorkspaceId(), participantIds, task.getId());
            task.setCollaborationSessionId(session.getId());
            taskStore.update(task);
        }

        // 4. 执行子任务（委托给 WorkspaceTaskExecutionService）
        taskExecutionService.executeChildTasks(childTasks, task);
    }

    /**
     * SPECIFIED 模式：使用指定的 Character
     */
    private void handleSpecifiedMode(Task task, List<WorkspaceMember> members, List<String> specifiedCharacterIds) {
        List<String> characterIds = getSpecifiedCharacterIds(members, specifiedCharacterIds);
        if (characterIds.isEmpty()) {
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("No valid specified characters");
            taskStore.update(task);
            return;
        }

        // 创建单一子任务，分配给指定的第一个 Character
        Task childTask = Task.builder()
                .id(UUID.randomUUID().toString())
                .parentTaskId(task.getId())
                .workspaceId(task.getWorkspaceId())
                .characterId(characterIds.get(0))
                .name(task.getName())
                .description(task.getDescription())
                .input(task.getInput())
                .status(TaskStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        taskStore.save(childTask);
        task.setChildTaskIds(List.of(childTask.getId()));
        task.setAssignedMemberIds(characterIds);
        task.setStatus(TaskStatus.RUNNING);
        taskStore.update(task);

        // 执行
        taskExecutionService.executeChildTask(childTask, task);
    }

    /**
     * DEFAULT 模式：使用默认 Character
     */
    private void handleDefaultMode(Task task, List<WorkspaceMember> members) {
        if (members.isEmpty()) {
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage("No members available");
            taskStore.update(task);
            return;
        }

        // 使用第一个成员
        WorkspaceMember defaultMember = members.get(0);
        Task childTask = Task.builder()
                .id(UUID.randomUUID().toString())
                .parentTaskId(task.getId())
                .workspaceId(task.getWorkspaceId())
                .characterId(defaultMember.getCharacterId())
                .name(task.getName())
                .description(task.getDescription())
                .input(task.getInput())
                .status(TaskStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        taskStore.save(childTask);
        task.setChildTaskIds(List.of(childTask.getId()));
        task.setAssignedMemberIds(List.of(defaultMember.getCharacterId()));
        task.setStatus(TaskStatus.RUNNING);
        taskStore.update(task);

        // 执行
        taskExecutionService.executeChildTask(childTask, task);
    }

    /**
     * 通过 ProjectManager Character 分解任务
     * 返回结构化的 TaskDecompositionResult
     */
    private TaskDecompositionResult decomposeTaskWithCharacter(Task task, Workspace workspace,
            List<WorkspaceMember> members) {
        try {
            // 1. 获取或创建 PromptWriter Character 用于组装 prompt
            Character promptWriterCharacter = promptWriterCharacterFactory
                    .getOrCreatePromptWriterCharacter(workspace.getId());

            // 2. 获取 prompt 模板
            String promptTemplate = promptManager.getGlobalPrompt(PromptKeys.PROJECT_MANAGER_DECOMPOSE,
                    "请将以下任务拆解为可执行的子任务。");

            // 3. 构建给 PromptWriter 的输入（包含模板和动态数据）
            String promptWriterInput = buildPromptWriterInput("task_decompose", promptTemplate, task, members);

            // 4. 调用 PromptWriter Character 获取完整的 prompt
            String fullPrompt = characterCaller.call(promptWriterCharacter, promptWriterInput);

            // 5. 获取或创建 ProjectManager Character
            Character projectManagerCharacter = projectManagerCharacterFactory
                    .getOrCreateProjectManagerCharacter(workspace.getId());

            // 6. 调用 ProjectManager Character 进行任务分解
            String result = characterCaller.call(projectManagerCharacter, fullPrompt);

            // 7. 解析结果为 TaskDecompositionResult
            return parseDecompositionResult(result);

        } catch (Exception e) {
            log.error("[WorkspaceTaskArrangementService] Task decomposition failed: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 构建给 PromptWriter Character 的输入
     * 包含更丰富的上下文：成员能力、协作约束等
     */
    private String buildPromptWriterInput(String promptType, String promptTemplate, Task task,
            List<WorkspaceMember> members) {
        List<PromptWriterInput.MemberInfo> memberInfos = members.stream()
                .map(m -> PromptWriterInput.MemberInfo.builder()
                        .characterId(m.getCharacterId())
                        .role(m.getRole())
                        .layer(m.getLayer() != null ? m.getLayer().toString() : null)
                        .build())
                .toList();

        // 构建更丰富的上下文提示
        Map<String, Object> contextHints = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "collaborationMode", "AUTO",
                "allowFollowUp", true,
                "maxChildTasks", 10
        );

        PromptWriterInput input = PromptWriterInput.builder()
                .workspaceId(task.getWorkspaceId())
                .promptType(promptType)
                .promptTemplate(promptTemplate)
                .task(PromptWriterInput.TaskInfo.builder()
                        .id(task.getId())
                        .name(task.getName())
                        .description(task.getDescription())
                        .input(task.getInput())
                        .parentTaskId(task.getParentTaskId())
                        .build())
                .members(memberInfos)
                .contextHints(contextHints)
                .build();

        return gson.toJson(input);
    }

    /**
     * 解析 ProjectManager 返回的分解结果
     * 期望返回 JSON 格式的 TaskDecompositionResult
     */
    private TaskDecompositionResult parseDecompositionResult(String result) {
        if (result == null || result.isEmpty()) {
            log.warn("[WorkspaceTaskArrangementService] Empty decomposition result");
            return null;
        }

        try {
            // 尝试解析为 TaskDecompositionResult
            return gson.fromJson(result, TaskDecompositionResult.class);
        } catch (JsonSyntaxException e) {
            log.warn("[WorkspaceTaskArrangementService] Failed to parse decomposition result as TaskDecompositionResult: {}", e.getMessage());
            // 尝试从更宽泛的 JSON 结构中提取
            return tryExtractFromAlternativeFormat(result);
        }
    }

    /**
     * 尝试从替代格式提取分解结果
     */
    private TaskDecompositionResult tryExtractFromAlternativeFormat(String result) {
        try {
            JsonObject json = gson.fromJson(result, JsonObject.class);

            TaskDecompositionResult.TaskDecompositionResultBuilder builder = TaskDecompositionResult.builder();

            if (json.has("summary")) {
                builder.summary(json.get("summary").getAsString());
            }
            if (json.has("collaborationMode")) {
                builder.collaborationMode(json.get("collaborationMode").getAsString());
            }

            if (json.has("childTasks")) {
                JsonArray childTasksArray = json.getAsJsonArray("childTasks");
                List<ChildTaskPlan> childTasks = new ArrayList<>();

                for (JsonElement element : childTasksArray) {
                    JsonObject childObj = element.getAsJsonObject();
                    ChildTaskPlan.ChildTaskPlanBuilder childBuilder = ChildTaskPlan.builder();

                    if (childObj.has("name")) childBuilder.name(childObj.get("name").getAsString());
                    if (childObj.has("description")) childBuilder.description(childObj.get("description").getAsString());
                    if (childObj.has("characterId")) childBuilder.characterId(childObj.get("characterId").getAsString());
                    if (childObj.has("characterRole")) childBuilder.characterRole(childObj.get("characterRole").getAsString());
                    if (childObj.has("needsUserInput")) childBuilder.needsUserInput(childObj.get("needsUserInput").getAsBoolean());
                    if (childObj.has("expectedOutput")) childBuilder.expectedOutput(childObj.get("expectedOutput").getAsString());

                    if (childObj.has("dependencyTaskIds")) {
                        JsonArray deps = childObj.getAsJsonArray("dependencyTaskIds");
                        List<String> depList = new ArrayList<>();
                        for (JsonElement dep : deps) {
                            depList.add(dep.getAsString());
                        }
                        childBuilder.dependencyTaskIds(depList);
                    }

                    childTasks.add(childBuilder.build());
                }

                builder.childTasks(childTasks);
            }

            return builder.build();
        } catch (Exception e) {
            log.error("[WorkspaceTaskArrangementService] Failed to extract decomposition result: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 将 TaskDecompositionResult 转换为 List<Task>
     */
    private List<Task> parseDecomposedTasks(TaskDecompositionResult result, Task parentTask) {
        if (result == null || result.getChildTasks() == null || result.getChildTasks().isEmpty()) {
            log.warn("[WorkspaceTaskArrangementService] No child tasks in decomposition result");
            return Collections.emptyList();
        }

        List<Task> childTasks = new ArrayList<>();

        for (ChildTaskPlan plan : result.getChildTasks()) {
            Task childTask = Task.builder()
                    .id(UUID.randomUUID().toString())
                    .parentTaskId(parentTask.getId())
                    .workspaceId(parentTask.getWorkspaceId())
                    .name(plan.getName())
                    .description(plan.getDescription())
                    .characterId(plan.getCharacterId())
                    .input(parentTask.getInput())
                    .status(TaskStatus.PENDING)
                    .dependencyTaskIds(plan.getDependencyTaskIds() != null ? plan.getDependencyTaskIds() : new ArrayList<>())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            childTasks.add(childTask);
        }

        return childTasks;
    }

    /**
     * 获取指定的 Character ID 列表
     */
    private List<String> getSpecifiedCharacterIds(List<WorkspaceMember> members,
            List<String> specifiedCharacterIds) {
        if (specifiedCharacterIds == null || specifiedCharacterIds.isEmpty()) {
            return Collections.emptyList();
        }

        return members.stream()
                .filter(m -> specifiedCharacterIds.contains(m.getCharacterId()))
                .map(WorkspaceMember::getCharacterId)
                .collect(Collectors.toList());
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
