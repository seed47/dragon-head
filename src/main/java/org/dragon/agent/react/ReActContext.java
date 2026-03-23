package org.dragon.agent.react;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.task.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ReAct 执行上下文
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReActContext {

    /**
     * 执行 ID
     */
    private String executionId;

    /**
     * Character ID
     */
    private String characterId;

    /**
     * 默认模型 ID
     */
    private String defaultModelId;

    /**
     * 当前模型 ID
     */
    private String currentModelId;

    /**
     * 是否需要切换模型
     */
    private boolean modelSwitchRequested;

    /**
     * 下一个模型 ID
     */
    private String nextModelId;

    /**
     * 用户输入
     */
    private String userInput;

    /**
     * 系统提示
     */
    private String systemPrompt;

    /**
     * 思考历史
     */
    @Builder.Default
    private List<String> thoughts = new ArrayList<>();

    /**
     * 动作历史
     */
    @Builder.Default
    private List<Action> actions = new ArrayList<>();

    /**
     * 观察结果历史
     */
    @Builder.Default
    private List<String> observations = new ArrayList<>();

    /**
     * 上下文变量
     */
    @Builder.Default
    private Map<String, Object> context = new HashMap<>();

    /**
     * 最大迭代次数
     */
    private int maxIterations;

    /**
     * 当前迭代次数
     */
    private int currentIteration;

    /**
     * 是否启用流式调用
     */
    @Builder.Default
    private boolean streamingEnabled = false;

    /**
     * 关联的 Task（用于流式输出）
     */
    private Task task;

    /**
     * 是否完成
     */
    private boolean complete;

    /**
     * 最终响应
     */
    private String finalResponse;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 允许使用的工具名称集合
     */
    @Builder.Default
    private Set<String> allowedTools = new HashSet<>();

    /**
     * 获取当前模型 ID
     *
     * @return 模型 ID
     */
    public String getCurrentModelId() {
        return currentModelId != null ? currentModelId : defaultModelId;
    }

    /**
     * 添加思考
     *
     * @param thought 思考内容
     */
    public void addThought(String thought) {
        this.thoughts.add(thought);
    }

    /**
     * 添加动作
     *
     * @param action 动作
     */
    public void addAction(Action action) {
        this.actions.add(action);
    }

    /**
     * 添加观察结果
     *
     * @param observation 观察结果
     */
    public void addObservation(String observation) {
        this.observations.add(observation);
    }

    /**
     * 检查是否需要切换模型
     *
     * @return 是否需要切换
     */
    public boolean hasModelSwitch() {
        return modelSwitchRequested;
    }

    /**
     * 标记为完成
     *
     * @param response 最终响应
     */
    public void complete(String response) {
        this.complete = true;
        this.finalResponse = response;
    }

    /**
     * 递增迭代次数
     *
     * @return 当前迭代次数
     */
    public int incrementIteration() {
        return ++currentIteration;
    }

    /**
     * 检查工具是否允许使用
     *
     * @param toolName 工具名称
     * @return 是否允许
     */
    public boolean isToolAllowed(String toolName) {
        return allowedTools == null || allowedTools.isEmpty() || allowedTools.contains(toolName);
    }
}
