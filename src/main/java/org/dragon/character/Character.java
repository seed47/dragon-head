package org.dragon.character;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.dragon.agent.orchestration.OrchestrationService;
import org.dragon.agent.orchestration.OrchestrationService.OrchestrationResult;
import org.dragon.agent.orchestration.OrchestrationService.ReActRequest;
import org.dragon.agent.react.ReActResult;
import org.dragon.agent.workflow.WorkflowResult;
import org.dragon.character.task.DefaultTaskManager;
import org.dragon.character.task.Task;
import org.dragon.character.task.TaskManager;
import org.dragon.character.task.TaskOperation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Character 实体
 * AI 数字员工实体，由 Mind 和 Agent Engine 两部分组成
 * 作为主流程入口类，负责执行任务和管理任务队列
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Character {

    /**
     * 任务管理器
     */
    @Builder.Default
    private TaskManager taskManager = new DefaultTaskManager();

    /**
     * 编排服务（内部流程组件）
     * 由外部注入
     */
    private OrchestrationService orchestrationService;

    /**
     * Character 全局唯一标识
     */
    private String id;

    /**
     * Character 名称
     */
    private String name;

    /**
     * 版本号，用于版本管理
     */
    private int version;

    /**
     * 描述
     */
    private String description;

    /**
     * 心智模块配置
     */
    private MindConfig mindConfig;

    /**
     * 执行引擎配置
     */
    private AgentEngineConfig agentEngineConfig;

    /**
     * 扩展属性
     */
    private Map<String, Object> extensions;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 状态
     */
    private Status status;

    /**
     * Character 状态枚举
     */
    public enum Status {
        /** 未加载 */
        UNLOADED,
        /** 已加载 */
        LOADED,
        /** 运行中 */
        RUNNING,
        /** 暂停 */
        PAUSED,
        /** 已销毁 */
        DESTROYED
    }

    /**
     * Mind 配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MindConfig {
        /** 性格描述文件路径 */
        private String personalityDescriptorPath;
        /** 标签存储类型 */
        private String tagRepositoryType;
        /** 记忆存储类型 */
        private String memoryAccessType;
        /** 技能存储类型 */
        private String skillAccessType;
    }

    /**
     * Agent Engine 配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentEngineConfig {
        /** 默认模型 ID */
        private String defaultModelId;
        /** 工作流配置 */
        private WorkflowConfig workflowConfig;
        /** ReAct 配置 */
        private ReActConfig reActConfig;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowConfig {
        private String defaultWorkflowId;
        private int maxSteps;
        private String timeout;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReActConfig {
        private int maxIterations;
        private boolean enableMemorySearch;
        private boolean enableToolUse;
    }

    // ==================== 执行接口 ====================

    /**
     * 执行主入口
     * 根据配置选择执行模式（REACT 或 WORKFLOW）
     *
     * @param userInput 用户输入
     * @return 执行结果
     */
    public String run(String userInput) {
        if (orchestrationService == null) {
            throw new IllegalStateException("OrchestrationService not initialized");
        }

        // 获取执行模式配置
        String executionMode = getAgentEngineConfig() != null &&
                getAgentEngineConfig().getReActConfig() != null ? "REACT" : "REACT";

        // 构建编排请求
        OrchestrationService.OrchestrationRequest request = OrchestrationService.OrchestrationRequest.builder()
                .characterId(this.id)
                .message(userInput)
                .mode(OrchestrationService.Mode.valueOf(executionMode))
                .build();

        OrchestrationResult result = orchestrationService.orchestrate(request);
        return result.getResponse();
    }

    /**
     * 使用 ReAct 模式执行
     *
     * @param userInput 用户输入
     * @return ReAct 执行结果
     */
    public ReActResult runReAct(String userInput) {
        if (orchestrationService == null) {
            throw new IllegalStateException("OrchestrationService not initialized");
        }

        ReActConfig config = getAgentEngineConfig() != null ?
                getAgentEngineConfig().getReActConfig() : null;

        ReActRequest request = ReActRequest.builder()
                .characterId(this.id)
                .userInput(userInput)
                .defaultModelId(getAgentEngineConfig() != null ?
                        getAgentEngineConfig().getDefaultModelId() : null)
                .maxIterations(config != null ? config.getMaxIterations() : 10)
                .build();

        return orchestrationService.runReAct(request);
    }

    /**
     * 使用 Workflow 模式执行
     *
     * @param workflowId 工作流 ID
     * @return Workflow 执行结果
     */
    public WorkflowResult runWorkflow(String workflowId) {
        if (orchestrationService == null) {
            throw new IllegalStateException("OrchestrationService not initialized");
        }

        OrchestrationService.OrchestrationRequest request = OrchestrationService.OrchestrationRequest.builder()
                .characterId(this.id)
                .message("")
                .mode(OrchestrationService.Mode.WORKFLOW)
                .workflowId(workflowId)
                .build();

        OrchestrationResult result = orchestrationService.orchestrate(request);
        // 将 OrchestrationResult 转换为 WorkflowResult
        return WorkflowResult.builder()
                .status(result.isSuccess() ? org.dragon.agent.workflow.WorkflowState.State.COMPLETED :
                        org.dragon.agent.workflow.WorkflowState.State.FAILED)
                .output(Collections.singletonMap("response", result.getResponse()))
                .executionId(result.getExecutionId())
                .durationMs(result.getDurationMs())
                .build();
    }

    // ==================== 任务管理接口 ====================

    /**
     * 添加任务
     *
     * @param task 任务
     * @return 添加后的任务
     */
    public Task addTask(Task task) {
        task.setCharacterId(this.id);
        if (task.getType() == null) {
            task.setType(Task.TaskType.USER_REQUEST);
        }
        return taskManager.addTask(task);
    }

    /**
     * 创建并添加任务，然后执行
     *
     * @param userInput 用户输入
     * @return 执行结果
     */
    public Task addTaskAndRun(String userInput) {
        Task task = Task.builder()
                .name("Task-" + System.currentTimeMillis())
                .type(Task.TaskType.USER_REQUEST)
                .input(userInput)
                .characterId(this.id)
                .executionMode("REACT")
                .build();

        task = addTask(task);

        // 执行任务
        try {
            String result = run(userInput);
            task.setResult(result);
            task.setStatus(org.dragon.character.task.TaskStatus.COMPLETED);
        } catch (Exception e) {
            task.setErrorMessage(e.getMessage());
            task.setStatus(org.dragon.character.task.TaskStatus.FAILED);
        }

        return task;
    }

    /**
     * 操作任务
     *
     * @param taskId    任务ID
     * @param operation 操作
     * @return 操作后的任务，如果不存在则返回 null
     */
    public Task operateTask(String taskId, TaskOperation operation) {
        return taskManager.operateTask(taskId, operation);
    }

    /**
     * 暂停任务
     *
     * @param taskId 任务ID
     * @return 暂停后的任务，如果不存在则返回 null
     */
    public Task pauseTask(String taskId) {
        return operateTask(taskId, TaskOperation.PAUSE);
    }

    /**
     * 恢复任务
     *
     * @param taskId 任务ID
     * @return 恢复后的任务，如果不存在则返回 null
     */
    public Task resumeTask(String taskId) {
        return operateTask(taskId, TaskOperation.RESUME);
    }

    /**
     * 取消任务
     *
     * @param taskId 任务ID
     * @return 取消后的任务，如果不存在则返回 null
     */
    public Task cancelTask(String taskId) {
        return operateTask(taskId, TaskOperation.CANCEL);
    }

    /**
     * 重试任务
     *
     * @param taskId 任务ID
     * @return 任务，如果不存在则返回 null
     */
    public Task retryTask(String taskId) {
        return operateTask(taskId, TaskOperation.RETRY);
    }

    /**
     * 获取任务
     *
     * @param taskId 任务ID
     * @return 任务，如果不存在则返回 null
     */
    public Task getTask(String taskId) {
        return taskManager.getTask(taskId);
    }

    /**
     * 列出所有任务
     *
     * @return 任务列表
     */
    public List<Task> listTasks() {
        return taskManager.listTasks();
    }

    /**
     * 根据状态获取任务
     *
     * @param status 任务状态
     * @return 任务列表
     */
    public List<Task> getTasksByStatus(org.dragon.character.task.TaskStatus status) {
        return taskManager.getTasksByStatus(status);
    }

    /**
     * 删除任务
     *
     * @param taskId 任务ID
     * @return 是否删除成功
     */
    public boolean deleteTask(String taskId) {
        return taskManager.deleteTask(taskId);
    }

    /**
     * 清空所有任务
     */
    public void clearTasks() {
        taskManager.clear();
    }

    /**
     * 获取任务数量
     *
     * @return 任务数量
     */
    public int getTaskCount() {
        return taskManager.size();
    }
}
